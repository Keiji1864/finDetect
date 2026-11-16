package com.ghostteam.findetect.feature.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.repository.CategoryRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveAnalyticsUseCase
import com.ghostteam.findetect.feature.transaction.FinancePeriodSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

data class CategoryDetailUiState(val data: AnalyticsUiState, val category: Category? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val clock: Clock,
    settings: SettingsRepository,
    observeAnalytics: ObserveAnalyticsUseCase,
    categories: CategoryRepository,
) : ViewModel() {
    val categoryId: String = requireNotNull(savedState.get<String>("categoryId"))
    private val isIncome = savedState.get<String>("type") == "income"
    private val environment = MutableStateFlow(clock.analyticsEnvironment())
    private val entryPeriod = savedState.readPeriod("start", "end", Period.currentMonth(environment.value.today))
    val period = FinancePeriodSelection(savedState.readPeriod("categoryStart", "categoryEnd", entryPeriod))
    private val reload = MutableStateFlow(0)
    private val initialRequest = request()

    val state = combine(period.state.map { it.selected }.distinctUntilChanged(), environment, reload) { selected, env, _ ->
        AnalyticsRequest(isIncome, selected, env.today, env.zone)
    }.flatMapLatest { request ->
        analyticsStates(request, settings, observeAnalytics).map { data ->
            // Keep the category's name even when this period has no operations in it.
            val category = if (data.summary != null) {
                (data.summary.transactions.categories.firstOrNull { it.category.id == categoryId }?.category
                    ?: categories.getById(categoryId))?.takeIf { it.isIncome == isIncome }
            } else null
            CategoryDetailUiState(data, category)
        }.catch { error ->
            if (error is CancellationException) throw error
            emit(CategoryDetailUiState(AnalyticsUiState(request, loading = false, error = true)))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState(AnalyticsUiState(initialRequest)))

    fun request(): AnalyticsRequest {
        val env = environment.value
        return AnalyticsRequest(isIncome, period.state.value.selected, env.today, env.zone)
    }

    fun applyPeriod() {
        period.apply()
        savedState.writePeriod("category", period.state.value.selected)
    }

    fun onResume() { environment.value = clock.analyticsEnvironment() }
    fun retry() { reload.update { it + 1 } }
}
