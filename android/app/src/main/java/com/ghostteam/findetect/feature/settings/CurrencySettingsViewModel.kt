package com.ghostteam.findetect.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrencySettingsUiState(
    val selected: AppCurrency = AppCurrency.RUB,
    val ready: Boolean = false,
    val saving: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class CurrencySettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CurrencySettingsUiState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null

    init { retryRead() }

    fun retryRead() {
        readJob?.cancel()
        mutableState.update { it.copy(error = false) }
        readJob = viewModelScope.launch {
            settings.currency.catch { error ->
                if (error is CancellationException) throw error
                mutableState.update { it.copy(error = true, ready = false) }
            }.collect { currency -> mutableState.update { it.copy(selected = currency, ready = true) } }
        }
    }

    fun select(currency: AppCurrency) {
        if (!state.value.ready || state.value.saving) return
        mutableState.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                settings.setCurrency(currency)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(error = true) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
