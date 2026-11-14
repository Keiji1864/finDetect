package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryTotal
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.model.TransactionsDay
import com.ghostteam.findetect.domain.model.TransactionsSummary
import com.ghostteam.findetect.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class ObserveTransactionsSummaryUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(currency: String, isIncome: Boolean, start: LocalDate, end: LocalDate,
        zone: TimeZone = TimeZone.currentSystemDefault()): Flow<TransactionsSummary> {
        require(start <= end) { "Period start must not follow its end" }
        require(AppCurrency.entries.any { it.name == currency }) { "Unsupported currency" }
        val filter = TransactionFilter(currency = currency, isIncome = isIncome,
            fromInclusive = start.atStartOfDayIn(zone),
            toExclusive = end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone))
        return repository.observeDetails(filter).map { records ->
            val total = records.fold(BigDecimal.ZERO.setScale(2)) { sum, item -> sum + item.transaction.amount }
            val grouped = records.groupBy { it.category.id }.map { (_, items) ->
                items.first().category to items.fold(BigDecimal.ZERO.setScale(2)) { sum, item -> sum + item.transaction.amount }
            }.sortedWith(compareByDescending<Pair<Category, BigDecimal>> { it.second }
                .thenBy { it.first.id })
            // Largest remainders keep integer legend percentages at exactly 100 without rounding money.
            val percentages = grouped.map { (_, amount) ->
                if (total.signum() == 0) 0 else amount.multiply(BigDecimal(100)).divide(total, 0, RoundingMode.DOWN).toInt()
            }.toMutableList()
            if (total.signum() > 0) {
                grouped.indices.sortedWith(compareByDescending<Int> { index ->
                    grouped[index].second.multiply(BigDecimal(100)) - total.multiply(BigDecimal(percentages[index]))
                }.thenBy { grouped[it].first.id }).take(100 - percentages.sum()).forEach { percentages[it] += 1 }
            }
            TransactionsSummary(
                total = total,
                days = records.groupBy { it.transaction.transactionDate.toLocalDateTime(zone).date }
                    .toSortedMap(compareByDescending<LocalDate> { it }).map { (date, items) -> TransactionsDay(date, items) },
                categories = grouped.mapIndexed { index, (category, amount) -> CategoryTotal(category, amount, percentages[index]) },
            )
        }.flowOn(Dispatchers.Default)
    }
}
