package com.ghostteam.findetect.feature.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.domain.model.AnalyticsSummary
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveAnalyticsUseCase
import com.ghostteam.findetect.feature.transaction.FinancePeriodSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal data class AnalyticsEnvironment(val today: LocalDate, val zone: TimeZone)

internal fun Clock.analyticsEnvironment(): AnalyticsEnvironment {
    val zone = TimeZone.currentSystemDefault()
    return AnalyticsEnvironment(now().toLocalDateTime(zone).date, zone)
}

data class AnalyticsRequest(val isIncome: Boolean, val period: Period, val today: LocalDate, val zone: TimeZone)

data class AnalyticsUiState(
    val request: AnalyticsRequest,
    val currency: AppCurrency? = null,
    val summary: AnalyticsSummary? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

/** Each new request first drops the previous filter's data, including after a currency change. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun analyticsStates(
    request: AnalyticsRequest,
    settings: SettingsRepository,
    observeAnalytics: ObserveAnalyticsUseCase,
): Flow<AnalyticsUiState> = flow {
    emitAll(settings.currency.flatMapLatest { currency ->
        flow {
            emitAll(observeAnalytics(currency.name, request.isIncome, request.period.start, request.period.end, request.zone)
                .map { AnalyticsUiState(request, currency, it, loading = false) })
        }.onStart { emit(AnalyticsUiState(request, currency)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(AnalyticsUiState(request, currency, loading = false, error = true))
            }
    })
}.onStart { emit(AnalyticsUiState(request)) }
    .catch { error ->
        if (error is CancellationException) throw error
        emit(AnalyticsUiState(request, loading = false, error = true))
    }

internal fun SavedStateHandle.readPeriod(startKey: String, endKey: String, fallback: Period): Period =
    runCatching {
        Period(LocalDate.parse(requireNotNull(get<String>(startKey))), LocalDate.parse(requireNotNull(get<String>(endKey))))
    }.getOrDefault(fallback)

internal fun SavedStateHandle.writePeriod(prefix: String, period: Period) {
    set("${prefix}Start", period.start.toString())
    set("${prefix}End", period.end.toString())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val clock: Clock,
    settings: SettingsRepository,
    observeAnalytics: ObserveAnalyticsUseCase,
) : ViewModel() {
    private val environment = MutableStateFlow(clock.analyticsEnvironment())
    private val initialEnvironment = environment.value
    private val month = Period.currentMonth(initialEnvironment.today)
    private val entryPeriod = savedState.readPeriod("start", "end", month)
    val monthDetail: Boolean = savedState["monthDetail"] ?: false
    val isIncome = savedState.getStateFlow("analyticsIncome", savedState.get<String>("type") == "income")
    val expensePeriod = FinancePeriodSelection(savedState.readPeriod("expenseStart", "expenseEnd", entryPeriod))
    val incomePeriod = FinancePeriodSelection(savedState.readPeriod("incomeStart", "incomeEnd",
        if (monthDetail) entryPeriod else Period.lastSixCalendarMonths(initialEnvironment.today)))
    private val reload = MutableStateFlow(0)
    private val initialRequest = request()

    init {
        savedState.writePeriod("expense", expensePeriod.state.value.selected)
        savedState.writePeriod("income", incomePeriod.state.value.selected)
    }

    val state = combine(isIncome, expensePeriod.state.map { it.selected }.distinctUntilChanged(),
        incomePeriod.state.map { it.selected }.distinctUntilChanged(), environment, reload) { income, expense, receipts, env, _ ->
        AnalyticsRequest(income, if (income) receipts else expense, env.today, env.zone)
    }.flatMapLatest { analyticsStates(it, settings, observeAnalytics) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState(initialRequest))

    fun request(): AnalyticsRequest {
        val env = environment.value
        val period = if (isIncome.value) incomePeriod.state.value.selected else expensePeriod.state.value.selected
        return AnalyticsRequest(isIncome.value, period, env.today, env.zone)
    }

    fun selectType(income: Boolean) { savedState["analyticsIncome"] = income }

    fun applyPeriod() {
        val selection = if (isIncome.value) incomePeriod else expensePeriod
        selection.apply()
        savedState.writePeriod(if (isIncome.value) "income" else "expense", selection.state.value.selected)
    }

    fun onResume() { environment.value = clock.analyticsEnvironment() }
    fun retry() { reload.update { it + 1 } }
}
