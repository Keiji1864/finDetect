package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.XpDao
import com.ghostteam.findetect.data.local.entity.XpEventEntity
import com.ghostteam.findetect.data.local.entity.XpStateEntity
import com.ghostteam.findetect.domain.model.XpReason
import com.ghostteam.findetect.domain.model.XpRules
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.AchievementRules
import com.ghostteam.findetect.domain.repository.XpRepository
import com.ghostteam.findetect.domain.usecase.CalculateStreakUseCase
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LocalXpRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: XpDao,
    private val clock: Clock,
    private val calculateStreak: CalculateStreakUseCase,
) : XpRepository {
    override fun observeTotalXp() = dao.observeTotalXp().distinctUntilChanged()

    /** Freezes the legacy baseline once and reconciles unpaid milestones before publishing the scope. */
    internal suspend fun initialize() = database.withTransaction {
        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val state = dao.getState()
        if (state == null) {
            val dates = database.activityDayDao().getActiveDates().map { LocalDate.parse(it) }
            dao.insertState(XpStateEntity(startedAt = now, legacyBestStreak = calculateStreak(dates, today).bestStreak))
        } else {
            // Reconcile milestones earned after XP started, including activity saved before bonuses shipped.
            recordStreakMilestones(state, today)
        }
    }

    /** Nested Room transactions join the originating write, so an XP failure rolls it back too. */
    internal suspend fun recordTransaction(id: String, date: LocalDate, at: Instant) = recordLimitedEvent(
        reason = XpReason.TRANSACTION,
        sourceId = id,
        date = date,
        at = at,
        amount = XpRules.TRANSACTION_XP,
        dailyLimit = XpRules.TRANSACTIONS_PER_DAY,
    )

    internal suspend fun recordNoSpending(date: LocalDate, at: Instant) = recordLimitedEvent(
        reason = XpReason.NO_SPENDING_MARK,
        sourceId = date.toString(),
        date = date,
        at = at,
        amount = XpRules.NO_SPENDING_XP,
        dailyLimit = XpRules.NO_SPENDING_MARKS_PER_DAY,
    )

    /** Joined by the achievement transaction; retry returns the existing stable event identity. */
    internal suspend fun recordAchievement(code: AchievementCode, date: LocalDate, at: Instant): String = database.withTransaction {
        checkNotNull(dao.getState()) { "XP must be initialized before awarding achievements" }
        val reference = code.name
        val existing = dao.getBySource(XpReason.ACHIEVEMENT, reference)
        if (existing != null) return@withTransaction existing.id
        val event = XpEventEntity(
            id = UUID.nameUUIDFromBytes("ACHIEVEMENT:$reference".toByteArray()).toString(),
            reason = XpReason.ACHIEVEMENT,
            sourceId = reference,
            reasonRef = reference,
            amount = AchievementRules.rewards.getValue(code),
            counted = true,
            localDate = date.toString(),
            occurredAt = at,
        )
        dao.insertEvent(event)
        event.id
    }

    private suspend fun recordLimitedEvent(
        reason: XpReason,
        sourceId: String,
        date: LocalDate,
        at: Instant,
        amount: Int,
        dailyLimit: Int,
    ) = database.withTransaction {
        val state = checkNotNull(dao.getState()) { "XP must be initialized before accepting user actions" }
        if (dao.getBySource(reason, sourceId) == null) {
            val localDate = date.toString()
            val counted = dao.countDailyAwards(localDate, reason) < dailyLimit
            // Even a capped action retains its identity; later deletion/retry cannot turn it into an award.
            dao.insertEvent(XpEventEntity(
                id = UUID.nameUUIDFromBytes("$reason:$sourceId".toByteArray()).toString(),
                reason = reason,
                sourceId = sourceId,
                reasonRef = null,
                amount = if (counted) amount else 0,
                counted = counted,
                localDate = localDate,
                occurredAt = at,
            ))
        }
        recordStreakMilestones(state, date)
    }

    /** Runs within initialization or the originating activity transaction, never from a screen. */
    private suspend fun recordStreakMilestones(state: XpStateEntity, today: LocalDate) {
        val pending = XpRules.streakMilestones.filter { milestone ->
            milestone.activeDays > state.legacyBestStreak &&
                dao.getBySource(XpReason.STREAK_MILESTONE, milestone.activeDays.toString()) == null
        }
        if (pending.isEmpty()) return
        val dates = database.activityDayDao().getActiveDates().map { LocalDate.parse(it) }
        val firstReached = calculateStreak.firstReachedDates(dates, today, pending.map { it.activeDays })
        for (milestone in pending) {
            val reachedDate = firstReached[milestone.activeDays] ?: continue
            val activity = checkNotNull(database.activityDayDao().get(reachedDate.toString()))
            val reference = milestone.activeDays.toString()
            dao.insertEvent(XpEventEntity(
                id = UUID.nameUUIDFromBytes("STREAK_MILESTONE:$reference".toByteArray()).toString(),
                reason = XpReason.STREAK_MILESTONE,
                sourceId = reference,
                reasonRef = reference,
                amount = milestone.amount,
                counted = true,
                localDate = reachedDate.toString(),
                occurredAt = activity.enteredAt,
            ))
        }
    }
}
