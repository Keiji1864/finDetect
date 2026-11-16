package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.repository.ActivityRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Both streak screens observe the same user-scoped history and use the same calculation. */
class ObserveStreakUseCase @Inject constructor(
    private val activity: ActivityRepository,
    private val calculate: CalculateStreakUseCase,
) {
    operator fun invoke(today: LocalDate): Flow<StreakProgress> = activity.observeActiveDates()
        .map { calculate(it, today) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
