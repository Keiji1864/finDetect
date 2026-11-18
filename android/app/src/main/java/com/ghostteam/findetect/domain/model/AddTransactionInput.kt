package com.ghostteam.findetect.domain.model

import kotlinx.datetime.LocalDate

/** Raw form input; it contains no UI types and can be submitted by any caller. */
data class AddTransactionInput(
    val accountId: String?,
    val categoryId: String?,
    val isIncome: Boolean,
    val amountText: String,
    val date: LocalDate,
    val comment: String? = null,
)
