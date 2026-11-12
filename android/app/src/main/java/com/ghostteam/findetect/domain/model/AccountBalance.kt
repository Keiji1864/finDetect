package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Derived amounts for the account's entire history, independent of the UI's selected period. */
data class AccountBalance(
    val account: Account,
    val income: BigDecimal,
    val expense: BigDecimal,
    val adjustments: BigDecimal,
) {
    val balance: BigDecimal = (account.initialBalance + income - expense + adjustments)
        .setScale(2, RoundingMode.UNNECESSARY)
}

/** Active accounts of one currency, read together with their balances. */
data class BalanceSummary(
    val currency: String,
    val accounts: List<AccountBalance>,
) {
    init {
        require(accounts.all { it.account.currency == currency && !it.account.isDeleted }) {
            "A balance summary requires active accounts of the same currency"
        }
    }

    val total: BigDecimal = accounts.fold(BigDecimal.ZERO.setScale(2)) { total, account ->
        total + account.balance
    }
}
