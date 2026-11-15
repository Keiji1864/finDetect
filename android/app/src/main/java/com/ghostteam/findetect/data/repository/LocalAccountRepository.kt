package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.local.dao.AccountDeletionDao
import com.ghostteam.findetect.data.local.entity.AccountDeletionEntity
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.data.mapper.toUpsertRequest
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountManagementError
import com.ghostteam.findetect.domain.model.AccountManagementException
import com.ghostteam.findetect.domain.model.PendingAccountDeletion
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

class LocalAccountRepository @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val deletionDao: AccountDeletionDao,
    private val clock: Clock,
) : AccountRepository {
    override fun observeActive(currency: String?): Flow<List<Account>> =
        accountDao.observeActive(currency).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): Account? = accountDao.getById(id)?.toDomain()

    override fun observeHasMovements(id: String): Flow<Boolean> = accountDao.observeHasMovements(id)

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
        val existing = accountDao.getById(account.id)
            ?: throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (existing.deletedAt != null || account.isDeleted) throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (account.modifiedAt != existing.modifiedAt) throw AccountManagementException(AccountManagementError.CHANGED)
        if (account.initialBalance.compareTo(existing.toDomain().initialBalance) != 0) {
            throw AccountManagementException(AccountManagementError.INVALID_BALANCE)
        }
        if (account.currency != existing.currency) {
            if (accountDao.hasMovements(account.id)) throw AccountManagementException(AccountManagementError.CURRENCY_LOCKED)
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

    override suspend fun deleteWithUndo(id: String, expectedModifiedAt: Instant): PendingAccountDeletion = database.withTransaction {
        val existing = accountDao.getById(id) ?: throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (existing.deletedAt != null) throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (existing.modifiedAt != expectedModifiedAt) throw AccountManagementException(AccountManagementError.CHANGED)
        val deletedAt = nextModifiedAt(existing.modifiedAt)
        val deletion = AccountDeletionEntity(id, existing.name, deletedAt, clock.now() + 6.seconds, existing.syncStatus)
        deletionDao.insert(deletion)
        check(accountDao.update(existing.copy(
            deletedAt = deletedAt,
            modifiedAt = deletedAt,
            syncStatus = SyncStatus.PENDING_DELETE,
        )) == 1) { "Account deletion failed" }
        deletion.toDomain()
    }

    override fun observePendingDeletions(): Flow<List<PendingAccountDeletion>> =
        deletionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun undoDelete(deletion: PendingAccountDeletion): Boolean = database.withTransaction {
        val held = deletionDao.getById(deletion.accountId) ?: return@withTransaction false
        if (held.deletedAt != deletion.deletedAt || held.undoUntil != deletion.undoUntil || clock.now() >= held.undoUntil) {
            return@withTransaction false
        }
        val existing = accountDao.getById(held.accountId) ?: return@withTransaction false
        if (existing.deletedAt != held.deletedAt || existing.modifiedAt != held.deletedAt || existing.syncStatus != SyncStatus.PENDING_DELETE) {
            return@withTransaction false
        }
        check(accountDao.update(existing.copy(
            deletedAt = null,
            modifiedAt = nextModifiedAt(existing.modifiedAt),
            syncStatus = held.previousSyncStatus,
        )) == 1) { "Account restoration failed" }
        deletionDao.remove(held.accountId)
        true
    }

    override suspend fun getPendingChanges(): List<Account> = database.withTransaction {
        // Release is durable: a clock rollback cannot reopen undo after sync obtained a deletion.
        deletionDao.releaseExpired(clock.now())
        accountDao.getPendingChanges().map { it.toDomain() }
    }

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

    private fun AccountDeletionEntity.toDomain() = PendingAccountDeletion(accountId, name, undoUntil, deletedAt)
}
