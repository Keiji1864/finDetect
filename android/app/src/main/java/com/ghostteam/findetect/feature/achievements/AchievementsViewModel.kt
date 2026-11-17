package com.ghostteam.findetect.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveAchievementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AchievementsScreenState(
    val achievements: AchievementsUiState = AchievementsUiState(),
    val gameElementsEnabled: Boolean? = null,
    val settingsError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    days: LocalDayObserver,
    settings: SettingsRepository,
    observe: ObserveAchievementsUseCase,
) : ViewModel() {
    private val achievements = AchievementsStateHolder(viewModelScope, days.observe(), observe)
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(AchievementsScreenState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            achievements.state.collect { content -> mutableState.update { it.copy(achievements = content) } }
        }
        viewModelScope.launch {
            reload.flatMapLatest {
                settings.settings.map { it.gameElementsEnabled }
                    .onStart { mutableState.update { it.copy(settingsError = false) } }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(gameElementsEnabled = null, settingsError = true) }
                    }
            }.collect { enabled -> mutableState.update { it.copy(gameElementsEnabled = enabled, settingsError = false) } }
        }
    }

    fun retry() {
        achievements.retry()
        reload.update { it + 1 }
    }
}
