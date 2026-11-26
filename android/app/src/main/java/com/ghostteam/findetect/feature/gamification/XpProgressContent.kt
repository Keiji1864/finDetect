package com.ghostteam.findetect.feature.gamification

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.XpTitle
import com.ghostteam.findetect.domain.usecase.CalculateXpProgressUseCase
import com.ghostteam.findetect.feature.prototype.UiIconTile

/** Profile/streak level row; expanded also explains lifetime XP and the next threshold. */
@Composable
fun XpProgressCard(state: XpProgressUiState, onRetry: () -> Unit, modifier: Modifier = Modifier, expanded: Boolean = false) {
    FinCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UiIconTile(R.drawable.ic_trophy)
            XpProgressContent(state, onRetry, Modifier.weight(1f), expanded)
        }
    }
}

/** Shared level/title and XP bar; loading and errors never display a fabricated zero level. */
@Composable
fun XpProgressContent(state: XpProgressUiState, onRetry: () -> Unit, modifier: Modifier = Modifier, expanded: Boolean = false, levelInHeader: Boolean = false) {
    val progress = state.progress
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            state.error -> {
                Text(stringResource(R.string.xp_read_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
                FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
            }
            state.loading || progress == null -> {
                Text(stringResource(R.string.finance_loading), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
            else -> {
                Text(if (levelInHeader) stringResource(progress.title.titleRes())
                    else stringResource(R.string.xp_level_title, progress.level, stringResource(progress.title.titleRes())),
                    style = FinTheme.typography.captionMedium, color = FinTheme.colors.navy)
                val levelLength = progress.xpForCurrentLevel
                val remaining = progress.xpToNextLevel
                Text(
                    if (progress.isMaxLevel) stringResource(R.string.xp_max_level)
                    else stringResource(if (levelInHeader) R.string.profile_exp_progress else R.string.xp_level_progress, progress.xpInCurrentLevel, checkNotNull(levelLength)),
                    style = FinTheme.typography.caption, color = FinTheme.colors.muted,
                )
                val description = if (progress.isMaxLevel) stringResource(R.string.xp_max_accessibility, progress.totalXp)
                    else stringResource(R.string.xp_progress_accessibility, progress.xpInCurrentLevel,
                        checkNotNull(levelLength), checkNotNull(remaining))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(FinTheme.shapes.tiny).background(FinTheme.colors.line)
                    .semantics {
                        contentDescription = description
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.progressFraction, 0f..1f)
                    }) {
                    Box(Modifier.fillMaxWidth(progress.progressFraction).fillMaxHeight().background(FinTheme.colors.yellow))
                }
                if (expanded || progress.isMaxLevel) {
                    Text(stringResource(R.string.xp_total, progress.totalXp),
                        style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                }
                if (expanded && remaining != null) {
                    Text(stringResource(R.string.xp_remaining, remaining),
                        style = FinTheme.typography.caption, color = FinTheme.colors.muted)
                }
            }
        }
    }
}

@StringRes
private fun XpTitle.titleRes(): Int = when (this) {
    XpTitle.TRAINEE -> R.string.xp_title_trainee
    XpTitle.INVESTIGATOR -> R.string.xp_title_investigator
    XpTitle.SENIOR_INVESTIGATOR -> R.string.xp_title_senior_investigator
    XpTitle.DETECTIVE -> R.string.xp_title_detective
    XpTitle.CHIEF_DETECTIVE -> R.string.xp_title_chief_detective
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
private fun XpProgressCardsPreview() {
    FinDetectTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val progress = XpProgressUiState(CalculateXpProgressUseCase()(700L), loading = false)
            XpProgressCard(progress, {})
            XpProgressCard(progress, {}, expanded = true)
            XpProgressCard(XpProgressUiState(CalculateXpProgressUseCase()(6_000L), loading = false), {}, expanded = true)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun XpProgressStatesPreview() {
    FinDetectTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            XpProgressCard(XpProgressUiState(), {})
            XpProgressCard(XpProgressUiState(loading = false, error = true), {})
        }
    }
}
