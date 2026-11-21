package com.ghostteam.findetect.feature.achievements

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.component.FinIconTile
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.util.formatDate
import com.ghostteam.findetect.domain.model.AchievementCode
import com.ghostteam.findetect.domain.model.AchievementProgress
import com.ghostteam.findetect.domain.model.AchievementRules
import com.ghostteam.findetect.domain.model.AchievementsProgress
import com.ghostteam.findetect.domain.model.EarnedAchievement
import com.ghostteam.findetect.feature.prototype.UiIconTile
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Full catalogue row with earned date or actual progress; unavailable conditions have no counter. */
@Composable
fun AchievementCard(achievement: AchievementProgress, modifier: Modifier = Modifier) {
    FinCard(modifier.semantics(mergeDescendants = true) {}) {
        AchievementContent(achievement, compact = false)
    }
}

/** Home summary: latest earned badge, or the first available target when none has been earned. */
@Composable
fun AchievementsPreviewCard(state: AchievementsUiState, onOpen: () -> Unit, onRetry: () -> Unit) {
    val progress = state.progress
    FinCard(onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val title = if (progress != null && !state.loading && !state.error) {
                    stringResource(R.string.achievements_home_title, progress.earnedCount, progress.totalCount)
                } else stringResource(R.string.achievements_title)
                Text(title, Modifier.weight(1f), style = FinTheme.typography.label, color = FinTheme.colors.navy)
                Icon(Icons.Outlined.ChevronRight, null, tint = FinTheme.colors.muted, modifier = Modifier.size(20.dp))
            }
            when {
                state.error -> AchievementsReadError(onRetry)
                state.loading || progress == null -> Text(stringResource(R.string.finance_loading),
                    style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                else -> progress.preview?.let { AchievementContent(it, compact = true) }
            }
        }
    }
}

@Composable
internal fun AchievementsReadError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.achievements_read_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
        FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
    }
}

@Composable
private fun AchievementContent(achievement: AchievementProgress, compact: Boolean) {
    val earned = achievement.earned
    val locale = LocalConfiguration.current.locales[0]
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        if (earned != null) {
            UiIconTile(achievement.code.iconRes())
        } else {
            FinIconTile(background = Color.Transparent) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = stringResource(R.string.achievement_locked),
                    modifier = Modifier.size(FinDimens.iconSize),
                    tint = FinTheme.colors.muted,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(achievement.code.nameRes()), style = FinTheme.typography.label, color = FinTheme.colors.navy)
            Text(achievementCondition(achievement.code), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            if (!compact) {
                if (earned != null) {
                    Text(
                        text = stringResource(R.string.achievement_earned_on, earned.earnedOn.formatDate(locale)),
                        style = FinTheme.typography.captionMedium,
                        color = FinTheme.colors.navy,
                    )
                }
                val current = achievement.current
                val target = achievement.target
                if (earned == null && current != null && target != null) {
                    Text(stringResource(R.string.achievement_progress, current, target),
                        style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                }
                Text(if (earned?.isLegacy == true) stringResource(R.string.achievement_legacy_reward)
                    else stringResource(R.string.achievement_reward, achievement.rewardXp),
                    style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
        }
    }
}

internal fun achievementsPreviewProgress(earned: Boolean = true): AchievementsProgress {
    val at = Instant.parse("2026-11-17T12:00:00Z")
    val date = LocalDate(2026, 11, 17)
    return AchievementsProgress(AchievementCode.entries.map { code ->
        val badge = if (earned && code in setOf(AchievementCode.FIRST_CLUE, AchievementCode.STREAK_3)) {
            EarnedAchievement(code, at, date, isLegacy = code == AchievementCode.FIRST_CLUE, rewardPending = false)
        } else null
        val target = AchievementRules.target(code)
        AchievementProgress(code, badge, target?.let { if (badge != null) it else if (earned) minOf(it, 2) else 0 })
    })
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun AchievementCardsPreview() {
    val progress = achievementsPreviewProgress()
    FinDetectTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AchievementsPreviewCard(AchievementsUiState(progress, loading = false), {}, {})
            AchievementCard(progress.achievements.first())
            AchievementCard(progress.achievements.first { it.code == AchievementCode.STREAK_3 })
            AchievementCard(progress.achievements.first { it.code == AchievementCode.STREAK_7 })
            AchievementCard(progress.achievements.last())
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AchievementStatesPreview() {
    FinDetectTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AchievementsPreviewCard(AchievementsUiState(achievementsPreviewProgress(earned = false), loading = false), {}, {})
            AchievementsPreviewCard(AchievementsUiState(), {}, {})
            AchievementsPreviewCard(AchievementsUiState(loading = false, error = true), {}, {})
        }
    }
}
