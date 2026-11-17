package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.ActivityDayDao
import com.ghostteam.findetect.data.local.entity.NotificationDayEntity
import com.ghostteam.findetect.domain.model.ConfirmNoSpendingResult
import com.ghostteam.findetect.domain.model.DayActivity
import com.ghostteam.findetect.domain.repository.ActivityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class LocalActivityRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: ActivityDayDao,
    private val clock: Clock,
    private val xp: LocalXpRepository,
) : ActivityRepository {
    override fun observeActiveDates() = dao.observeActiveDates()
        .distinctUntilChanged()
        .map { dates -> dates.map { LocalDate.parse(it) } }

    override fun observeDay(date: LocalDate, zone: TimeZone) = combine(
        dao.observe(date.toString()),
        dao.observeHasExpense(date.atStartOfDayIn(zone), date.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)),
    ) { activity, hasExpense -> DayActivity(activity?.noSpendingConfirmed == true, hasExpense) }

    override suspend fun confirmNoSpending(expectedDate: LocalDate): ConfirmNoSpendingResult = database.withTransaction {
        val now = clock.now()
        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date
        if (today != expectedDate) return@withTransaction ConfirmNoSpendingResult.DAY_CHANGED
        if (dao.get(today.toString())?.noSpendingConfirmed == true) {
            return@withTransaction ConfirmNoSpendingResult.ALREADY_CONFIRMED
        }
        if (dao.hasExpense(today.atStartOfDayIn(zone), today.plus(DatePeriod(days = 1)).atStartOfDayIn(zone))) {
            return@withTransaction ConfirmNoSpendingResult.HAS_EXPENSE
        }
        dao.confirmNoSpending(today.toString(), now)
        xp.recordNoSpending(today, now)
        ConfirmNoSpendingResult.CONFIRMED
    }

    override suspend fun claimDailyReminder(date: LocalDate, zone: TimeZone): Boolean = database.withTransaction {
        val currentZone = TimeZone.currentSystemDefault()
        if (currentZone != zone || clock.now().toLocalDateTime(currentZone).date != date) return@withTransaction false
        val activity = dao.get(date.toString())
        if (activity?.hadTransaction == true || activity?.noSpendingConfirmed == true) return@withTransaction false
        if (dao.hasEnteredTransaction(date.atStartOfDayIn(zone), date.plus(DatePeriod(days = 1)).atStartOfDayIn(zone))) {
            return@withTransaction false
        }
        dao.claimNotification(NotificationDayEntity(date.toString(), clock.now())) != -1L
    }
}
