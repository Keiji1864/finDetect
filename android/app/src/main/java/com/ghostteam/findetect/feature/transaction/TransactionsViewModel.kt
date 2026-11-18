package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.TransactionsSummary
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.ObserveTransactionsSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

data class TransactionsRequest(val isIncome: Boolean, val period: Period, val zone: TimeZone)

private data class LoadedTransactions(
    val currency: AppCurrency,
    val request: TransactionsRequest,
    val summary: TransactionsSummary,
)

data class TransactionsUiState(
    val currency: AppCurrency? = null,
    val request: TransactionsRequest? = null,
    val summary: TransactionsSummary? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val observeSummary: ObserveTransactionsSummaryUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionsUiState())
    val state = mutableState.asStateFlow()
    private val requests = MutableStateFlow<TransactionsRequest?>(null)
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            requests.filterNotNull().flatMapLatest { request ->
                reload.flatMapLatest {
                    settings.currency.flatMapLatest { currency ->
                        flow {
                            emitAll(
                                observeSummary(currency.name, request.isIncome, request.period.start, request.period.end, request.zone)
                                    .map { LoadedTransactions(currency, request, it) },
                            )
                        }.onStart {
                            mutableState.update { previous ->
                                previous.copy(
                                    currency = currency,
                                    request = request,
                                    summary = previous.summary?.takeIf {
                                        previous.currency == currency && previous.request == request
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
                    if (it.currency == result.currency && it.request == result.request) {
                        it.copy(summary = result.summary, loading = false, error = false)
                    } else it
                }
            }
        }
    }

    fun bind(isIncome: Boolean, period: Period) {
        val request = TransactionsRequest(isIncome, period, TimeZone.currentSystemDefault())
        if (requests.value == request) return
        mutableState.update { it.copy(request = request, summary = null, loading = true, error = false) }
        requests.value = request
    }

    fun retry() { reload.value += 1 }
}
