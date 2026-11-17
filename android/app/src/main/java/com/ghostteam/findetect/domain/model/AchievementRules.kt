package com.ghostteam.findetect.domain.model

/** Agreed catalogue in .ai-docs/04_Геймификация.md, section 5. */
object AchievementRules {
    const val COMEBACK_DAYS = 3
    const val FRESH_CLUES_DAYS = 7

    val rewards: Map<AchievementCode, Int> = mapOf(
        AchievementCode.FIRST_CLUE to 20,
        AchievementCode.STREAK_3 to 20,
        AchievementCode.STREAK_7 to 30,
        AchievementCode.STREAK_14 to 50,
        AchievementCode.STREAK_30 to 70,
        AchievementCode.STREAK_100 to 100,
        AchievementCode.BACK_IN_ACTION to 30,
        AchievementCode.FRESH_CLUES to 50,
        AchievementCode.WITHIN_LIMIT to 70,
        AchievementCode.REACHED_INCOME to 100,
        AchievementCode.FORECASTER to 20,
    )

    val streakTargets: Map<AchievementCode, Int> = mapOf(
        AchievementCode.STREAK_3 to 3,
        AchievementCode.STREAK_7 to 7,
        AchievementCode.STREAK_14 to 14,
        AchievementCode.STREAK_30 to 30,
        AchievementCode.STREAK_100 to 100,
    )

    val supportedCodes: Set<AchievementCode> = streakTargets.keys + setOf(
        AchievementCode.FIRST_CLUE, AchievementCode.BACK_IN_ACTION, AchievementCode.FRESH_CLUES,
    )

    fun target(code: AchievementCode): Int? = streakTargets[code] ?: when (code) {
        AchievementCode.FIRST_CLUE -> 1
        AchievementCode.BACK_IN_ACTION -> COMEBACK_DAYS
        AchievementCode.FRESH_CLUES -> FRESH_CLUES_DAYS
        else -> null
    }
}
