package com.ghostteam.findetect.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.model.IncomeSchedule
import com.ghostteam.findetect.domain.model.AppSettings
import com.ghostteam.findetect.domain.repository.PinRepository
import com.ghostteam.findetect.domain.repository.PinStatus
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class SettingsUiState(
    val settings: AppSettings? = null,
    val pin: PinStatus = PinStatus.LOADING,
    val notificationsAllowed: Boolean = false,
    val saving: Boolean = false,
    val readError: Boolean = false,
    val writeError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val pin: PinRepository,
    private val notifications: ReminderNotifications,
    days: LocalDayObserver,
) : ViewModel() {
    val today = days.observe().map { it.date }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), days.current().date)
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(SettingsUiState(notificationsAllowed = notifications.allowed()))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            reload.flatMapLatest {
                repository.settings.onStart { mutableState.update { it.copy(readError = false) } }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(settings = null, readError = true) }
                    }
            }.collect { value -> mutableState.update { it.copy(settings = value, readError = false) } }
        }
        viewModelScope.launch { pin.status.collect { value -> mutableState.update { it.copy(pin = value) } } }
    }

    fun refreshSystemPermission() { mutableState.update { it.copy(notificationsAllowed = notifications.allowed()) } }
    fun retryRead() { reload.value += 1 }
    fun setGameEnabled(enabled: Boolean) = save { repository.setGameElementsEnabled(enabled) }
    fun setReminderEnabled(enabled: Boolean) = save { repository.setDailyReminderEnabled(enabled) }
    fun setReminderHour(hour: Int) = save { repository.setDailyReminderHour(hour) }
    fun saveIncome(schedule: IncomeSchedule?, date: LocalDate?, onSaved: () -> Unit) = save {
        repository.setIncomeSchedule(schedule, date)
        onSaved()
    }

    private fun save(block: suspend () -> Unit) {
        if (state.value.settings == null || state.value.saving) return
        mutableState.update { it.copy(saving = true, writeError = false) }
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(writeError = true) } }
            finally { mutableState.update { it.copy(saving = false) } }
        }
    }
}
