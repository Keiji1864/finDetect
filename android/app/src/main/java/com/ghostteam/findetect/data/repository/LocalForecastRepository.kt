package com.ghostteam.findetect.data.repository

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
            val start = day.date.minus(28, DateTimeUnit.DAY)
            val room = user.database
            user.gate.use {
                room.withTransaction {
                    val accounts = room.balanceDao().forecastAccounts(currency.name)
                    val expenses = room.transactionDao().forecastExpenses(currency.name, start.atStartOfDayIn(day.zone),
                        day.date.atStartOfDayIn(day.zone)).map { row ->
                        val detail = row.toDomain()
                        ForecastExpense(detail.transaction.transactionDate.toLocalDateTime(day.zone).date,
                            detail.category.id, detail.category.names, detail.transaction.amount)
                    }
                    val zeros = room.activityDayDao().confirmedDates(start.toString(), day.date.toString()).map { LocalDate.parse(it) }
                    calculateForecast(currency, day.date, room.personalSettingsDao().current().incomeSchedule(),
                        accounts.sumOf { it.toDomain().balance }, accounts.size, expenses, zeros)
                }
            }
        }.flowOn(Dispatchers.Default)
}
