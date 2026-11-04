package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile

/**
 * Настройки (Figma 07): пять строк, каждая ведёт на свой подэкран.
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
) {
    FinSubScreen(title = stringResource(R.string.ui_settings), onBack = onBack) {
        SettingsRow(R.string.ui_settings_appearance, R.string.ui_settings_appearance_hint, Icons.Outlined.Settings, onOpenAppearance)
        SettingsRow(R.string.ui_settings_currency, R.string.ui_settings_currency_hint, Icons.Outlined.Payments, onOpenCurrency)
        SettingsRow(R.string.ui_settings_security, R.string.ui_settings_security_hint, Icons.Outlined.Lock, onOpenSecurity)
        SettingsRow(R.string.ui_settings_reminders, R.string.ui_settings_reminders_hint, Icons.Outlined.CalendarMonth, onOpenReminders)
        SettingsRow(R.string.ui_settings_gamification, R.string.ui_settings_gamification_hint, Icons.Outlined.EmojiEvents, onOpenGamification)
    }
}

@Composable
private fun SettingsRow(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FinRow(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        leading = { UiIconTile(icon) },
        onClick = onClick,
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() { FinDetectTheme { SettingsScreen() } }