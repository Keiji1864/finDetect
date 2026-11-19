package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.util.formatDate
import kotlinx.datetime.LocalDate
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.AppSettings
import com.ghostteam.findetect.domain.repository.PinStatus

/**
 * Настройки: каждая строка ведёт на свой подэкран.
 * Выхода из аккаунта здесь нет, он только в Профиле.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenGamification: () -> Unit = {},
    onOpenIncome: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    RefreshNotificationPermission(viewModel::refreshSystemPermission)
    SettingsContent(state, today, viewModel::retryRead, onBack, onOpenAppearance, onOpenCurrency, onOpenSecurity, onOpenReminders, onOpenGamification, onOpenIncome)
}

@Composable
private fun SettingsContent(
    state: SettingsUiState, today: LocalDate, onRetry: () -> Unit, onBack: () -> Unit,
    onOpenAppearance: () -> Unit, onOpenCurrency: () -> Unit, onOpenSecurity: () -> Unit,
    onOpenReminders: () -> Unit, onOpenGamification: () -> Unit, onOpenIncome: () -> Unit,
) {
    val settings = state.settings
    val loading = stringResource(R.string.finance_loading)
    FinSubScreen(title = stringResource(R.string.ui_settings), onBack = onBack) {
        SettingsStatus(state, onRetry)
        SettingsRow(R.string.ui_settings_appearance, stringResource(R.string.ui_settings_appearance_hint), R.drawable.ic_settings, onOpenAppearance)
        SettingsRow(R.string.ui_settings_currency, settings?.let { stringResource(currencyName(it.currency)) } ?: loading, R.drawable.ic_cash, onOpenCurrency)
        val schedule = settings?.incomeSchedule
        val incomeSummary = when {
            settings == null -> loading
            schedule == null -> stringResource(R.string.income_schedule_unset)
            else -> schedule.nextAfter(today)?.formatDate()
                ?: stringResource(R.string.income_schedule_due, schedule.anchor.formatDate())
        }
        SettingsRow(R.string.income_schedule_title, incomeSummary, R.drawable.ic_calendar, onOpenIncome)
        SettingsRow(R.string.ui_settings_security, stringResource(when (state.pin) {
            PinStatus.LOADING -> R.string.finance_loading
            PinStatus.DISABLED -> R.string.settings_pin_disabled
            PinStatus.ENABLED -> R.string.settings_pin_enabled
            PinStatus.ERROR -> R.string.settings_pin_error
        }), R.drawable.ic_lock, onOpenSecurity)
        val reminderSummary = when {
            settings == null -> loading
            !settings.dailyReminder.enabled -> stringResource(R.string.settings_disabled)
            !state.notificationsAllowed -> stringResource(R.string.reminder_system_blocked)
            else -> stringResource(R.string.reminder_summary, stringResource(R.string.reminder_time, settings.dailyReminder.hour))
        }
        SettingsRow(R.string.ui_settings_reminders, reminderSummary, R.drawable.ic_calendar, onOpenReminders)
        SettingsRow(R.string.ui_settings_gamification, settings?.let { stringResource(if (it.gameElementsEnabled) R.string.settings_enabled else R.string.settings_disabled) } ?: loading,
            R.drawable.ic_trophy, onOpenGamification)
    }
}

@Composable
private fun SettingsRow(
    @StringRes title: Int,
    subtitle: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    FinRow(
        title = stringResource(title),
        subtitle = subtitle,
        leading = { UiIconTile(icon) },
        onClick = onClick,
    )
}

@StringRes
private fun currencyName(currency: AppCurrency): Int = when (currency) {
    AppCurrency.RUB -> R.string.ui_currency_rub_name
    AppCurrency.USD -> R.string.ui_currency_usd_name
    AppCurrency.EUR -> R.string.ui_currency_eur_name
    AppCurrency.CNY -> R.string.ui_currency_cny_name
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    FinDetectTheme { SettingsContent(SettingsUiState(settings = AppSettings(), pin = PinStatus.DISABLED), LocalDate(2026, 11, 19), {}, {}, {}, {}, {}, {}, {}, {}) }
}
