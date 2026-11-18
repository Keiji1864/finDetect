package com.ghostteam.findetect.feature.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinButtonStyle
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinSpacing
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.domain.model.DayActivity
import com.ghostteam.findetect.domain.model.LocalDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The same no-spending action and feedback on both screens, independent of gamification visibility. */
@Composable
fun NoSpendingSection(
    state: DayActivityUiState,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(FinSpacing.s)) {
        FinButton(
            text = stringResource(if (state.activity?.noSpendingConfirmed == true) R.string.home_day_confirmed else R.string.home_no_spending),
            onClick = onConfirm,
            style = FinButtonStyle.Secondary,
            enabled = state.canConfirm,
            loading = state.saving,
        )
        if (state.activity?.hasExpense == true && !state.activity.noSpendingConfirmed) {
            Text(stringResource(R.string.home_has_expense), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
        if (state.dayChanged) {
            Text(stringResource(R.string.home_day_changed), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
        if (state.error) {
            Text(stringResource(R.string.home_activity_error), style = FinTheme.typography.caption, color = FinTheme.colors.red)
            FinButton(stringResource(R.string.finance_retry), onRetry, style = FinButtonStyle.Secondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoSpendingSectionPreview() {
    FinDetectTheme {
        NoSpendingSection(
            DayActivityUiState(LocalDay(LocalDate(2026, 11, 19), TimeZone.UTC), DayActivity(), loading = false), {}, {},
        )
    }
}
