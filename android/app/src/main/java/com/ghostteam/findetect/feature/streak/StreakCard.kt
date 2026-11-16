package com.ghostteam.findetect.feature.streak

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinSpacing
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.domain.model.StreakEvent
import com.ghostteam.findetect.domain.model.StreakProgress
import com.ghostteam.findetect.domain.usecase.CalculateStreakUseCase
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Shared streak card: a real calendar week, loading/error states, and messages only for today's events. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreakCard(
    progress: StreakProgress?,
    loading: Boolean,
    error: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    expanded: Boolean = false,
) {
    val colors = FinTheme.colors
    FinCard(modifier = modifier, background = colors.cream, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(FinSpacing.m)) {
            when {
                error -> {
                    Text(stringResource(R.string.streak_title), style = FinTheme.typography.subheading, color = colors.navy)
                    Text(stringResource(R.string.streak_read_error), style = FinTheme.typography.label, color = colors.red)
                    FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
                }
                loading || progress == null -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FinSpacing.s)) {
                        CircularProgressIndicator(Modifier.size(FinDimens.iconSize), color = colors.navy)
                        Text(stringResource(R.string.finance_loading), style = FinTheme.typography.label, color = colors.muted)
                    }
                }
                else -> {
                    val title = pluralStringResource(R.plurals.streak_tracking_days, progress.currentStreak, progress.currentStreak)
                    if (expanded) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(FinSpacing.s)) {
                            Icon(painterResource(R.drawable.ic_flame), null, tint = colors.yellow, modifier = Modifier.size(FinDimens.rowIconTile))
                            Text(title, style = FinTheme.typography.title, color = colors.navy, textAlign = TextAlign.Center)
                        }
                    } else {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(FinSpacing.s)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FinSpacing.xs)) {
                                Icon(painterResource(R.drawable.ic_flame), null, tint = colors.yellow, modifier = Modifier.size(FinDimens.iconSize))
                                Text(title, style = FinTheme.typography.subheading, color = colors.navy)
                            }
                            Text(pluralStringResource(R.plurals.streak_record_days, progress.bestStreak, progress.bestStreak),
                                style = FinTheme.typography.caption, color = colors.muted)
                        }
                    }
                    StreakWeek(progress)
                    if (progress.lastActiveDate == null) {
                        Text(stringResource(R.string.streak_start_hint), style = FinTheme.typography.label, color = colors.muted)
                    }
                    val eventText = when (progress.event) {
                        StreakEvent.NONE -> null
                        StreakEvent.GRACE_USED -> R.string.streak_grace_used
                        StreakEvent.GRACE_RESTORED -> R.string.streak_grace_restored
                        StreakEvent.STREAK_RESET -> R.string.streak_reset
                    }
                    if (eventText != null) Text(stringResource(eventText), style = FinTheme.typography.label, color = colors.navy)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StreakWeek(progress: StreakProgress) {
    val colors = FinTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val names = listOf(R.string.ui_mon, R.string.ui_tue, R.string.ui_wed, R.string.ui_thu,
        R.string.ui_fri, R.string.ui_sat, R.string.ui_sun)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(FinSpacing.s), maxItemsInEachRow = 7) {
        progress.week.forEach { day ->
            val isToday = day.date == progress.today
            val status = when {
                day.isActive -> R.string.streak_day_active
                day.date > progress.today -> R.string.streak_day_future
                isToday -> R.string.streak_day_today_inactive
                else -> R.string.streak_day_inactive
            }
            val date = day.date.formatDate(locale)
            val dateLabel = if (isToday) stringResource(R.string.streak_today_date, date) else date
            val description = stringResource(R.string.streak_day_description, dateLabel, stringResource(status))
            Column(Modifier.widthIn(min = FinDimens.smallTile).clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FinSpacing.xs)) {
                Box(Modifier.size(FinDimens.smallTile)
                    .background(if (day.isActive) colors.yellow else colors.white, FinTheme.shapes.chip)
                    .border(FinDimens.borderWidth, if (isToday) colors.navy else if (day.isActive) colors.yellow else colors.line,
                        FinTheme.shapes.chip), contentAlignment = Alignment.Center) {
                    if (day.isActive) Icon(painterResource(R.drawable.ic_check), null, tint = colors.navy,
                        modifier = Modifier.size(FinDimens.iconSize))
                }
                Text(stringResource(names[day.date.dayOfWeek.ordinal]), style = FinTheme.typography.caption, color = colors.muted)
            }
        }
    }
}

internal fun streakPreviewProgress(): StreakProgress {
    val today = LocalDate(2026, 11, 19)
    return CalculateStreakUseCase()(List(5) { today.minus(it, DateTimeUnit.DAY) }, today)
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun StreakCardPreview() {
    FinDetectTheme { StreakCard(streakPreviewProgress(), loading = false, error = false, onRetry = {}) }
}
