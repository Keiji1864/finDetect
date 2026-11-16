package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.StreakEvent
import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.model.StreakWeekDay
import javax.inject.Inject
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Pure history replay. Dates are deduplicated, future dates ignored, and gaps processed without daily iteration. */
class CalculateStreakUseCase @Inject constructor() {
    operator fun invoke(activeDates: List<LocalDate>, today: LocalDate): StreakProgress {
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
