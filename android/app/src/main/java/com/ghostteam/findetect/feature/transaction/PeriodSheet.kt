package com.ghostteam.findetect.feature.transaction

import android.app.DatePickerDialog
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.*
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.Period
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.core.util.formatMonth
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
fun FinancePeriodSheet(viewModel: FinancePeriodViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PeriodSheet(state, viewModel::dismiss, viewModel::moveMonth, viewModel::selectMonth, viewModel::selectDay,
        viewModel::showCustom, viewModel::editField, viewModel::closeField, viewModel::setFieldDate, viewModel::apply)
}

/** Calendar draft is applied explicitly; dismissal never changes the shared period. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSheet(
    state: FinancePeriodState,
    onDismiss: () -> Unit,
    onMoveMonth: (Int) -> Unit,
    onSelectMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onShowCustom: (Boolean) -> Unit,
    onEditField: (PeriodField) -> Unit,
    onCloseField: () -> Unit,
    onFieldDate: (LocalDate) -> Unit,
    onApply: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    FinBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (state.custom) R.string.transactions_custom_period else R.string.ui_choose_period),
                style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
            if (!state.custom) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMoveMonth(-1) }) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.transactions_previous_month), tint = FinTheme.colors.navy)
                    }
                    val chooseMonth = stringResource(R.string.transactions_select_month, state.visibleMonth.formatMonth(locale))
                    Text(state.visibleMonth.formatMonth(locale), modifier = Modifier.weight(1f)
                        .clip(FinTheme.shapes.r12).clickable(role = Role.Button, onClick = onSelectMonth)
                        .semantics { contentDescription = chooseMonth }.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center, style = FinTheme.typography.label, color = FinTheme.colors.navy)
                    IconButton(onClick = { onMoveMonth(1) }) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.transactions_next_month),
                            modifier = Modifier.rotate(180f), tint = FinTheme.colors.navy)
                    }
                }
                val weekdays = DateFormatSymbols(locale).shortWeekdays
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..6).forEach { index ->
                        val calendarDay = if (index == 6) Calendar.SUNDAY else Calendar.MONDAY + index
                        Text(weekdays[calendarDay], Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                    }
                }
                val first = state.visibleMonth
                val days = Period.currentMonth(first).end.dayOfMonth
                val offset = first.dayOfWeek.ordinal
                val rows = (offset + days + 6) / 7
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { column ->
                            val day = row * 7 + column - offset + 1
                            if (day !in 1..days) {
                                Spacer(Modifier.weight(1f).height(40.dp))
                            } else {
                                val date = first.plus(day - 1, DateTimeUnit.DAY)
                                val endpoint = date == state.draftStart || date == state.draftEnd
                                val inside = state.valid && date >= requireNotNull(state.draftStart) && date <= requireNotNull(state.draftEnd)
                                val description = date.formatDate(locale)
                                val background = when {
                                    endpoint -> FinTheme.colors.yellow
                                    inside -> FinTheme.colors.cream
                                    else -> FinTheme.colors.white
                                }
                                Box(Modifier.weight(1f).height(40.dp).clip(FinTheme.shapes.r12).background(background)
                                    .clickable(role = Role.Button) { onSelectDay(date) }
                                    .semantics { selected = endpoint; contentDescription = description },
                                    contentAlignment = Alignment.Center) {
                                    Text(day.toString(), style = FinTheme.typography.label, color = FinTheme.colors.navy)
                                }
                            }
                        }
                    }
                }
            }
            FinSelectField(stringResource(R.string.transactions_period_start), state.draftStart?.formatDate(locale).orEmpty(),
                onClick = { onEditField(PeriodField.START) }, placeholder = stringResource(R.string.ui_choose_date))
            FinSelectField(stringResource(R.string.transactions_period_end), state.draftEnd?.formatDate(locale).orEmpty(),
                onClick = { onEditField(PeriodField.END) }, placeholder = stringResource(R.string.ui_choose_date))
            if (!state.valid) {
                Text(stringResource(if (state.draftEnd == null) R.string.transactions_select_end else R.string.transactions_period_error),
                    color = FinTheme.colors.red, style = FinTheme.typography.caption)
            }
            FinButton(stringResource(if (state.custom) R.string.transactions_calendar else R.string.transactions_custom_period),
                { onShowCustom(!state.custom) }, style = FinButtonStyle.Secondary)
            FinButton(stringResource(R.string.transactions_apply), onApply, enabled = state.valid)
            FinButton(stringResource(R.string.ui_close), onDismiss, style = FinButtonStyle.Secondary)
        }
    }
    state.editingField?.let { field ->
        val context = LocalContext.current
        val date = (if (field == PeriodField.START) state.draftStart else state.draftEnd) ?: state.visibleMonth
        DisposableEffect(field) {
            val dialog = DatePickerDialog(context, { _, year, month, day -> onFieldDate(LocalDate(year, month + 1, day)) },
                date.year, date.monthNumber - 1, date.dayOfMonth)
            dialog.setOnDismissListener { onCloseField() }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PeriodSheetPreview() {
    val period = Period.currentMonth(LocalDate(2026, 11, 14))
    FinDetectTheme { PeriodSheet(FinancePeriodState(period, period.start, period.start, period.end), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}
