package com.ghostteam.findetect.domain.model

/** Values match the XP journal API. Milestone and achievement awarding is connected separately. */
enum class XpReason {
    TRANSACTION,
    NO_SPENDING_MARK,
    STREAK_MILESTONE,
    ACHIEVEMENT,
}
