package com.ghostteam.findetect.feature.achievements

import com.ghostteam.findetect.domain.model.AchievementsProgress
import com.ghostteam.findetect.domain.model.LocalDay
import com.ghostteam.findetect.domain.usecase.ObserveAchievementsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class AchievementsUiState(
    val progress: AchievementsProgress? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

/** Read-only calendar-aware subscription shared by the full screen and the home preview. */
@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsStateHolder(scope: CoroutineScope, days: Flow<LocalDay>, observe: ObserveAchievementsUseCase) {
    private val reload = MutableStateFlow(0)
    val state = combine(days, reload) { day, _ -> day }.flatMapLatest { day ->
        flow { emitAll(observe(day.date).map { AchievementsUiState(it, loading = false) }) }
            .onStart { emit(AchievementsUiState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(AchievementsUiState(loading = false, error = true))
            }
    }.stateIn(scope, SharingStarted.Eagerly, AchievementsUiState())

    fun retry() { reload.update { it + 1 } }
}
