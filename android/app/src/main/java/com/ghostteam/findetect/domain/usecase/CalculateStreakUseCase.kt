package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.StreakEvent
import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.model.StreakWeekDay
import com.ghostteam.findetect.domain.model.StreakAchievementHistory
import javax.inject.Inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Pure history replay. Dates are deduplicated, future dates ignored, and gaps processed without daily iteration. */
class CalculateStreakUseCase @Inject constructor() {
    operator fun invoke(activeDates: List<LocalDate>, today: LocalDate): StreakProgress =
        replay(activeDates, today) { _, _ -> }

    /** First attainment in the full history, using exactly the same grace/reset rules as the UI. */
    fun firstReachedDates(activeDates: List<LocalDate>, today: LocalDate, targets: Collection<Int>): Map<Int, LocalDate> {
        val pending = targets.filter { it > 0 }.toMutableSet()
        val reached = mutableMapOf<Int, LocalDate>()
        replay(activeDates, today) { date, count ->
            if (pending.remove(count)) reached[count] = date
        }
        return reached
    }

    /** Detects actual resets from the shared replay; a grace day alone never starts a comeback. */
    fun achievementHistory(
        activeDates: List<LocalDate>,
        today: LocalDate,
        targets: Collection<Int>,
        comebackTarget: Int,
    ): StreakAchievementHistory {
        require(comebackTarget > 0)
        val pending = targets.filter { it > 0 }.toMutableSet()
        val reached = mutableMapOf<Int, LocalDate>()
        var previous: LocalDate? = null
        var hadReset = false
        var consecutive = 0
        var firstComeback: LocalDate? = null
        val progress = replay(activeDates, today) { date, count ->
            if (pending.remove(count)) reached[count] = date
            val previousDate = previous
            if (previousDate != null && count == 1) hadReset = true
            consecutive = if (previousDate != null && previousDate.daysUntil(date) == 1) consecutive + 1 else 1
            if (hadReset && consecutive >= comebackTarget && firstComeback == null) firstComeback = date
            previous = date
        }
        val recent = previous?.let { it.daysUntil(today) <= 1 } == true
        return StreakAchievementHistory(
            progress = progress,
            firstReachedDates = reached,
            firstComebackDate = firstComeback,
            comebackDays = if (hadReset && recent && progress.currentStreak > 0) consecutive.coerceAtMost(comebackTarget) else 0,
        )
    }

    private fun replay(
        activeDates: List<LocalDate>,
        today: LocalDate,
        onActiveDay: (LocalDate, Int) -> Unit,
    ): StreakProgress {
        val dates = activeDates.asSequence().filter { it <= today }.distinct().sorted().toList()
        var current = 0
        var best = 0
        var graceAvailable = true
        var recoveryDays = 0
        var event = StreakEvent.NONE

        fun applyGap(previous: LocalDate, missedDays: Int) {
            if (missedDays <= 0 || current == 0) return
            if (missedDays == 1 && graceAvailable) {
                graceAvailable = false
                recoveryDays = 0
                // A missed day only takes effect on the following calendar date.
                if (previous.plus(2, DateTimeUnit.DAY) == today) event = StreakEvent.GRACE_USED
            } else {
                val resetDate = previous.plus(if (graceAvailable) 3 else 2, DateTimeUnit.DAY)
                current = 0
                graceAvailable = true
                recoveryDays = 0
                if (resetDate == today) event = StreakEvent.STREAK_RESET
            }
        }

        var previous: LocalDate? = null
        dates.forEach { date ->
            previous?.let { applyGap(it, it.daysUntil(date) - 1) }
            current += 1
            best = maxOf(best, current)
            onActiveDay(date, current)
            if (!graceAvailable) {
                recoveryDays += 1
                if (recoveryDays == GRACE_RECOVERY_DAYS) {
                    graceAvailable = true
                    recoveryDays = 0
                    if (date == today) event = StreakEvent.GRACE_RESTORED
                }
            }
            previous = date
        }
        // Today is unfinished, even when the user has not acted yet.
        previous?.let { applyGap(it, it.daysUntil(today) - 1) }

        val active = dates.toHashSet()
        val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        return StreakProgress(
            today = today,
            currentStreak = current,
            bestStreak = best,
            graceDayAvailable = graceAvailable,
            graceRecoveryDays = recoveryDays,
            lastActiveDate = dates.lastOrNull(),
            event = event,
            week = List(7) { offset ->
                val date = monday.plus(offset, DateTimeUnit.DAY)
                StreakWeekDay(date, date in active)
            },
        )
    }

    private companion object {
        const val GRACE_RECOVERY_DAYS = 7
    }
}
