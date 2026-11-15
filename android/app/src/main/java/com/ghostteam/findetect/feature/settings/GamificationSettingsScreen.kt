package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinSwitchPanel
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.domain.model.AppSettings

/**
 * Игровые элементы: в Figma экрана нет (DESIGN.md: "[не нарисован]"),
 * собран из готовых компонентов: тумблер + плашка с пояснением (файл 04, раздел 8).
 */
@Composable
fun GamificationSettingsScreen(onBack: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GamificationContent(state, viewModel::setGameEnabled, viewModel::retryRead, onBack)
}

@Composable
private fun GamificationContent(state: SettingsUiState, onChange: (Boolean) -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    FinSubScreen(title = stringResource(R.string.ui_settings_gamification), onBack = onBack) {
        SettingsStatus(state, onRetry)
        state.settings?.let { settings -> FinSwitchPanel(
            title = stringResource(R.string.ui_settings_gamification),
            checked = settings.gameElementsEnabled,
            onCheckedChange = { if (!state.saving) onChange(it) },
        ) }
        FinNotice(
            title = stringResource(R.string.ui_gamification_about),
            text = stringResource(R.string.ui_gamification_about_hint),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GamificationSettingsScreenPreview() { FinDetectTheme { GamificationContent(SettingsUiState(settings = AppSettings()), {}, {}, {}) } }
