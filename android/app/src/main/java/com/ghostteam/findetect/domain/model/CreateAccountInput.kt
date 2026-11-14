package com.ghostteam.findetect.domain.model

import com.ghostteam.findetect.domain.validation.InitialBalanceParser

data class CreateAccountInput(
    val name: String = "",
    val currency: String = AppCurrency.RUB.name,
    val initialBalanceText: String = "0",
    val iconId: String = FinanceIcon.WALLET.key,
    val includeInForecast: Boolean = true,
) {
    fun validationError(parser: InitialBalanceParser): AccountCreationError? = when {
        name.trim().length !in 1..100 -> AccountCreationError.INVALID_NAME
        AppCurrency.entries.none { it.name == currency } -> AccountCreationError.INVALID_CURRENCY
        parser.parse(initialBalanceText) == null -> AccountCreationError.INVALID_BALANCE
        FinanceIcon.entries.none { it.key == iconId } -> AccountCreationError.INVALID_ICON
        else -> null
    }
}

enum class AccountCreationError { INVALID_NAME, INVALID_CURRENCY, INVALID_BALANCE, INVALID_ICON, STORAGE_FAILURE }

sealed interface CreateAccountResult {
    data class Success(val account: Account) : CreateAccountResult
    data class Failure(val error: AccountCreationError) : CreateAccountResult
}
