package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.local.dao.CategoryDao
import com.ghostteam.findetect.data.local.dao.TransactionDao
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.data.mapper.toUpsertRequest
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.model.toMoneyAmount
import com.ghostteam.findetect.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalTransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) : TransactionRepository {
    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> = transactionDao.observeFiltered(
        currency = filter.currency,
        accountId = filter.accountId,
        categoryId = filter.categoryId,
        isIncome = filter.isIncome,
        fromInclusive = filter.fromInclusive,
        toExclusive = filter.toExclusive,
    ).map { rows -> rows.map { it.toDomain() } }

    override fun observeTotalAmount(filter: TransactionFilter): Flow<BigDecimal> {
        require(!filter.currency.isNullOrBlank() && filter.isIncome != null) {
            "A total requires a currency and an income/expense type"
        }
        return transactionDao.observeTotalMinor(
            currency = filter.currency,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            isIncome = filter.isIncome,
            fromInclusive = filter.fromInclusive,
            toExclusive = filter.toExclusive,
        ).map { it.toMoneyAmount() }
    }

    override suspend fun getById(id: String): Transaction? = transactionDao.getById(id)?.toDomain()

    override suspend fun create(transaction: Transaction): Transaction = database.withTransaction {
        require(transaction.id.isNotBlank()) { "A transaction ID is required" }
        require(!transaction.isDeleted) { "A new transaction cannot be deleted" }
        check(transactionDao.getById(transaction.id) == null) { "Transaction already exists" }
        val account = requireNotNull(accountDao.getById(transaction.accountId)) { "Account does not exist" }
        require(account.deletedAt == null) { "A new transaction requires an active account" }
        val category = requireNotNull(categoryDao.getById(transaction.categoryId)) { "Category does not exist" }
        require(!category.isArchived) { "A new transaction requires an available category" }
        val local = transaction.copy(
            modifiedAt = nextModifiedAt(),
            createdAt = null,
            updatedAt = null,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        local.toUpsertRequest()
        val entity = local.toEntity()
        transactionDao.insert(entity)
        entity.toDomain()
    }

    override suspend fun update(transaction: Transaction): Transaction = database.withTransaction {
        val existing = requireNotNull(transactionDao.getById(transaction.id)) { "Transaction does not exist" }
        require(existing.deletedAt == null && !transaction.isDeleted) { "A deleted transaction cannot be edited" }
        check(transaction.modifiedAt == existing.modifiedAt) { "Transaction changed; reload before editing" }
        val account = requireNotNull(accountDao.getById(transaction.accountId)) { "Account does not exist" }
        require(account.deletedAt == null || transaction.accountId == existing.accountId) {
            "Cannot move a transaction to a deleted account"
        }
        val category = requireNotNull(categoryDao.getById(transaction.categoryId)) { "Category does not exist" }
        val originalCategory = requireNotNull(categoryDao.getById(existing.categoryId)) { "Original category is missing" }
        require(category.isIncome == originalCategory.isIncome) { "Transaction type cannot change" }
        require(!category.isArchived || transaction.categoryId == existing.categoryId) {
            "Cannot change to an archived category"
        }
        val local = existing.toDomain().copy(
            accountId = transaction.accountId,
            categoryId = transaction.categoryId,
            amount = transaction.amount,
            transactionDate = transaction.transactionDate,
            comment = transaction.comment,
            modifiedAt = nextModifiedAt(existing.modifiedAt),
            syncStatus = existing.syncStatus.afterLocalEdit(),
        )
        local.toUpsertRequest()
        val entity = local.toEntity()
        check(transactionDao.update(entity) == 1) { "Transaction update failed" }
        entity.toDomain()
    }

    override suspend fun softDelete(id: String): Boolean = database.withTransaction {
        val existing = transactionDao.getById(id) ?: return@withTransaction false
        if (existing.deletedAt != null) return@withTransaction false
        val now = nextModifiedAt(existing.modifiedAt)
        check(transactionDao.update(existing.copy(
            deletedAt = now,
            modifiedAt = now,
            syncStatus = SyncStatus.PENDING_DELETE,
        )) == 1) { "Transaction deletion failed" }
        true
    }

    override suspend fun getPendingChanges(): List<Transaction> =
        transactionDao.getPendingChanges().map { it.toDomain() }

    override suspend fun acknowledgeUpsert(sent: Transaction, confirmed: Transaction): Boolean = database.withTransaction {
        require(sent.syncStatus.isPendingUpsert() && !sent.isDeleted) { "Expected a pending transaction upsert" }
        require(sent.id == confirmed.id) { "Server response belongs to another transaction" }
        require(confirmed.createdAt != null && confirmed.updatedAt != null) { "Server timestamps are required" }
        val existing = transactionDao.getById(sent.id) ?: return@withTransaction false
        if (existing.toDomain() != sent) return@withTransaction false
        check(transactionDao.update(confirmed.copy(syncStatus = SyncStatus.SYNCED).toEntity()) == 1) {
            "Transaction acknowledgement failed"
        }
        true
    }

    override suspend fun acknowledgeDeletion(sent: Transaction): Boolean = database.withTransaction {
        require(sent.syncStatus == SyncStatus.PENDING_DELETE && sent.isDeleted) { "Expected a pending deletion" }
        val existing = transactionDao.getById(sent.id) ?: return@withTransaction false
        if (existing.toDomain() != sent) return@withTransaction false
        check(transactionDao.update(existing.copy(syncStatus = SyncStatus.SYNCED)) == 1) {
            "Transaction deletion acknowledgement failed"
        }
        true
    }
}
