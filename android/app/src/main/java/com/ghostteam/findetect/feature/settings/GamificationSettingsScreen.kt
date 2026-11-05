package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.component.FinSwitchPanel
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme

/**
 * Игровые элементы: в Figma экрана нет (DESIGN.md: "[не нарисован]"),
 * собран из готовых компонентов: тумблер + плашка с пояснением (файл 04, раздел 8).
 */
@Composable
fun GamificationSettingsScreen(onBack: () -> Unit = {}) {
    var enabled by rememberSaveable { mutableStateOf(true) }

    FinSubScreen(title = stringResource(R.string.ui_settings_gamification), onBack = onBack) {
        FinSwitchPanel(
            title = stringResource(R.string.ui_settings_gamification),
            checked = enabled,
            onCheckedChange = { enabled = it },
        )
        FinNotice(
            title = stringResource(R.string.ui_gamification_about),
            text = stringResource(R.string.ui_gamification_about_hint),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GamificationSettingsScreenPreview() { FinDetectTheme { GamificationSettingsScreen() } }