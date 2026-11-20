package com.ghostteam.findetect.data.repository

import com.ghostteam.findetect.data.local.AppDatabase
import androidx.room.withTransaction
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.session.UserDataScope
import com.ghostteam.findetect.domain.model.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

class LocalForecastRepository @Inject constructor(private val user: UserDataScope) {
    fun observe(currency: AppCurrency, day: LocalDay) = user.database.invalidationTracker
        .createFlow("accounts", "transactions", "categories", "balance_adjustments", "activity_days", "personal_settings")
        .map {
            val room = user.database
            user.gate.use {
                room.withTransaction {
                    val accounts = room.balanceDao().forecastAccounts(currency.name).map { it.toDomain() }
                    room.readForecast(currency, day, accounts, room.personalSettingsDao().current().incomeSchedule())
                }
            }
        }.flowOn(Dispatchers.Default)
}

internal suspend fun AppDatabase.readForecast(currency: AppCurrency, day: LocalDay, accounts: List<AccountBalance>,
    schedule: IncomeSchedule?): SpendingForecast {
    val start = day.date.minus(28, DateTimeUnit.DAY)
    val expenses = transactionDao().forecastExpenses(currency.name, start.atStartOfDayIn(day.zone),
        day.date.atStartOfDayIn(day.zone)).map { row ->
        val detail = row.toDomain()
        ForecastExpense(detail.transaction.transactionDate.toLocalDateTime(day.zone).date,
            detail.category.id, detail.category.names, detail.transaction.amount)
    }
    val zeros = activityDayDao().confirmedDates(start.toString(), day.date.toString()).map { LocalDate.parse(it) }
    return calculateForecast(currency, day.date, schedule, accounts.sumOf { it.balance }, accounts.size, expenses, zeros)
}
