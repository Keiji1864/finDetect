package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.EarnedAchievement
import com.ghostteam.findetect.domain.model.AchievementsProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Recognition is driven by local writes and calendar changes, never by a screen subscription. */
interface AchievementRepository {
    fun observeEarned(): Flow<List<EarnedAchievement>>
    fun observeProgress(today: LocalDate): Flow<AchievementsProgress>
}
