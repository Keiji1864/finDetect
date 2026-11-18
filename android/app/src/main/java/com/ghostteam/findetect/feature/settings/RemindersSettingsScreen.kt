package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinSwitchPanel
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.prototype.UiIconTile
import com.ghostteam.findetect.data.reminder.ReminderNotifications
import com.ghostteam.findetect.domain.model.AppSettings

/** Daily reminder opt-in; Android permission is requested only after enabling the switch. */
@Composable
fun RemindersSettingsScreen(onBack: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notifications = remember(context) { ReminderNotifications(context.applicationContext) }
    RefreshNotificationPermission(viewModel::refreshSystemPermission)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshSystemPermission()
        viewModel.setReminderEnabled(granted)
    }
    RemindersContent(
        state = state, onBack = onBack, onRetry = viewModel::retryRead, onHour = viewModel::setReminderHour,
        onEnabled = { enabled ->
            if (!enabled) viewModel.setReminderEnabled(false)
            else {
                notifications.createChannel()
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else viewModel.setReminderEnabled(true)
            }
        },
        onSystemSettings = { context.startActivity(notifications.settingsIntent()) },
    )
}

@Composable
private fun RemindersContent(state: SettingsUiState, onBack: () -> Unit, onRetry: () -> Unit,
    onHour: (Int) -> Unit, onEnabled: (Boolean) -> Unit, onSystemSettings: () -> Unit) {
    var chooseTime by rememberSaveable { mutableStateOf(false) }
    val reminder = state.settings?.dailyReminder
    val enabled = reminder?.enabled == true
    val hours = (18..22).toList()
    val times = hours.map { stringResource(R.string.reminder_time, it) }
    val dimmed = Modifier.alpha(if (enabled) 1f else 0.4f)

    FinSubScreen(title = stringResource(R.string.ui_settings_reminders), onBack = onBack) {
        SettingsStatus(state, onRetry)
        if (reminder != null) {
            FinSwitchPanel(
                title = stringResource(R.string.ui_reminders_toggle),
                checked = enabled,
                onCheckedChange = { if (!state.saving) onEnabled(it) },
            )
            TimeBox(
                title = stringResource(R.string.ui_reminders_time),
                time = stringResource(R.string.reminder_time, reminder.hour),
                modifier = dimmed,
                onClick = if (enabled && !state.saving) ({ chooseTime = true }) else null,
            )
            FinRow(
                title = stringResource(R.string.ui_reminders_period),
                subtitle = stringResource(R.string.reminder_daily),
                modifier = dimmed,
                leading = { UiIconTile(R.drawable.ic_calendar) },
            )
        }
        FinNotice(
            title = stringResource(R.string.reminder_timing_title),
            text = stringResource(R.string.reminder_timing_hint),
        )
        if (!state.notificationsAllowed) {
            FinNotice(title = stringResource(R.string.ui_notifications_permission), text = stringResource(R.string.ui_notifications_permission_hint))
            FinButton(
                text = stringResource(R.string.ui_open_settings),
                onClick = onSystemSettings,
                style = FinButtonStyle.Secondary,
            )
        }
    }

    if (chooseTime && enabled && !state.saving && reminder != null) SettingsChoiceSheet(
            title = stringResource(R.string.ui_reminders_choose_time),
            options = times,
            selectedIndex = hours.indexOf(reminder.hour),
            onSelect = { onHour(hours[it]); chooseTime = false },
            onDismiss = { chooseTime = false },
        )
}

/** Блок "Время  20:00" по ширине содержимого (DESIGN.md 5.19): белый, обводка 1 dp, скругление 12. */
@Composable
private fun TimeBox(
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = FinTheme.colors
    val shape = FinTheme.shapes.r12
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .clip(shape)
            .background(c.white)
            .border(FinDimens.borderWidth, c.line, shape)
            .then(clickMod)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = FinTheme.typography.label, color = c.navy)
        Text(time, style = FinTheme.typography.timeValue, color = c.navy)
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemindersSettingsScreenPreview() { FinDetectTheme { RemindersContent(SettingsUiState(settings = AppSettings()), {}, {}, {}, {}, {}) } }
