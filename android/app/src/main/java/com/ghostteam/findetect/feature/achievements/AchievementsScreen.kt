package com.ghostteam.findetect.feature.achievements

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

@Composable
fun AchievementsScreen(state: AchievementsScreenState, onBack: () -> Unit, onRetry: () -> Unit) {
    FinSubScreen(title = stringResource(R.string.achievements_title), onBack = onBack, titleMaxLines = 2) {
        val content = state.achievements
        val progress = content.progress
        when {
            state.settingsError -> {
                Text(stringResource(R.string.settings_read_error), style = FinTheme.typography.label, color = FinTheme.colors.red)
                FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
            }
            state.gameElementsEnabled == null -> {
                Text(stringResource(R.string.finance_loading), style = FinTheme.typography.label, color = FinTheme.colors.muted)
            }
            state.gameElementsEnabled == false -> {
                FinNotice(stringResource(R.string.achievements_game_disabled), text = stringResource(R.string.achievements_game_disabled_hint))
            }
            content.error -> AchievementsReadError(onRetry)
            content.loading || progress == null -> {
                Text(stringResource(R.string.finance_loading), style = FinTheme.typography.label, color = FinTheme.colors.muted)
            }
            else -> {
                Text(stringResource(R.string.achievements_count, progress.earnedCount, progress.totalCount),
                    style = FinTheme.typography.sectionTitle, color = FinTheme.colors.navy)
                progress.achievements.forEach { AchievementCard(it) }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 740, fontScale = 2f)
@Composable
private fun AchievementsScreenPreview() {
    FinDetectTheme {
        AchievementsScreen(AchievementsScreenState(AchievementsUiState(achievementsPreviewProgress(), loading = false),
            gameElementsEnabled = true), {}, {})
    }
}
