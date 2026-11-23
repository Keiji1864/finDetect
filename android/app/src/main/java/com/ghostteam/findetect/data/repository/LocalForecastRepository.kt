package com.ghostteam.findetect.data.repository

import com.ghostteam.findetect.data.local.AppDatabase
import androidx.room.withTransaction
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.session.UserDataScope
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.data.local.dao.ForecastPlanRow
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

class LocalForecastRepository @Inject constructor(private val user: UserDataScope, private val clock: Clock) {
    fun observe(currency: AppCurrency, day: LocalDay) = user.database.invalidationTracker
        .createFlow("accounts", "transactions", "categories", "balance_adjustments", "activity_days", "personal_settings", "forecast_plans")
        .map {
            val room = user.database
            user.gate.use {
                room.withTransaction {
                    val accounts = room.balanceDao().forecastAccounts(currency.name).map { it.toDomain() }
                    room.readForecast(currency, day, accounts, room.personalSettingsDao().current().incomeSchedule())
                }
            }
        }.flowOn(Dispatchers.Default)

    suspend fun save(forecast: SpendingForecast, plan: ForecastPlan) = user.gate.use {
        val room = user.database
        user.history.write {
            val now = clock.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val target = room.personalSettingsDao().current().incomeSchedule()?.nextAfter(today)
            check(target != null && target == forecast.income)
            val dao = room.forecastPlanDao()
            check(dao.current(forecast.currency.name).forDate(target) == forecast.plan)
            dao.save(ForecastPlanRow(forecast.currency.name, plan.savings?.toMinorUnits(),
                plan.payments?.toMinorUnits(), target.toString().takeIf { plan.payments != null },
                plan.manualDaily?.toMinorUnits(), now.toString()))
        }
    }
}

internal suspend fun AppDatabase.readForecast(currency: AppCurrency, day: LocalDay, accounts: List<AccountBalance>,
    schedule: IncomeSchedule?): SpendingForecast {
    val start = day.date.minus(28, DateTimeUnit.DAY)
    val todayStart = day.date.atStartOfDayIn(day.zone)
    val end = day.date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(day.zone)
    val rows = transactionDao().forecastExpenses(currency.name, start.atStartOfDayIn(day.zone), end)
    val (todayRows, previousRows) = rows.partition { it.transaction.transactionDate >= todayStart }
    val expenses = previousRows.map { row ->
        val detail = row.toDomain()
        ForecastExpense(detail.transaction.transactionDate.toLocalDateTime(day.zone).date,
            detail.category.id, detail.category.names, detail.transaction.amount)
    }
    val zeros = activityDayDao().confirmedDates(start.toString(), day.date.toString()).map { LocalDate.parse(it) }
    val plan = forecastPlanDao().current(currency.name).forDate(schedule?.nextAfter(day.date))
    val selected = accounts.mapTo(mutableSetOf()) { it.account.id }
    return calculateForecast(currency, day.date, schedule, accounts.sumOf { it.balance }, accounts.size, expenses,
        zeros, plan, todayRows.sumOf { it.transaction.amountMinor.toMoneyAmount() },
        todayRows.filter { it.transaction.accountId in selected }.sumOf { it.transaction.amountMinor.toMoneyAmount() })
}

private fun ForecastPlanRow?.forDate(date: LocalDate?) = ForecastPlan(
    savings = this?.savingsMinor?.toMoneyAmount(),
    payments = this?.takeIf { it.paymentDate == date?.toString() }?.paymentsMinor?.toMoneyAmount(),
    manualDaily = this?.manualDailyMinor?.toMoneyAmount(),
)
