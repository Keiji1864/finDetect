package com.ghostteam.findetect.feature.gamification

import com.ghostteam.findetect.domain.model.XpProgress
import com.ghostteam.findetect.domain.usecase.ObserveXpProgressUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class XpProgressUiState(
    val progress: XpProgress? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

/** Shared read/retry behavior; each screen owns its subscription to the same user-scoped journal. */
@OptIn(ExperimentalCoroutinesApi::class)
class XpProgressStateHolder(scope: CoroutineScope, observe: ObserveXpProgressUseCase) {
    private val reload = MutableStateFlow(0)
    val state = reload.flatMapLatest {
        flow { emitAll(observe().map { XpProgressUiState(progress = it, loading = false) }) }
            .onStart { emit(XpProgressUiState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(XpProgressUiState(loading = false, error = true))
            }
    }.stateIn(scope, SharingStarted.Eagerly, XpProgressUiState())

    fun retry() { reload.update { it + 1 } }
}
