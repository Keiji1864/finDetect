package com.ghostteam.findetect.feature.accounts

import com.ghostteam.findetect.domain.model.AccountBalance

data class AccountEditDraft(
    val name: String = "",
    val currency: String = "RUB",
    val iconId: String = "wallet",
    val includeInForecast: Boolean = true,
)

enum class AccountEditorError { LOAD, SAVE, CONFLICT, CURRENCY_LOCKED, UNAVAILABLE, DELETE, BALANCE }

data class AccountEditorState(
    val open: Boolean = false,
    val balance: AccountBalance? = null,
    val draft: AccountEditDraft = AccountEditDraft(),
    val currencyLocked: Boolean = true,
    val actualBalanceText: String = "",
    val loading: Boolean = false,
    val readError: Boolean = false,
    val busy: Boolean = false,
    val error: AccountEditorError? = null,
    val confirmDelete: Boolean = false,
    val actualBalanceValid: Boolean = false,
)
