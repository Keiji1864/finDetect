package com.ghostteam.findetect.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.HomeFinanceSummary
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveHomeFinanceUseCase
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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HomeFinanceDay(val date: LocalDate, val zone: TimeZone)

data class HomeUiState(
    val day: HomeFinanceDay,
    val currency: AppCurrency? = null,
    val finance: HomeFinanceSummary? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

private data class LoadedHomeFinance(
    val day: HomeFinanceDay,
    val currency: AppCurrency,
    val summary: HomeFinanceSummary,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
    private val settings: SettingsRepository,
    private val observeFinance: ObserveHomeFinanceUseCase,
) : ViewModel() {
    private val day = MutableStateFlow(currentDay())
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(HomeUiState(day = day.value))
    val state = mutableState.asStateFlow()

    init {
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
                                previous.copy(
                                    day = selectedDay,
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
                            mutableState.update { it.copy(loading = false, error = true) }
                        }
                    }.catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(loading = false, error = true) }
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

    /** Rebind after midnight, a time zone change, or returning to the foreground. */
    fun refreshDay() {
        val current = currentDay()
        if (day.value == current) return
        mutableState.update { it.copy(day = current, finance = null, loading = true, error = false) }
        day.value = current
    }

    fun retry() {
        refreshDay()
        reload.value += 1
    }

    private fun currentDay(): HomeFinanceDay {
        val zone = TimeZone.currentSystemDefault()
        return HomeFinanceDay(clock.now().toLocalDateTime(zone).date, zone)
    }
}
