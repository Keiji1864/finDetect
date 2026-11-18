package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant

enum class AccountManagementError {
    INVALID_NAME, INVALID_CURRENCY, INVALID_ICON, INVALID_BALANCE,
    CURRENCY_LOCKED, CHANGED, NOT_FOUND, STORAGE_FAILURE,
}

class AccountManagementException(val error: AccountManagementError) : IllegalStateException(error.name)

data class PendingAccountDeletion(
    val accountId: String,
    val name: String,
    val undoUntil: Instant,
    val deletedAt: Instant,
)

sealed interface UpdateAccountResult {
    data class Success(val account: Account) : UpdateAccountResult
    data class Failure(val error: AccountManagementError) : UpdateAccountResult
}

sealed interface AdjustAccountBalanceResult {
    data class Success(val adjustment: BalanceAdjustment?) : AdjustAccountBalanceResult
    data class Failure(val error: AccountManagementError) : AdjustAccountBalanceResult
}
