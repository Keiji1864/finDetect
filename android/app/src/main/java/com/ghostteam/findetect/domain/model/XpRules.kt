package com.ghostteam.findetect.domain.model

data class XpLevelDefinition(val level: Int, val title: XpTitle, val startXp: Long)

data class XpStreakMilestone(val activeDays: Int, val amount: Int)

/** Agreed on 17.11.2026 in .ai-docs/04_Геймификация.md; keep server configuration aligned. */
object XpRules {
    const val TRANSACTION_XP = 10
    const val TRANSACTIONS_PER_DAY = 5
    const val NO_SPENDING_XP = 5
    const val NO_SPENDING_MARKS_PER_DAY = 1

    val levels: List<XpLevelDefinition> = listOf(
        XpLevelDefinition(1, XpTitle.TRAINEE, 0L),
        XpLevelDefinition(2, XpTitle.INVESTIGATOR, 250L),
        XpLevelDefinition(3, XpTitle.SENIOR_INVESTIGATOR, 1_000L),
        XpLevelDefinition(4, XpTitle.DETECTIVE, 2_500L),
        XpLevelDefinition(5, XpTitle.CHIEF_DETECTIVE, 5_000L),
    )

    val streakMilestones: List<XpStreakMilestone> = listOf(
        XpStreakMilestone(3, 20),
        XpStreakMilestone(7, 50),
        XpStreakMilestone(14, 100),
        XpStreakMilestone(30, 200),
        XpStreakMilestone(100, 500),
    )
}
