package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.repository.TransactionRepository
import java.math.BigDecimal
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus

data class TransactionsRequest(val isIncome: Boolean, val period: Period, val zone: TimeZone)

private data class LoadedTransactions(
    val currency: AppCurrency,
    val request: TransactionsRequest,
    val summary: TransactionsSummary,
    val comparison: PeriodComparison?,
)

data class PeriodComparison(val previous: Period, val current: Period, val difference: BigDecimal)

data class TransactionsUiState(
    val currency: AppCurrency? = null,
    val request: TransactionsRequest? = null,
    val summary: TransactionsSummary? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val comparison: PeriodComparison? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val observeSummary: ObserveTransactionsSummaryUseCase,
    private val transactions: TransactionRepository,
    private val days: LocalDayObserver,
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
                                days.observe().flatMapLatest { day -> load(currency, request, day.date, day.zone) },
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
                                    comparison = null,
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
                        it.copy(summary = result.summary, comparison = result.comparison, loading = false, error = false)
                    } else it
                }
            }
        }
    }

    private fun load(currency: AppCurrency, request: TransactionsRequest, today: LocalDate, zone: TimeZone): Flow<LoadedTransactions> {
        val start = request.period.start
        val summary = observeSummary(currency.name, request.isIncome, start, request.period.end, zone)
        val end = minOf(request.period.end, today.minus(1, DateTimeUnit.DAY))
        if (end < start) return summary.map { LoadedTransactions(currency, request, it, null) }
        val duration = end.toEpochDays() - start.toEpochDays() + 1
        val previous = Period(start.minus(duration, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
        val filter = TransactionFilter(currency = currency.name, isIncome = request.isIncome,
            fromInclusive = previous.start.atStartOfDayIn(zone), toExclusive = start.atStartOfDayIn(zone))
        return combine(summary, transactions.observeTotalAmount(filter)) { current, prior ->
            val amount = current.days.filter { it.date <= end }.sumOf { day -> day.transactions.sumOf { it.transaction.amount } }
            val comparison = if (amount.signum() > 0 && prior.signum() > 0)
                PeriodComparison(previous, Period(start, end), amount - prior) else null
            LoadedTransactions(currency, request, current, comparison)
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
