package com.ghostteam.findetect.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.repository.ActivityRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveStreakUseCase
import com.ghostteam.findetect.feature.activity.DayActivityStateHolder
import com.ghostteam.findetect.feature.activity.DayActivityUiState
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

data class StreakUiState(
    val dayStatus: DayActivityUiState,
    val progress: StreakProgress? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val gameElementsEnabled: Boolean? = null,
    val settingsError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StreakViewModel @Inject constructor(
    days: LocalDayObserver,
    activity: ActivityRepository,
    settings: SettingsRepository,
    observeStreak: ObserveStreakUseCase,
) : ViewModel() {
    private val dayStatus = DayActivityStateHolder(viewModelScope, activity, days)
    private val day = MutableStateFlow(dayStatus.state.value.day)
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(StreakUiState(dayStatus.state.value))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            dayStatus.state.collect { status ->
                mutableState.update {
                    if (it.dayStatus.day == status.day) it.copy(dayStatus = status)
                    else it.copy(dayStatus = status, progress = null, loading = true, error = false)
                }
                day.value = status.day
            }
        }
        viewModelScope.launch {
            day.flatMapLatest { selectedDay ->
                reload.flatMapLatest {
                    observeStreak(selectedDay.date)
                        .map { selectedDay to it }
                        .onStart {
                            mutableState.update {
                                if (it.dayStatus.day == selectedDay) it.copy(loading = true, error = false) else it
                            }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            mutableState.update {
                                if (it.dayStatus.day == selectedDay) it.copy(progress = null, loading = false, error = true) else it
                            }
                        }
                }
            }.collect { (loadedDay, progress) ->
                mutableState.update {
                    if (it.dayStatus.day == loadedDay) it.copy(progress = progress, loading = false, error = false) else it
                }
            }
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

    fun confirmNoSpending() = dayStatus.confirmNoSpending()

    fun retry() {
        dayStatus.retry()
        reload.update { it + 1 }
    }
}
