package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.XpProgress
import com.ghostteam.findetect.domain.model.XpRules
import javax.inject.Inject

/** Resolves all crossed thresholds at once, without persisting a separate mutable level. */
class CalculateXpProgressUseCase @Inject constructor() {
    operator fun invoke(totalXp: Long): XpProgress {
        require(totalXp >= 0) { "Total XP cannot be negative" }
        val index = XpRules.levels.indexOfLast { it.startXp <= totalXp }
        val current = XpRules.levels[index]
        return XpProgress(
            totalXp = totalXp,
            level = current.level,
            title = current.title,
            levelStartXp = current.startXp,
            nextLevelXp = XpRules.levels.getOrNull(index + 1)?.startXp,
        )
    }
}
