package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.time.LocalDayObserver
import com.ghostteam.findetect.core.util.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class PeriodField { START, END }

data class FinancePeriodState(
    val selected: Period,
    val visibleMonth: LocalDate,
    val draftStart: LocalDate?,
    val draftEnd: LocalDate?,
    val open: Boolean = false,
    val selectingEnd: Boolean = false,
    val custom: Boolean = false,
    val editingField: PeriodField? = null,
) {
    val valid: Boolean get() = draftStart != null && draftEnd != null && draftStart <= draftEnd
}

/** Expense calendar selection and the rolling six-calendar-month income period. */
@HiltViewModel
class FinancePeriodViewModel @Inject constructor(days: LocalDayObserver) : ViewModel() {
    private val today = days.current().date
    val selection = FinancePeriodSelection(Period.currentMonth(today))
    val state = selection.state
    val incomePeriod = days.observe()
        .map { Period.lastSixCalendarMonths(it.date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Period.lastSixCalendarMonths(today))

    fun open() = selection.open()
    fun dismiss() = selection.dismiss()
    fun moveMonth(offset: Int) = selection.moveMonth(offset)
    fun selectMonth() = selection.selectMonth()
    fun selectDay(date: LocalDate) = selection.selectDay(date)
    fun showCustom(custom: Boolean) = selection.showCustom(custom)
    fun editField(field: PeriodField) = selection.editField(field)
    fun closeField() = selection.closeField()
    fun setFieldDate(date: LocalDate) = selection.setFieldDate(date)
    fun apply() = selection.apply()
}

/** Independent calendar draft for a screen; opening it never changes another screen's period. */
class FinancePeriodSelection(initial: Period) {
    private val mutableState = MutableStateFlow(FinancePeriodState(initial, initial.start, initial.start, initial.end))
    val state = mutableState.asStateFlow()

    fun open() = mutableState.update { it.copy(open = true, draftStart = it.selected.start, draftEnd = it.selected.end,
        visibleMonth = LocalDate(it.selected.start.year, it.selected.start.month, 1),
        selectingEnd = false, custom = false, editingField = null) }
    fun dismiss() = mutableState.update { it.copy(open = false, editingField = null) }
    fun moveMonth(offset: Int) = mutableState.update { it.copy(visibleMonth = it.visibleMonth.plus(offset, DateTimeUnit.MONTH)) }
    fun selectMonth() = mutableState.update {
        val month = Period.currentMonth(it.visibleMonth)
        it.copy(draftStart = month.start, draftEnd = month.end, selectingEnd = false)
    }
    fun selectDay(date: LocalDate) = mutableState.update {
        if (it.selectingEnd && it.draftStart != null && date >= it.draftStart) {
            it.copy(draftEnd = date, selectingEnd = false)
        } else it.copy(draftStart = date, draftEnd = null, selectingEnd = true)
    }
    fun showCustom(custom: Boolean) = mutableState.update { it.copy(custom = custom, editingField = null) }
    fun editField(field: PeriodField) = mutableState.update { it.copy(editingField = field) }
    fun closeField() = mutableState.update { it.copy(editingField = null) }
    fun setFieldDate(date: LocalDate) = mutableState.update {
        when (it.editingField) {
            PeriodField.START -> it.copy(draftStart = date, editingField = null, selectingEnd = false)
            PeriodField.END -> it.copy(draftEnd = date, editingField = null, selectingEnd = false)
            null -> it
        }
    }
    fun apply() {
        val current = state.value
        if (!current.valid) return
        mutableState.update { it.copy(selected = Period(requireNotNull(current.draftStart), requireNotNull(current.draftEnd)), open = false) }
    }
}
