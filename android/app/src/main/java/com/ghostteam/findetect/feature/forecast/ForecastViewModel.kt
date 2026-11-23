package com.ghostteam.findetect.feature.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.data.repository.LocalForecastRepository
import com.ghostteam.findetect.domain.model.ForecastPlan
import com.ghostteam.findetect.domain.model.SpendingForecast
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class ForecastState(val result: SpendingForecast? = null, val failed: Boolean = false, val tips: Boolean = true)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForecastViewModel @Inject constructor(
    settings: SettingsRepository, days: LocalDayObserver, private val repository: LocalForecastRepository,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state = refresh.flatMapLatest {
        val display = settings.settings.map { it.currency to it.forecastTipsEnabled }.distinctUntilChanged()
        combine(display, days.observe()) { options, day -> options to day }
            .flatMapLatest { (options, day) ->
                val (currency, tips) = options
                repository.observe(currency, day)
                    .map { ForecastState(it, tips = tips) }
                    .onStart { emit(ForecastState()) }
            }.catch { emit(ForecastState(failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000, 0), ForecastState())

    // A confirmed edit can finish when navigating back to the home card.
    suspend fun save(forecast: SpendingForecast, plan: ForecastPlan) = viewModelScope.async {
        repository.save(forecast, plan)
    }.await()

    fun retry() { refresh.value += 1 }
}
