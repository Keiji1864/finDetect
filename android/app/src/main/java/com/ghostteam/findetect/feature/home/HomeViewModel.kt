package com.ghostteam.findetect.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.HomeFinanceSummary
import com.ghostteam.findetect.domain.model.LocalDay
import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.repository.ActivityRepository
import com.ghostteam.findetect.domain.usecase.ObserveHomeFinanceUseCase
import com.ghostteam.findetect.domain.usecase.ObserveStreakUseCase
import com.ghostteam.findetect.domain.usecase.ObserveXpProgressUseCase
import com.ghostteam.findetect.domain.usecase.ObserveAchievementsUseCase
import com.ghostteam.findetect.feature.achievements.AchievementsStateHolder
import com.ghostteam.findetect.feature.achievements.AchievementsUiState
import com.ghostteam.findetect.feature.activity.DayActivityStateHolder
import com.ghostteam.findetect.feature.activity.DayActivityUiState
import com.ghostteam.findetect.feature.gamification.XpProgressStateHolder
import com.ghostteam.findetect.feature.gamification.XpProgressUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias HomeFinanceDay = LocalDay

data class HomeUiState(
    val day: HomeFinanceDay,
    val currency: AppCurrency? = null,
    val finance: HomeFinanceSummary? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val gameElementsEnabled: Boolean = false,
    val dayStatus: DayActivityUiState = DayActivityUiState(day),
    val streak: StreakProgress? = null,
    val streakLoading: Boolean = true,
    val streakError: Boolean = false,
    val xp: XpProgressUiState = XpProgressUiState(),
    val achievements: AchievementsUiState = AchievementsUiState(),
)

private data class LoadedHomeFinance(
    val day: HomeFinanceDay,
    val currency: AppCurrency,
    val summary: HomeFinanceSummary,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    days: LocalDayObserver,
    private val settings: SettingsRepository,
    private val observeFinance: ObserveHomeFinanceUseCase,
    activity: ActivityRepository,
    private val observeStreak: ObserveStreakUseCase,
    observeXp: ObserveXpProgressUseCase,
    observeAchievements: ObserveAchievementsUseCase,
) : ViewModel() {
    private val dayStatus = DayActivityStateHolder(viewModelScope, activity, days)
    private val xp = XpProgressStateHolder(viewModelScope, observeXp)
    private val achievements = AchievementsStateHolder(viewModelScope, days.observe(), observeAchievements)
    private val day = MutableStateFlow(dayStatus.state.value.day)
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(HomeUiState(day = day.value))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch { xp.state.collect { progress -> mutableState.update { it.copy(xp = progress) } } }
        viewModelScope.launch { achievements.state.collect { content -> mutableState.update { it.copy(achievements = content) } } }
        viewModelScope.launch {
            dayStatus.state.collect { status ->
                mutableState.update {
                    if (it.day == status.day) it.copy(dayStatus = status)
                    else it.copy(day = status.day, dayStatus = status, finance = null, loading = true, error = false,
                        streak = null, streakLoading = true, streakError = false)
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
                                if (it.day == selectedDay) it.copy(streakLoading = true, streakError = false) else it
                            }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            mutableState.update {
                                if (it.day == selectedDay) it.copy(streak = null, streakLoading = false, streakError = true) else it
                            }
                        }
                }
            }.collect { (loadedDay, progress) ->
                mutableState.update {
                    if (it.day == loadedDay) it.copy(streak = progress, streakLoading = false, streakError = false) else it
                }
            }
        }
        viewModelScope.launch {
            reload.flatMapLatest {
                settings.settings.map { it.gameElementsEnabled }.catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(gameElementsEnabled = false) }
                }
            }.collect { enabled -> mutableState.update { it.copy(gameElementsEnabled = enabled) } }
        }
        viewModelScope.launch {
            day.flatMapLatest { selectedDay ->
                reload.flatMapLatest {
                    settings.currency.flatMapLatest { currency ->
                        flow {
                            emitAll(
                                observeFinance(currency.name, selectedDay.date, selectedDay.zone)
                                    .map { LoadedHomeFinance(selectedDay, currency, it) },
                            )
                        }.onStart {
                            mutableState.update { previous ->
                                if (previous.day != selectedDay) previous else previous.copy(
                                    currency = currency,
                                    finance = previous.finance?.takeIf {
                                        previous.day == selectedDay && previous.currency == currency
                                    },
                                    loading = true,
                                    error = false,
                                )
                            }
                        }.catch { error ->
                            if (error is CancellationException) throw error
                            mutableState.update { if (it.day == selectedDay) it.copy(loading = false, error = true) else it }
                        }
                    }.catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { if (it.day == selectedDay) it.copy(loading = false, error = true) else it }
                    }
                }
            }.collect { result ->
                mutableState.update {
                    if (it.day == result.day && it.currency == result.currency) {
                        it.copy(finance = result.summary, loading = false, error = false)
                    } else it
                }
            }
        }
    }

    fun retry() {
        dayStatus.retry()
        xp.retry()
        achievements.retry()
        reload.value += 1
    }

    fun confirmNoSpending() = dayStatus.confirmNoSpending()
}
