package com.ghostteam.findetect.feature.onboarding

import android.content.res.Configuration
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSelectField
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.settings.SettingsChoiceSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Дата по умолчанию: ровно через месяц от сегодня, в UTC (так работает Material DatePicker). */
private fun defaultIncomeMillis(): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        add(Calendar.MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** "27 октября 2026" */
private fun formatIncomeDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

/**
 * Онбоардинг, шаг "Когда следующий доход?" (Figma 06): стрелка "назад", заголовок в две строки,
 * подсказка, поля "Следующее поступление" (календарь) и "Повторять" (шторка), плашка,
 * внизу "Продолжить" и "Пропустить". Ничего не сохраняется: и "Продолжить", и "Пропустить" идут дальше.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingIncomeScreen(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    var dateMillis by rememberSaveable { mutableLongStateOf(defaultIncomeMillis()) }
    var repeatIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showRepeat by rememberSaveable { mutableStateOf(false) }
    val repeatOptions = stringArrayResource(R.array.ui_onboarding_repeat_options).toList()
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
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
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
                text = stringResource(R.string.ui_onboarding_income_title),
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
                value = formatIncomeDate(dateMillis),
                onClick = { showDate = true },
            )
            FinSelectField(
                label = stringResource(R.string.ui_onboarding_income_repeat),
                value = repeatOptions[repeatIndex],
                onClick = { showRepeat = true },
            )
            FinNotice(
                title = stringResource(R.string.ui_onboarding_income_notice),
                text = stringResource(R.string.ui_onboarding_income_notice_hint),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FinDimens.screenPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
        ) {
            FinButton(text = stringResource(R.string.ui_onboarding_continue), onClick = onContinue)
            FinButton(
                text = stringResource(R.string.ui_onboarding_skip),
                onClick = onSkip,
                style = FinButtonStyle.Secondary,
            )
        }
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
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
            selectedIndex = repeatIndex,
            onSelect = { repeatIndex = it; showRepeat = false },
            onDismiss = { showRepeat = false },
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingIncomeScreenPreview() { FinDetectTheme { OnboardingIncomeScreen() } }