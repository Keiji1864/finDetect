package com.ghostteam.findetect.feature.activity

import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.domain.model.ConfirmNoSpendingResult
import com.ghostteam.findetect.domain.model.DayActivity
import com.ghostteam.findetect.domain.model.LocalDay
import com.ghostteam.findetect.domain.repository.ActivityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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

data class DayActivityUiState(
    val day: LocalDay,
    val activity: DayActivity? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false,
    val dayChanged: Boolean = false,
) {
    val canConfirm: Boolean get() = activity != null && !activity.hasExpense && !activity.noSpendingConfirmed &&
        !loading && !saving && !error
}

/** Shared presentation logic for the home and streak screens, owned by the caller's ViewModel scope. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayActivityStateHolder(
    private val scope: CoroutineScope,
    private val activity: ActivityRepository,
    private val days: LocalDayObserver,
) {
    private val day = MutableStateFlow(days.current())
    private val reload = MutableStateFlow(0)
    private val mutableState = MutableStateFlow(DayActivityUiState(day.value))
    val state = mutableState.asStateFlow()

    init {
        scope.launch { days.observe().collect { updateDay(it) } }
        scope.launch {
            day.flatMapLatest { selectedDay ->
                reload.flatMapLatest {
                    activity.observeDay(selectedDay.date, selectedDay.zone)
                        .map { selectedDay to it }
                        .onStart {
                            mutableState.update { if (it.day == selectedDay) it.copy(loading = true, error = false) else it }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            mutableState.update {
                                if (it.day == selectedDay) it.copy(activity = null, loading = false, error = true) else it
                            }
                        }
                }
            }.collect { (loadedDay, value) ->
                mutableState.update {
                    if (it.day == loadedDay) it.copy(activity = value, loading = false, error = false) else it
                }
            }
        }
    }

    private fun updateDay(current: LocalDay) {
        if (day.value == current) return
        mutableState.update {
            it.copy(day = current, activity = null, loading = true, error = false, dayChanged = false)
        }
        day.value = current
    }

    fun refreshDay() = updateDay(days.current())

    fun retry() {
        refreshDay()
        reload.update { it + 1 }
    }

    fun confirmNoSpending() {
        val current = state.value
        if (!current.canConfirm) return
        mutableState.update { it.copy(saving = true, error = false, dayChanged = false) }
        scope.launch {
            try {
                when (activity.confirmNoSpending(current.day.date)) {
                    ConfirmNoSpendingResult.CONFIRMED, ConfirmNoSpendingResult.ALREADY_CONFIRMED -> mutableState.update {
                        if (it.day == current.day) it.copy(activity = (it.activity ?: DayActivity()).copy(noSpendingConfirmed = true)) else it
                    }
                    ConfirmNoSpendingResult.HAS_EXPENSE -> mutableState.update {
                        if (it.day == current.day) it.copy(activity = (it.activity ?: DayActivity()).copy(hasExpense = true)) else it
                    }
                    ConfirmNoSpendingResult.DAY_CHANGED -> {
                        refreshDay()
                        mutableState.update { it.copy(dayChanged = true) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { if (it.day == current.day) it.copy(error = true) else it }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
