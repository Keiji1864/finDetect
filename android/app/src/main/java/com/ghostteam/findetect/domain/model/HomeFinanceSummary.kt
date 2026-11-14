package com.ghostteam.findetect.domain.model

import java.math.BigDecimal

data class HomeFinanceSummary(
    val todaySpent: BigDecimal,
    val yesterdaySpent: BigDecimal,
    val recentTransactions: List<TransactionDetails>,
) {
    val spendingDifference: BigDecimal get() = todaySpent - yesterdaySpent
}
