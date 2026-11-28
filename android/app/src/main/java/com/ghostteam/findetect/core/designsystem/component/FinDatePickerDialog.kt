package com.ghostteam.findetect.core.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinDatePickerDialog(date: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays().toLong() * 86_400_000L)
    val picked = state.selectedDateMillis?.let { LocalDate.fromEpochDays((it / 86_400_000L).toInt()) }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = picked != null, onClick = {
                picked?.let(onSelect)
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) { DatePicker(state = state, colors = finDatePickerColors()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun finDatePickerColors() = DatePickerDefaults.colors(
    containerColor = FinTheme.colors.white,
    selectedDayContainerColor = FinTheme.colors.yellow,
    selectedDayContentColor = FinTheme.colors.navy,
    selectedYearContainerColor = FinTheme.colors.yellow,
    selectedYearContentColor = FinTheme.colors.navy,
    todayContentColor = FinTheme.colors.navy,
    todayDateBorderColor = FinTheme.colors.yellow,
    currentYearContentColor = FinTheme.colors.navy,
    dayInSelectionRangeContainerColor = FinTheme.colors.cream,
    dayInSelectionRangeContentColor = FinTheme.colors.navy,
)
