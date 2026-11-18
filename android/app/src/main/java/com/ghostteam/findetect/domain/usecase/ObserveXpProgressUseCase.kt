package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.XpProgress
import com.ghostteam.findetect.domain.repository.XpRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Common progress source for the home, profile and streak screens. */
class ObserveXpProgressUseCase @Inject constructor(
    private val xp: XpRepository,
    private val calculate: CalculateXpProgressUseCase,
) {
    operator fun invoke(): Flow<XpProgress> = xp.observeTotalXp()
        .map { calculate(it) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
