package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Stable catalogue keys shared with the achievement and XP APIs. */
enum class AchievementCode {
    FIRST_CLUE,
    STREAK_3,
    STREAK_7,
    STREAK_14,
    STREAK_30,
    STREAK_100,
    BACK_IN_ACTION,
    FRESH_CLUES,
    WITHIN_LIMIT,
    REACHED_INCOME,
    FORECASTER,
}

data class EarnedAchievement(
    val code: AchievementCode,
    val earnedAt: Instant,
    val earnedOn: LocalDate,
    val isLegacy: Boolean,
    val rewardPending: Boolean,
)

data class AchievementActivityDay(val date: LocalDate, val enteredAt: Instant)

/** Immutable creation facts; dates were captured in zoneId rather than inferred from today's zone. */
data class AchievementTransactionFact(
    val transactionId: String,
    val enteredOn: LocalDate,
    val transactionOn: LocalDate,
    val zoneId: String,
    val enteredAt: Instant,
)

/** A null occurredAt means a completed-day condition is recognized now, not before its day ended. */
data class AchievementCandidate(
    val code: AchievementCode,
    val qualifyingDate: LocalDate,
    val occurredAt: Instant?,
)

data class AchievementEvaluation(
    val candidates: List<AchievementCandidate>,
    val progress: Map<AchievementCode, Int>,
)

data class AchievementProgress(
    val code: AchievementCode,
    val earned: EarnedAchievement?,
    val current: Int?,
) {
    val target: Int? get() = AchievementRules.target(code)
    val rewardXp: Int get() = AchievementRules.rewards.getValue(code)
    val supported: Boolean get() = code in AchievementRules.supportedCodes
}

data class AchievementsProgress(val achievements: List<AchievementProgress>) {
    val earnedCount: Int get() = achievements.count { it.earned != null }
    val totalCount: Int get() = achievements.size
    val preview: AchievementProgress?
        get() = achievements.filter { it.earned != null }
            .maxWithOrNull(compareBy<AchievementProgress> { checkNotNull(it.earned).earnedAt }.thenBy { it.code.ordinal })
            ?: achievements.firstOrNull { it.supported }
}

data class StreakAchievementHistory(
    val progress: StreakProgress,
    val firstReachedDates: Map<Int, LocalDate>,
    val firstComebackDate: LocalDate?,
    val comebackDays: Int,
)
