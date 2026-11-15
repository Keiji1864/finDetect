package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.local.dao.CategoryDao
import com.ghostteam.findetect.data.local.dao.TransactionDao
import com.ghostteam.findetect.data.local.dao.TransactionDeletionDao
import com.ghostteam.findetect.data.local.entity.TransactionDeletionEntity
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionCreationError
import com.ghostteam.findetect.domain.model.TransactionCreationException
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.model.TransactionDetails
import com.ghostteam.findetect.domain.model.PendingTransactionDeletion
import com.ghostteam.findetect.domain.model.TransactionManagementError
import com.ghostteam.findetect.domain.model.TransactionManagementException
import com.ghostteam.findetect.domain.model.toMoneyAmount
import com.ghostteam.findetect.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LocalTransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val deletionDao: TransactionDeletionDao,
    private val clock: Clock,
) : TransactionRepository {
    override fun observeDetailsById(id: String): Flow<TransactionDetails?> =
        transactionDao.observeDetailsById(id).map { it?.toDomain() }

    override fun observeDetails(filter: TransactionFilter, limit: Int?): Flow<List<TransactionDetails>> {
        require(limit == null || limit > 0) { "Transaction limit must be positive" }
        return transactionDao.observeDetails(
            currency = filter.currency,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            isIncome = filter.isIncome,
            fromInclusive = filter.fromInclusive,
            toExclusive = filter.toExclusive,
            limit = limit ?: -1,
        ).map { rows -> rows.map { it.toDomain() } }
    }

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

    override suspend fun create(transaction: Transaction, expectedIsIncome: Boolean): Transaction = database.withTransaction {
        if (transaction.id.isBlank()) throw TransactionCreationException(TransactionCreationError.INVALID_TRANSACTION_ID)
        if (transaction.isDeleted) throw TransactionCreationException(TransactionCreationError.TRANSACTION_ALREADY_DELETED)
        if (transaction.accountId.isBlank()) throw TransactionCreationException(TransactionCreationError.ACCOUNT_REQUIRED)
        if (transaction.categoryId.isBlank()) throw TransactionCreationException(TransactionCreationError.CATEGORY_REQUIRED)
        if (transaction.comment != null && transaction.comment.length > 200) {
            throw TransactionCreationException(TransactionCreationError.COMMENT_TOO_LONG)
        }
        if (transactionDao.getById(transaction.id) != null) {
            throw TransactionCreationException(TransactionCreationError.DUPLICATE_TRANSACTION_ID)
        }
        val account = accountDao.getById(transaction.accountId)
            ?: throw TransactionCreationException(TransactionCreationError.ACCOUNT_NOT_FOUND)
        if (account.deletedAt != null) throw TransactionCreationException(TransactionCreationError.ACCOUNT_DELETED)
        val category = categoryDao.getById(transaction.categoryId)
            ?: throw TransactionCreationException(TransactionCreationError.CATEGORY_NOT_FOUND)
        if (category.isArchived) throw TransactionCreationException(TransactionCreationError.CATEGORY_ARCHIVED)
        if (category.isIncome != expectedIsIncome) {
            throw TransactionCreationException(TransactionCreationError.CATEGORY_TYPE_MISMATCH)
        }
        val local = transaction.copy(
            modifiedAt = nextModifiedAt(),
            createdAt = null,
            updatedAt = null,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        val entity = try {
            local.toEntity()
        } catch (error: ArithmeticException) {
            throw TransactionCreationException(TransactionCreationError.INVALID_AMOUNT, error)
        } catch (error: IllegalArgumentException) {
            throw TransactionCreationException(TransactionCreationError.INVALID_AMOUNT, error)
        }
        transactionDao.insert(entity)
        database.activityDayDao().recordTransaction(
            entity.enteredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
            entity.enteredAt,
        )
        entity.toDomain()
    }

    override suspend fun update(transaction: Transaction, expectedCurrency: String): Transaction = database.withTransaction {
        val existing = transactionDao.getById(transaction.id)
            ?: throw TransactionManagementException(TransactionManagementError.NOT_FOUND)
        if (existing.deletedAt != null || transaction.isDeleted) throw TransactionManagementException(TransactionManagementError.NOT_FOUND)
        if (transaction.modifiedAt != existing.modifiedAt) throw TransactionManagementException(TransactionManagementError.CHANGED)
        val account = accountDao.getById(transaction.accountId)
            ?: throw TransactionManagementException(TransactionManagementError.ACCOUNT_UNAVAILABLE)
        if (account.deletedAt != null && transaction.accountId != existing.accountId) {
            throw TransactionManagementException(TransactionManagementError.ACCOUNT_UNAVAILABLE)
        }
        if (account.currency != expectedCurrency) throw TransactionManagementException(TransactionManagementError.ACCOUNT_CURRENCY_CHANGED)
        val category = categoryDao.getById(transaction.categoryId)
            ?: throw TransactionManagementException(TransactionManagementError.CATEGORY_UNAVAILABLE)
        val originalCategory = categoryDao.getById(existing.categoryId)
            ?: throw TransactionManagementException(TransactionManagementError.CATEGORY_UNAVAILABLE)
        if (category.isIncome != originalCategory.isIncome || (category.isArchived && transaction.categoryId != existing.categoryId)) {
            throw TransactionManagementException(TransactionManagementError.CATEGORY_UNAVAILABLE)
        }
        if (transaction.comment != null && transaction.comment.length > 200) {
            throw TransactionManagementException(TransactionManagementError.COMMENT_TOO_LONG)
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
        val entity = try {
            local.toEntity()
        } catch (error: ArithmeticException) {
            throw TransactionManagementException(TransactionManagementError.INVALID_AMOUNT)
        } catch (error: IllegalArgumentException) {
            throw TransactionManagementException(TransactionManagementError.INVALID_AMOUNT)
        }
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

    override suspend fun deleteWithUndo(id: String, expectedModifiedAt: Instant): PendingTransactionDeletion = database.withTransaction {
        val existing = transactionDao.getById(id)
            ?: throw TransactionManagementException(TransactionManagementError.NOT_FOUND)
        if (existing.deletedAt != null) throw TransactionManagementException(TransactionManagementError.NOT_FOUND)
        if (existing.modifiedAt != expectedModifiedAt) throw TransactionManagementException(TransactionManagementError.CHANGED)
        val deletedAt = nextModifiedAt(existing.modifiedAt)
        val deletion = TransactionDeletionEntity(id, deletedAt, clock.now() + 6.seconds, existing.syncStatus)
        deletionDao.insert(deletion)
        check(transactionDao.update(existing.copy(
            deletedAt = deletedAt, modifiedAt = deletedAt, syncStatus = SyncStatus.PENDING_DELETE,
        )) == 1) { "Transaction deletion failed" }
        deletion.toDomain()
    }

    override fun observePendingDeletions(): Flow<List<PendingTransactionDeletion>> =
        deletionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun undoDelete(deletion: PendingTransactionDeletion): Boolean = database.withTransaction {
        val held = deletionDao.getById(deletion.transactionId) ?: return@withTransaction false
        if (held.deletedAt != deletion.deletedAt || held.undoUntil != deletion.undoUntil || clock.now() >= held.undoUntil) {
            return@withTransaction false
        }
        val existing = transactionDao.getById(held.transactionId) ?: return@withTransaction false
        if (existing.deletedAt != held.deletedAt || existing.modifiedAt != held.deletedAt || existing.syncStatus != SyncStatus.PENDING_DELETE) {
            return@withTransaction false
        }
        check(transactionDao.update(existing.copy(
            deletedAt = null, modifiedAt = nextModifiedAt(existing.modifiedAt), syncStatus = held.previousSyncStatus,
        )) == 1) { "Transaction restoration failed" }
        deletionDao.remove(held.transactionId)
        true
    }

    override suspend fun getPendingChanges(): List<Transaction> = database.withTransaction {
        deletionDao.releaseExpired(clock.now())
        transactionDao.getPendingChanges().map { it.toDomain() }
    }

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

    private fun TransactionDeletionEntity.toDomain() = PendingTransactionDeletion(transactionId, deletedAt, undoUntil)
}
