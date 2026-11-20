package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.*
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.domain.model.*
import java.util.UUID
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class ForecastHistory internal constructor(
    private val database: AppDatabase,
    private val clock: Clock,
    private val achievements: LocalAchievementRepository,
    private val selectedCurrency: suspend () -> AppCurrency,
) {
    private val dao = database.forecastHistoryDao()

    internal suspend fun <T> write(action: suspend () -> T): T = database.withTransaction {
        capture()
        val result = action()
        checkpoint()
        result
    }

    internal suspend fun checkpoint() = database.withTransaction {
        capture()
        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val earned = database.achievementDao().getEarned().map { it.code }.toSet()
        if (AchievementCode.WITHIN_LIMIT !in earned) {
            qualifyingWeek(dao.completedDays(now))?.let { date ->
                achievements.recordForecastReward(AchievementCode.WITHIN_LIMIT, date, now)
            }
        }
        if (AchievementCode.REACHED_INCOME !in earned) {
            reachedIncome(dao.balances(), today)?.let { date ->
                achievements.recordForecastReward(AchievementCode.REACHED_INCOME, date, now)
            }
        }
    }

    private suspend fun capture() {
        val now = clock.now()
        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date
        val selected = selectedCurrency()
        val schedule = database.personalSettingsDao().current().incomeSchedule()
        for (currency in AppCurrency.entries) {
            val previous = dao.last(currency.name)
            if (currency != selected && previous?.targetDay == null) continue
            val accounts = database.balanceDao().forecastAccounts(currency.name).map { it.toDomain() }
            val ids = JsonArray(accounts.map { it.account.id }.sorted().map { JsonPrimitive(it) }).toString()
            val balance = accounts.sumOf { it.balance }
            val target = schedule?.nextAfter(today)?.takeIf { accounts.isNotEmpty() }?.toString()
            val rule = schedule?.let { "${it.anchor}/${it.repeat}" }
            if (target != null || previous?.targetDay != null) {
                val minor = balance.toMinorUnits()
                if (previous == null || previous.targetDay != target || previous.rule != rule ||
                    previous.accountIds != ids || previous.balanceMinor != minor) {
                    val stamp = maxOf(now.toEpochMilliseconds(), (previous?.stamp ?: 0L) + 1)
                    dao.insert(ForecastBalance(UUID.randomUUID().toString(), currency.name, stamp, today.toString(),
                        target, rule, ids, minor))
                }
            }
            if (currency == selected && !dao.hasDay(currency.name, today.toString())) {
                val start = today.atStartOfDayIn(zone)
                val end = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
                val spent = dao.alreadySpent(currency.name, start, end)
                val limit = if (spent || target == null) null else database.readForecast(currency, LocalDay(today, zone),
                    accounts, schedule).dailyLimit?.toMinorUnits()
                if (spent || limit != null) {
                    dao.insert(ForecastDay(UUID.randomUUID().toString(), currency.name, today.toString(),
                        now.toEpochMilliseconds(), ids, start, end, limit))
                }
            }
        }
    }
}

internal fun qualifyingWeek(days: List<DayEvidence>): LocalDate? {
    var earliest: LocalDate? = null
    for (currencyDays in days.groupBy { it.currency }.values) {
        var previous: LocalDate? = null
        var streak = 0
        for (day in currencyDays) {
            val date = LocalDate.parse(day.day)
            val within = day.limitMinor != null && (day.spent != null || day.confirmed) && (day.spent ?: 0) <= day.limitMinor
            streak = when {
                !within -> 0
                previous?.plus(1, DateTimeUnit.DAY) == date -> streak + 1
                else -> 1
            }
            if (streak == 7) earliest = earliest?.let { minOf(it, date) } ?: date
            previous = date
        }
    }
    return earliest
}

internal fun reachedIncome(history: List<ForecastBalance>, today: LocalDate): LocalDate? {
    var earliest: LocalDate? = null
    for (records in history.groupBy { it.currency }.values) {
        var period: ForecastBalance? = null
        var minimum = 0L
        fun finish(day: LocalDate) {
            val target = period?.targetDay?.let { LocalDate.parse(it) } ?: return
            if (minimum >= 0 && day >= target) earliest = earliest?.let { minOf(it, target) } ?: target
        }
        for (record in records) {
            val day = LocalDate.parse(record.localDay)
            val previous = period
            if (previous == null || previous.targetDay != record.targetDay || previous.rule != record.rule ||
                previous.accountIds != record.accountIds) {
                finish(day)
                period = record
                minimum = record.balanceMinor
            } else minimum = minOf(minimum, record.balanceMinor)
        }
        finish(today)
    }
    return earliest
}
