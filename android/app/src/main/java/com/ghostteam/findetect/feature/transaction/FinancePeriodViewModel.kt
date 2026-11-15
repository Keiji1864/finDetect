package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.ViewModel
import com.ghostteam.findetect.core.util.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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

/** Shared by the two tabs for this app session; a fresh process starts at the current full month. */
@HiltViewModel
class FinancePeriodViewModel @Inject constructor(clock: Clock) : ViewModel() {
    private val initial = Period.currentMonth(clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
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
