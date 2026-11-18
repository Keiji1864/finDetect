package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import kotlinx.datetime.LocalDate

data class TransactionDetails(
    val transaction: Transaction,
    val category: Category,
    val currency: String,
)

data class TransactionsDay(val date: LocalDate, val transactions: List<TransactionDetails>)

data class CategoryTotal(val category: Category, val amount: BigDecimal, val percentage: Int)

data class TransactionsSummary(
    val total: BigDecimal,
    val days: List<TransactionsDay>,
    val categories: List<CategoryTotal>,
)
