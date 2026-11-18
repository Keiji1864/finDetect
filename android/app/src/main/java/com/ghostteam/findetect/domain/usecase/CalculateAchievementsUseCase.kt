package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AchievementActivityDay
import com.ghostteam.findetect.domain.model.AchievementCandidate
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.AchievementEvaluation
import com.ghostteam.findetect.domain.model.AchievementRules
import com.ghostteam.findetect.domain.model.AchievementTransactionFact
import javax.inject.Inject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** Pure replay of the eight supported conditions, including completed timely-entry days. */
class CalculateAchievementsUseCase @Inject constructor(private val streak: CalculateStreakUseCase) {
    operator fun invoke(
        activeDays: List<AchievementActivityDay>,
        transactionFacts: List<AchievementTransactionFact>,
        today: LocalDate,
        freshStartDate: LocalDate,
    ): AchievementEvaluation {
        val days = activeDays.filter { it.date <= today }.groupBy { it.date }
            .mapValues { (_, entries) -> entries.minBy { it.enteredAt } }
        val history = streak.achievementHistory(days.keys.toList(), today,
            AchievementRules.streakTargets.values, AchievementRules.COMEBACK_DAYS)
        val candidates = mutableListOf<AchievementCandidate>()
        val progress = mutableMapOf<AchievementCode, Int>()

        val first = transactionFacts.filter { it.enteredOn <= today }.minByOrNull { it.enteredAt }
        progress[AchievementCode.FIRST_CLUE] = if (first == null) 0 else 1
        first?.let { candidates += AchievementCandidate(AchievementCode.FIRST_CLUE, it.enteredOn, it.enteredAt) }

        for ((code, target) in AchievementRules.streakTargets) {
            progress[code] = history.progress.currentStreak.coerceAtMost(target)
            history.firstReachedDates[target]?.let { date ->
                candidates += AchievementCandidate(code, date, checkNotNull(days[date]).enteredAt)
            }
        }
        progress[AchievementCode.BACK_IN_ACTION] = history.comebackDays
        history.firstComebackDate?.let { date ->
            candidates += AchievementCandidate(AchievementCode.BACK_IN_ACTION, date, checkNotNull(days[date]).enteredAt)
        }

        val completed = transactionFacts.filter { it.enteredOn >= freshStartDate && it.enteredOn < today }
            .groupBy { it.enteredOn }.toSortedMap()
        var previous: LocalDate? = null
        var timelyRun = 0
        var firstCompletedRun: LocalDate? = null
        for ((date, facts) in completed) {
            val previousDate = previous
            timelyRun = if (facts.all { it.enteredOn == it.transactionOn }) {
                if (previousDate != null && previousDate.daysUntil(date) == 1) timelyRun + 1 else 1
            } else 0
            if (timelyRun >= AchievementRules.FRESH_CLUES_DAYS && firstCompletedRun == null) firstCompletedRun = date
            previous = date
        }
        progress[AchievementCode.FRESH_CLUES] = if (previous?.daysUntil(today) == 1) {
            timelyRun.coerceAtMost(AchievementRules.FRESH_CLUES_DAYS)
        } else 0
        firstCompletedRun?.let { candidates += AchievementCandidate(AchievementCode.FRESH_CLUES, it, occurredAt = null) }
        return AchievementEvaluation(candidates, progress)
    }
}
