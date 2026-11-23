package com.ghostteam.findetect.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

data class ForecastPlan(
    val savings: BigDecimal? = null,
    val payments: BigDecimal? = null,
    val manualDaily: BigDecimal? = null,
)

enum class ForecastStatus { NO_DATE, DATE_PASSED, NO_ACCOUNTS, READY }

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
    val plan: ForecastPlan,
    val spentToday: BigDecimal,
    val usualDaily: BigDecimal? = null,
    val dailyLimit: BigDecimal? = null,
    val recommended: BigDecimal? = null,
    val projectedBalance: BigDecimal? = null,
    val growth: CategoryGrowth? = null,
) {
    val daysUntilIncome get() = income?.let { it.toEpochDays() - today.toEpochDays() }
    val leftToday get() = recommended?.let { (it - spentToday).max(BigDecimal.ZERO) }
    val available get() = balance - (plan.payments ?: BigDecimal.ZERO) - (plan.savings ?: BigDecimal.ZERO)
}

fun calculateForecast(
    currency: AppCurrency, today: LocalDate, schedule: IncomeSchedule?, balance: BigDecimal, accounts: Int,
    expenses: List<ForecastExpense>, zeroDays: List<LocalDate>, plan: ForecastPlan,
    spentToday: BigDecimal, spentFromSelectedToday: BigDecimal,
): SpendingForecast {
    val daily = zeroDays.associateWith { BigDecimal.ZERO }.toMutableMap()
    for (expense in expenses) daily.merge(expense.date, expense.amount, BigDecimal::add)
    val income = schedule?.nextAfter(today)
    val status = when {
        schedule == null -> ForecastStatus.NO_DATE
        income == null -> ForecastStatus.DATE_PASSED
        accounts == 0 -> ForecastStatus.NO_ACCOUNTS
        else -> ForecastStatus.READY
    }
    val base = SpendingForecast(currency, today, income, balance, accounts, daily.size, status, plan, spentToday)
    if (status != ForecastStatus.READY) return base
    val days = requireNotNull(base.daysUntilIncome).toBigDecimal()
    val laterDays = days - BigDecimal.ONE
    // The current balance already includes today's purchases on the selected accounts.
    val startFunds = base.available + spentFromSelectedToday
    val evenShare = startFunds.divide(days, 2, RoundingMode.DOWN)
    val affordable = when {
        base.available.signum() <= 0 -> BigDecimal.ZERO
        laterDays.signum() > 0 -> evenShare.min(base.available.divide(laterDays, 2, RoundingMode.DOWN))
        else -> evenShare
    }.max(BigDecimal.ZERO).dailyAmount(currency)
    val usual = if (daily.size >= 3) usualDaily(daily, today).dailyAmount(currency) else null
    val recommended = (usual ?: plan.manualDaily)?.min(affordable)
    val projected = recommended?.let {
        balance - (plan.payments ?: BigDecimal.ZERO) - (it - spentToday).max(BigDecimal.ZERO) - it * laterDays
    }
    return base.copy(usualDaily = usual, recommended = recommended, projectedBalance = projected,
        dailyLimit = affordable.takeIf { plan.savings != null },
        growth = if (daily.size == 28) largestGrowth(expenses, today.minus(14, DateTimeUnit.DAY)) else null)
}

private fun BigDecimal.dailyAmount(currency: AppCurrency): BigDecimal =
    setScale(if (currency == AppCurrency.RUB && this >= BigDecimal.ONE) 0 else 2, RoundingMode.DOWN)

private fun usualDaily(daily: Map<LocalDate, BigDecimal>, today: LocalDate): BigDecimal {
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
    return weighted.divide(totalWeight, 8, RoundingMode.DOWN)
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
