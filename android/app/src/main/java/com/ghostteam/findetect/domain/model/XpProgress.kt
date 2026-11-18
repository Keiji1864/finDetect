package com.ghostteam.findetect.domain.model

/** Stable title identifiers; presentation supplies the localized names. */
enum class XpTitle {
    TRAINEE,
    INVESTIGATOR,
    SENIOR_INVESTIGATOR,
    DETECTIVE,
    CHIEF_DETECTIVE,
}

/** Lifetime XP and the boundaries of its current level; the final level has no next boundary. */
data class XpProgress(
    val totalXp: Long,
    val level: Int,
    val title: XpTitle,
    val levelStartXp: Long,
    val nextLevelXp: Long?,
) {
    val xpInCurrentLevel: Long get() = totalXp - levelStartXp
    val xpForCurrentLevel: Long? get() = nextLevelXp?.minus(levelStartXp)
    val xpToNextLevel: Long? get() = nextLevelXp?.minus(totalXp)
    val isMaxLevel: Boolean get() = nextLevelXp == null
    val progressFraction: Float
        get() = nextLevelXp?.let { next ->
            (xpInCurrentLevel.toDouble() / (next - levelStartXp)).toFloat().coerceIn(0f, 1f)
        } ?: 1f
}
