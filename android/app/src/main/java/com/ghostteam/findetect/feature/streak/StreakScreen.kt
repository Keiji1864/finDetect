package com.ghostteam.findetect.feature.streak

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.DayActivity
import com.ghostteam.findetect.domain.model.LocalDay
import com.ghostteam.findetect.feature.activity.DayActivityUiState
import com.ghostteam.findetect.feature.activity.NoSpendingSection
import kotlinx.datetime.TimeZone

@Composable
fun StreakScreen(
    state: StreakUiState,
    onBack: () -> Unit,
    onConfirmNoSpending: () -> Unit,
    onRetry: () -> Unit,
) {
    FinSubScreen(title = stringResource(R.string.streak_title), onBack = onBack, titleMaxLines = 2) {
        when {
            state.settingsError -> {
                Text(stringResource(R.string.settings_read_error), style = FinTheme.typography.label, color = FinTheme.colors.red)
                FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
            }
            state.gameElementsEnabled == null -> {
                Text(stringResource(R.string.finance_loading), style = FinTheme.typography.label, color = FinTheme.colors.muted)
            }
            state.gameElementsEnabled == true -> {
                StreakCard(state.progress, state.loading, state.error, onRetry, expanded = true)
                FinNotice(stringResource(R.string.streak_every_day_title), text = stringResource(R.string.streak_every_day_hint))
            }
            else -> FinNotice(stringResource(R.string.streak_game_disabled), text = stringResource(R.string.streak_game_disabled_hint))
        }
        NoSpendingSection(state.dayStatus, onConfirmNoSpending, onRetry)
        val progress = state.progress
        if (state.gameElementsEnabled == true && progress != null && !state.loading && !state.error) {
            FinRow(title = stringResource(R.string.streak_record_title), trailing = {
                Text(pluralStringResource(R.plurals.streak_days, progress.bestStreak, progress.bestStreak),
                    style = FinTheme.typography.label, color = FinTheme.colors.navy)
            })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 740, fontScale = 2f)
@Composable
private fun StreakScreenPreview() {
    val progress = streakPreviewProgress()
    FinDetectTheme {
        StreakScreen(StreakUiState(
            dayStatus = DayActivityUiState(LocalDay(progress.today, TimeZone.UTC), DayActivity(), loading = false),
            progress = progress, loading = false, gameElementsEnabled = true,
        ), {}, {}, {})
    }
}
