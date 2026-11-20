package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

enum class ForecastStatus { NO_DATE, DATE_PASSED, NO_ACCOUNTS, COLLECTING, GREEN, YELLOW, RED }
enum class ForecastConfidence { PRELIMINARY, STANDARD, HIGH }

data class ForecastExpense(val date: LocalDate, val categoryId: String, val names: CategoryNames, val amount: BigDecimal)
data class CategoryGrowth(val names: CategoryNames, val increase: BigDecimal)
data class SpendingForecast(
    val currency: AppCurrency,
    val today: LocalDate,
    val income: LocalDate?,
    val balance: BigDecimal,
    val accounts: Int,
    val knownDays: Int,
    val status: ForecastStatus,
    val confidence: ForecastConfidence,
    val dailyLimit: BigDecimal? = null,
    val expectedDaily: BigDecimal? = null,
    val growth: CategoryGrowth? = null,
) {
    val daysUntilIncome get() = income?.let { it.toEpochDays() - today.toEpochDays() }
}

fun calculateForecast(
    currency: AppCurrency, today: LocalDate, schedule: IncomeSchedule?, balance: BigDecimal, accounts: Int,
    expenses: List<ForecastExpense>, zeroDays: List<LocalDate>,
): SpendingForecast {
    val daily = zeroDays.associateWith { BigDecimal.ZERO }.toMutableMap()
    for (expense in expenses) daily.merge(expense.date, expense.amount, BigDecimal::add)
    val midpoint = today.minus(14, DateTimeUnit.DAY)
    val recentDays = daily.keys.count { it >= midpoint }
    val confidence = when {
        recentDays < 7 -> ForecastConfidence.PRELIMINARY
        daily.size >= 21 -> ForecastConfidence.HIGH
        else -> ForecastConfidence.STANDARD
    }
    val income = schedule?.nextAfter(today)
    val unavailable = when {
        schedule == null -> ForecastStatus.NO_DATE
        income == null -> ForecastStatus.DATE_PASSED
        accounts == 0 -> ForecastStatus.NO_ACCOUNTS
        daily.size < 3 -> ForecastStatus.COLLECTING
        else -> null
    }
    val base = SpendingForecast(currency, today, income, balance, accounts, daily.size,
        unavailable ?: ForecastStatus.GREEN, confidence)
    if (unavailable != null) return base
    val days = requireNotNull(base.daysUntilIncome).toBigDecimal()
    val sorted = daily.entries.sortedWith(compareBy<Map.Entry<LocalDate, BigDecimal>> { it.value }.thenBy { it.key })
    val trimmed = sorted.take(sorted.size - sorted.size / 10)
    var totalWeight = BigDecimal.ZERO
    var weighted = BigDecimal.ZERO
    for ((date, amount) in trimmed) {
        val age = today.toEpochDays() - date.toEpochDays()
        val weight = BigDecimal.valueOf(0.5.pow(age / 7.0))
        totalWeight += weight
        weighted += amount * weight
    }
    val expected = weighted.divide(totalWeight, 8, RoundingMode.HALF_UP)
    val remaining = balance - expected * days
    val cautious = sorted[(3 * sorted.size - 1) / 4].value
    val status = when {
        remaining.signum() < 0 -> ForecastStatus.RED
        balance.signum() == 0 || remaining < expected * 3.toBigDecimal() || balance < cautious * days -> ForecastStatus.YELLOW
        else -> ForecastStatus.GREEN
    }
    val limit = balance.max(BigDecimal.ZERO).multiply(BigDecimal("0.90")).divide(days, 2, RoundingMode.DOWN)
    val growth = if (daily.size == 28) largestGrowth(expenses, midpoint) else null
    return base.copy(status = status, dailyLimit = limit, expectedDaily = expected, growth = growth)
}

private fun largestGrowth(expenses: List<ForecastExpense>, midpoint: LocalDate): CategoryGrowth? {
    val changes = expenses.groupBy { it.categoryId }.values.map { category ->
        val recent = category.filter { it.date >= midpoint }.sumOf { it.amount }
        val previous = category.filter { it.date < midpoint }.sumOf { it.amount }
        CategoryGrowth(category.first().names, recent - previous) to recent
    }
    return changes.filter { it.first.increase.signum() > 0 }
        .maxWithOrNull(compareBy<Pair<CategoryGrowth, BigDecimal>> { it.first.increase }.thenBy { it.second })?.first
}
