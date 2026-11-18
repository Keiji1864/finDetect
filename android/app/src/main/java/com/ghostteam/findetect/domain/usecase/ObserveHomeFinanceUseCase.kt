package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.HomeFinanceSummary
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Local calendar days use the operation date; recent rows are independent of the tab period. */
class ObserveHomeFinanceUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(
        currency: String,
        today: LocalDate,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): Flow<HomeFinanceSummary> {
        require(AppCurrency.entries.any { it.name == currency }) { "Unsupported currency" }
        val startOfToday = today.atStartOfDayIn(zone)
        val todayFilter = TransactionFilter(
            currency = currency,
            isIncome = false,
            fromInclusive = startOfToday,
            toExclusive = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone),
        )
        val yesterdayFilter = todayFilter.copy(
            fromInclusive = today.minus(1, DateTimeUnit.DAY).atStartOfDayIn(zone),
            toExclusive = startOfToday,
        )
        return combine(
            repository.observeTotalAmount(todayFilter),
            repository.observeTotalAmount(yesterdayFilter),
            repository.observeDetails(TransactionFilter(currency = currency), limit = 3),
        ) { todaySpent, yesterdaySpent, recent ->
            HomeFinanceSummary(todaySpent, yesterdaySpent, recent)
        }
    }
}
