package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.data.mapper.toUpsertRequest
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAccountRepository @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeActive(currency: String?): Flow<List<Account>> =
        accountDao.observeActive(currency).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): Account? = accountDao.getById(id)?.toDomain()

    override suspend fun create(account: Account): Account = database.withTransaction {
        require(account.id.isNotBlank()) { "An account ID is required" }
        require(!account.isDeleted) { "A new account cannot be deleted" }
        check(accountDao.getById(account.id) == null) { "Account already exists" }
        val local = account.copy(
            modifiedAt = nextModifiedAt(),
            createdAt = null,
            updatedAt = null,
            syncStatus = SyncStatus.PENDING_CREATE,
        )
        local.toUpsertRequest()
        val entity = local.toEntity()
        accountDao.insert(entity)
        entity.toDomain()
    }

    override suspend fun update(account: Account): Account = database.withTransaction {
        val existing = requireNotNull(accountDao.getById(account.id)) { "Account does not exist" }
        require(existing.deletedAt == null && !account.isDeleted) { "A deleted account cannot be edited" }
        check(account.modifiedAt == existing.modifiedAt) { "Account changed; reload before editing" }
        require(account.initialBalance.compareTo(existing.toDomain().initialBalance) == 0) {
            "The initial balance cannot be edited; create a balance adjustment"
        }
        if (account.currency != existing.currency) {
            check(!accountDao.hasMovements(account.id)) { "Account currency is locked by its history" }
        }
        val local = existing.toDomain().copy(
            name = account.name,
            iconId = account.iconId,
            currency = account.currency,
            includeInForecast = account.includeInForecast,
            modifiedAt = nextModifiedAt(existing.modifiedAt),
            syncStatus = existing.syncStatus.afterLocalEdit(),
        )
        local.toUpsertRequest()
        val entity = local.toEntity(legacyEmoji = existing.legacyEmoji)
        check(accountDao.update(entity) == 1) { "Account update failed" }
        entity.toDomain()
    }

    override suspend fun softDelete(id: String): Boolean = database.withTransaction {
        val existing = accountDao.getById(id) ?: return@withTransaction false
        if (existing.deletedAt != null) return@withTransaction false
        val now = nextModifiedAt(existing.modifiedAt)
        check(accountDao.update(existing.copy(
            deletedAt = now,
            modifiedAt = now,
            syncStatus = SyncStatus.PENDING_DELETE,
        )) == 1) { "Account deletion failed" }
        true
    }

    override suspend fun getPendingChanges(): List<Account> =
        accountDao.getPendingChanges().map { it.toDomain() }

    override suspend fun acknowledgeUpsert(sent: Account, confirmed: Account): Boolean = database.withTransaction {
        require(sent.syncStatus.isPendingUpsert() && !sent.isDeleted) { "Expected a pending account upsert" }
        require(sent.id == confirmed.id) { "Server response belongs to another account" }
        require(confirmed.createdAt != null && confirmed.updatedAt != null) { "Server timestamps are required" }
        val existing = accountDao.getById(sent.id) ?: return@withTransaction false
        if (existing.toDomain() != sent) return@withTransaction false
        val entity = confirmed.copy(syncStatus = SyncStatus.SYNCED).toEntity(existing.legacyEmoji)
        check(accountDao.update(entity) == 1) { "Account acknowledgement failed" }
        true
    }

    override suspend fun acknowledgeDeletion(sent: Account): Boolean = database.withTransaction {
        require(sent.syncStatus == SyncStatus.PENDING_DELETE && sent.isDeleted) { "Expected a pending deletion" }
        val existing = accountDao.getById(sent.id) ?: return@withTransaction false
        if (existing.toDomain() != sent) return@withTransaction false
        check(accountDao.update(existing.copy(syncStatus = SyncStatus.SYNCED)) == 1) {
            "Account deletion acknowledgement failed"
        }
        true
    }
}
