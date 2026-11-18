package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.AccountDao
import com.ghostteam.findetect.data.local.dao.AdjustmentDao
import com.ghostteam.findetect.data.local.dao.BalanceDao
import com.ghostteam.findetect.data.local.dao.BalanceConfirmationDao
import com.ghostteam.findetect.data.local.entity.BalanceConfirmationEntity
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.data.mapper.checkedMoney
import com.ghostteam.findetect.data.mapper.MoneyRule
import com.ghostteam.findetect.domain.model.AccountManagementError
import com.ghostteam.findetect.domain.model.AccountManagementException
import com.ghostteam.findetect.domain.model.AdjustmentFilter
import com.ghostteam.findetect.domain.model.BalanceAdjustment
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.toMoneyAmount
import com.ghostteam.findetect.domain.model.toMinorUnits
import com.ghostteam.findetect.domain.repository.BalanceAdjustmentRepository
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class LocalBalanceAdjustmentRepository @Inject constructor(
    private val database: AppDatabase,
    private val adjustmentDao: AdjustmentDao,
    private val accountDao: AccountDao,
    private val balanceDao: BalanceDao,
    private val confirmationDao: BalanceConfirmationDao,
    private val clock: Clock,
) : BalanceAdjustmentRepository {
    override fun observe(filter: AdjustmentFilter): Flow<List<BalanceAdjustment>> = adjustmentDao.observeFiltered(
        currency = filter.currency,
        accountId = filter.accountId,
        fromInclusive = filter.fromInclusive,
        toExclusive = filter.toExclusive,
    ).map { rows -> rows.map { it.toDomain() } }

    override fun observeTotalAmount(filter: AdjustmentFilter): Flow<BigDecimal> {
        require(!filter.currency.isNullOrBlank()) { "A total requires a currency" }
        return adjustmentDao.observeTotalMinor(
            currency = filter.currency,
            accountId = filter.accountId,
            fromInclusive = filter.fromInclusive,
            toExclusive = filter.toExclusive,
        ).map { it.toMoneyAmount() }
    }

    override suspend fun getById(id: String): BalanceAdjustment? = adjustmentDao.getById(id)?.toDomain()

    override suspend fun create(adjustment: BalanceAdjustment): BalanceAdjustment = database.withTransaction {
        require(adjustment.id.isNotBlank()) { "An adjustment ID is required" }
        val entity = adjustment.copy(createdAt = null, syncStatus = SyncStatus.PENDING_CREATE).toEntity()
        val existing = adjustmentDao.getById(adjustment.id)
        if (existing != null) {
            check(existing.accountId == entity.accountId && existing.amountMinor == entity.amountMinor &&
                existing.occurredAt == entity.occurredAt) { "An immutable adjustment cannot be overwritten" }
            return@withTransaction existing.toDomain()
        }
        val account = requireNotNull(accountDao.getById(adjustment.accountId)) { "Account does not exist" }
        require(account.deletedAt == null) { "A new adjustment requires an active account" }
        adjustmentDao.insert(entity)
        entity.toDomain()
    }

    override suspend fun setActualBalance(
        accountId: String,
        actual: BigDecimal,
        currency: String,
        requestId: String,
    ): BalanceAdjustment? = database.withTransaction {
        require(requestId.isNotBlank()) { "A confirmation request ID is required" }
        val actualMinor = try {
            checkedMoney(actual, MoneyRule.NON_NEGATIVE).toMinorUnits()
        } catch (_: IllegalArgumentException) {
            throw AccountManagementException(AccountManagementError.INVALID_BALANCE)
        } catch (_: ArithmeticException) {
            throw AccountManagementException(AccountManagementError.INVALID_BALANCE)
        }
        val receipt = confirmationDao.getById(requestId)
        if (receipt != null) {
            if (receipt.accountId != accountId || receipt.actualMinor != actualMinor || receipt.currency != currency) {
                throw AccountManagementException(AccountManagementError.CHANGED)
            }
            return@withTransaction receipt.adjustmentId?.let { id ->
                checkNotNull(adjustmentDao.getById(id)) { "Confirmation adjustment is missing" }.toDomain()
            }
        }
        val current = balanceDao.getAccount(accountId)?.toDomain()
            ?: throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (current.account.isDeleted) throw AccountManagementException(AccountManagementError.NOT_FOUND)
        if (current.account.currency != currency) throw AccountManagementException(AccountManagementError.CHANGED)
        val difference = actual - current.balance
        val adjustment = if (difference.signum() == 0) null else {
            val proposed = BalanceAdjustment(requestId, accountId, difference, clock.now())
            val entity = try {
                proposed.toEntity()
            } catch (_: IllegalArgumentException) {
                throw AccountManagementException(AccountManagementError.INVALID_BALANCE)
            } catch (_: ArithmeticException) {
                throw AccountManagementException(AccountManagementError.INVALID_BALANCE)
            }
            adjustmentDao.insert(entity)
            entity.toDomain()
        }
        confirmationDao.insert(BalanceConfirmationEntity(requestId, accountId, currency, actualMinor, adjustment?.id))
        adjustment
    }

    override suspend fun getPendingChanges(): List<BalanceAdjustment> =
        adjustmentDao.getPendingChanges().map { it.toDomain() }

    override suspend fun acknowledgeCreate(sent: BalanceAdjustment, confirmed: BalanceAdjustment): Boolean =
        database.withTransaction {
            require(sent.syncStatus == SyncStatus.PENDING_CREATE) { "Expected a pending adjustment" }
            require(sent.id == confirmed.id) { "Server response belongs to another adjustment" }
            val createdAt = requireNotNull(confirmed.createdAt) { "Server creation timestamp is required" }
            require(sent.accountId == confirmed.accountId && sent.amount.compareTo(confirmed.amount) == 0 &&
                sent.occurredAt == confirmed.occurredAt) { "Server changed an immutable adjustment" }
            val existing = adjustmentDao.getById(sent.id) ?: return@withTransaction false
            if (existing.toDomain() != sent) return@withTransaction false
            adjustmentDao.acknowledgeCreated(sent.id, createdAt) == 1
        }
}
