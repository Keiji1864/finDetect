package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AnalyticsBucket
import com.ghostteam.findetect.domain.model.AnalyticsGranularity
import com.ghostteam.findetect.domain.model.AnalyticsSummary
import com.ghostteam.findetect.domain.model.TransactionsSummary
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The total, category shares, history and chart all use one filtered Room snapshot. */
class ObserveAnalyticsUseCase @Inject constructor(private val observeSummary: ObserveTransactionsSummaryUseCase) {
    operator fun invoke(
        currency: String,
        isIncome: Boolean,
        start: LocalDate,
        end: LocalDate,
        zone: TimeZone,
    ): Flow<AnalyticsSummary> = observeSummary(currency, isIncome, start, end, zone)
        .map { buildAnalyticsSummary(it, isIncome, start, end) }
        .flowOn(Dispatchers.Default)
}

/** Income always uses calendar months; expense weeks start on Monday. Money stays decimal. */
internal fun buildAnalyticsSummary(
    summary: TransactionsSummary,
    isIncome: Boolean,
    start: LocalDate,
    end: LocalDate,
): AnalyticsSummary {
    require(start <= end)
    val days = start.daysUntil(end) + 1
    val granularity = when {
        isIncome -> AnalyticsGranularity.MONTH
        days <= 31 -> AnalyticsGranularity.DAY
        days <= 92 -> AnalyticsGranularity.WEEK
        else -> AnalyticsGranularity.MONTH
    }
    fun boundary(date: LocalDate): LocalDate = when (granularity) {
        AnalyticsGranularity.DAY -> date
        AnalyticsGranularity.WEEK -> date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
        AnalyticsGranularity.MONTH -> LocalDate(date.year, date.monthNumber, 1)
    }
    fun next(date: LocalDate): LocalDate = when (granularity) {
        AnalyticsGranularity.DAY -> date.plus(1, DateTimeUnit.DAY)
        AnalyticsGranularity.WEEK -> date.plus(7, DateTimeUnit.DAY)
        AnalyticsGranularity.MONTH -> date.plus(1, DateTimeUnit.MONTH)
    }

    val amounts = mutableMapOf<LocalDate, MutableMap<String, BigDecimal>>()
    summary.days.forEach { day ->
        val bucket = amounts.getOrPut(boundary(day.date)) { mutableMapOf() }
        day.transactions.forEach { item ->
            val id = item.category.id
            bucket[id] = (bucket[id] ?: BigDecimal.ZERO.setScale(2)) + item.transaction.amount
        }
    }
    val buckets = buildList {
        var cursor = boundary(start)
        while (cursor <= end) {
            val following = next(cursor)
            add(AnalyticsBucket(
                start = maxOf(cursor, start),
                end = minOf(following.minus(1, DateTimeUnit.DAY), end),
                amounts = amounts[cursor]?.toMap().orEmpty(),
            ))
            cursor = following
        }
    }
    return AnalyticsSummary(summary, granularity, buckets)
}
