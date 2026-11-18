package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import kotlinx.datetime.LocalDate

enum class AnalyticsGranularity { DAY, WEEK, MONTH }

/** Inclusive calendar bounds; empty intervals are retained with a zero total. */
data class AnalyticsBucket(
    val start: LocalDate,
    val end: LocalDate,
    val amounts: Map<String, BigDecimal>,
) {
    val total: BigDecimal get() = amounts.values.fold(BigDecimal.ZERO.setScale(2), BigDecimal::add)
}

data class AnalyticsSummary(
    val transactions: TransactionsSummary,
    val granularity: AnalyticsGranularity,
    val buckets: List<AnalyticsBucket>,
)
