package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSelectField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.domain.model.IncomeRepeat
import com.ghostteam.findetect.domain.model.IncomeSchedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun IncomeScheduleRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onboarding: Boolean = false,
    advancing: Boolean = false,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val title = stringResource(if (onboarding) R.string.ui_onboarding_income_title else R.string.income_schedule_title)
    val busy = state.saving || advancing
    BackHandler(enabled = busy) {}
    val settings = state.settings
    if (settings == null) {
        FinSubScreen(title, onBack) { SettingsStatus(state, viewModel::retryRead) }
    } else {
        IncomeScheduleForm(
            settings.incomeSchedule, today, title, onboarding, busy, state.writeError, onBack,
            onSave = { schedule, date -> viewModel.saveIncome(schedule, date, onSaved) },
        )
    }
}

/** Общая форма даты дохода: черновик сохраняется при повороте, но записывается только по кнопке. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeScheduleForm(
    initial: IncomeSchedule?, today: LocalDate, title: String, onboarding: Boolean, busy: Boolean,
    writeError: Boolean, onBack: () -> Unit, onSave: (IncomeSchedule?, LocalDate?) -> Unit,
) {
    var selectedDay by rememberSaveable { mutableIntStateOf(
        (initial?.nextAfter(today) ?: initial?.anchor ?: today.plus(1, DateTimeUnit.MONTH)).toEpochDays()
    ) }
    val selected = LocalDate.fromEpochDays(selectedDay)
    var anchorDay by rememberSaveable { mutableIntStateOf(initial?.anchor?.toEpochDays() ?: selectedDay) }
    val originalRepeat = rememberSaveable { initial?.repeat?.name ?: IncomeRepeat.MONTHLY.name }
    var repeat by rememberSaveable { mutableStateOf(IncomeRepeat.valueOf(originalRepeat)) }
    val anchor = if (repeat.name == originalRepeat) LocalDate.fromEpochDays(anchorDay) else selected
    val schedule = IncomeSchedule(anchor, repeat)
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showRepeat by rememberSaveable { mutableStateOf(false) }
    val choices = IncomeRepeat.entries
    val repeatOptions = choices.map { option -> stringResource(when (option) {
        IncomeRepeat.MONTHLY -> R.string.income_repeat_monthly
        IncomeRepeat.BIWEEKLY -> R.string.income_repeat_biweekly
        IncomeRepeat.WEEKLY -> R.string.income_repeat_weekly
        IncomeRepeat.ONCE -> R.string.income_repeat_once
    }) }
    val c = FinTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Своя шапка: заголовок переносится на две строки, а у FinTopBar высота фиксированная
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = FinDimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(enabled = !busy, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = null,
                    tint = c.navy,
                    modifier = Modifier.size(FinDimens.iconSize),
                )
            }
            Text(
                text = title,
                style = FinTheme.typography.title,
                color = c.navy,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = FinDimens.screenPadding,
                    end = FinDimens.screenPadding,
                    top = FinDimens.blockGap,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            Text(
                text = stringResource(R.string.ui_onboarding_income_hint),
                style = FinTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = c.muted,
            )
            FinSelectField(
                label = stringResource(R.string.ui_onboarding_income_date),
                value = selected.formatDate(),
                onClick = { if (!busy) showDate = true },
            )
            FinSelectField(
                label = stringResource(R.string.ui_onboarding_income_repeat),
                value = if (repeat == IncomeRepeat.MONTHLY && anchor.dayOfMonth != selected.dayOfMonth)
                    stringResource(R.string.income_monthly_anchor, anchor.dayOfMonth)
                else repeatOptions[choices.indexOf(repeat)],
                onClick = { if (!busy) showRepeat = true },
            )
            if (onboarding) FinNotice(
                title = stringResource(R.string.ui_onboarding_income_notice),
                text = stringResource(R.string.ui_onboarding_income_notice_hint),
            )
            if (selected <= today) FinNotice(stringResource(R.string.income_schedule_future))
            if (writeError) FinNotice(stringResource(R.string.settings_write_error))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinDimens.screenPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            FinButton(
                text = stringResource(if (onboarding) R.string.ui_onboarding_continue else R.string.ui_save),
                onClick = { onSave(schedule, selected) },
                enabled = !busy && selected > today,
                loading = busy,
            )
            if (onboarding) FinButton(
                text = stringResource(R.string.ui_onboarding_skip),
                onClick = { onSave(null, null) },
                enabled = !busy,
                style = FinButtonStyle.Secondary,
            )
        }
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selected.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        val picked = pickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(enabled = picked != null && picked > today, onClick = {
                    picked?.let { selectedDay = it.toEpochDays(); anchorDay = selectedDay }
                    showDate = false
                }) { Text(stringResource(R.string.ui_onboarding_date_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.ui_onboarding_date_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showRepeat) {
        SettingsChoiceSheet(
            title = stringResource(R.string.ui_onboarding_income_repeat),
            options = repeatOptions,
            selectedIndex = choices.indexOf(repeat),
            onSelect = { repeat = choices[it]; showRepeat = false },
            onDismiss = { showRepeat = false },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncomeSchedulePreview() {
    FinDetectTheme {
        IncomeScheduleForm(null, LocalDate(2026, 11, 19), stringResource(R.string.ui_onboarding_income_title),
            onboarding = true, busy = false, writeError = false, onBack = {}, onSave = { _, _ -> })
    }
}
