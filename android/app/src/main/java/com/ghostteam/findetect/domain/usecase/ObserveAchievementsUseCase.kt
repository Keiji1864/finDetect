package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AchievementsProgress
import com.ghostteam.findetect.domain.repository.AchievementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Both the home preview and the full catalogue use the same user-scoped, read-only snapshot. */
class ObserveAchievementsUseCase @Inject constructor(private val repository: AchievementRepository) {
    operator fun invoke(today: LocalDate): Flow<AchievementsProgress> = repository.observeProgress(today)
}
