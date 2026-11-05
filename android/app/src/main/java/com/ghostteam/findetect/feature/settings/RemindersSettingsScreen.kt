package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
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

private enum class RemindersSheet { Time, Period }

/**
 * Напоминания (Figma 07): тумблер, время, периодичность, плашка про разрешение.
 * Тумблер выключен: время и периодичность приглушены и не нажимаются.
 * Всё только визуально, ничего не сохраняется.
 */
@Composable
fun RemindersSettingsScreen(onBack: () -> Unit = {}) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var timeIndex by rememberSaveable { mutableIntStateOf(2) }
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf<RemindersSheet?>(null) }

    val times = stringArrayResource(R.array.ui_reminder_times).toList()
    val periods = stringArrayResource(R.array.ui_reminder_periods).toList()
    val dimmed = Modifier.alpha(if (enabled) 1f else 0.4f)

    FinSubScreen(title = stringResource(R.string.ui_settings_reminders), onBack = onBack) {
        FinSwitchPanel(
            title = stringResource(R.string.ui_reminders_toggle),
            checked = enabled,
            onCheckedChange = { enabled = it },
        )
        TimeBox(
            title = stringResource(R.string.ui_reminders_time),
            time = times[timeIndex],
            modifier = dimmed,
            onClick = if (enabled) ({ sheet = RemindersSheet.Time }) else null,
        )
        FinRow(
            title = stringResource(R.string.ui_reminders_period),
            subtitle = periods[periodIndex],
            modifier = dimmed,
            leading = { UiIconTile(Icons.Outlined.CalendarMonth) },
            onClick = if (enabled) ({ sheet = RemindersSheet.Period }) else null,
        )
        FinNotice(
            title = stringResource(R.string.ui_notifications_permission),
            text = stringResource(R.string.ui_notifications_permission_hint),
        )
        // Макет: пока без действия. Позже: Intent на системные настройки уведомлений (ACTION_APP_NOTIFICATION_SETTINGS, API 26+, minSdk у нас 24).
        FinButton(
            text = stringResource(R.string.ui_open_settings),
            onClick = {},
            style = FinButtonStyle.Secondary,
        )
    }

    when (sheet) {
        RemindersSheet.Time -> SettingsChoiceSheet(
            title = stringResource(R.string.ui_reminders_choose_time),
            options = times,
            selectedIndex = timeIndex,
            onSelect = { timeIndex = it; sheet = null },
            onDismiss = { sheet = null },
        )
        RemindersSheet.Period -> SettingsChoiceSheet(
            title = stringResource(R.string.ui_reminders_period),
            options = periods,
            selectedIndex = periodIndex,
            onSelect = { periodIndex = it; sheet = null },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
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
private fun RemindersSettingsScreenPreview() { FinDetectTheme { RemindersSettingsScreen() } }