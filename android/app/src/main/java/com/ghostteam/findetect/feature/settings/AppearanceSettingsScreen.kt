package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinCard
import com.ghostteam.findetect.core.designsystem.component.FinSelectField
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.feature.prototype.DisplayRow

private enum class AppearanceSheet { Theme, Language }

/** Оформление и язык (Figma 07): поля "Тема" и "Язык" открывают шторки выбора. Всё только визуально. */
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit = {}) {
    var theme by rememberSaveable { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf<AppearanceSheet?>(null) }

    val themes = listOf(R.string.ui_theme_light, R.string.ui_theme_dark, R.string.ui_theme_system)
        .map { stringResource(it) }
    val languages = listOf(R.string.ui_language_ru, R.string.ui_language_en)
        .map { stringResource(it) }

    FinSubScreen(title = stringResource(R.string.ui_settings_appearance), onBack = onBack) {
        FinSelectField(
            label = stringResource(R.string.ui_theme),
            value = themes[theme],
            onClick = { sheet = AppearanceSheet.Theme },
        )
        FinSelectField(
            label = stringResource(R.string.ui_language),
            value = languages[language],
            onClick = { sheet = AppearanceSheet.Language },
        )
        FinCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.ui_preview), style = FinTheme.typography.label, color = FinTheme.colors.navy)
                Text(stringResource(R.string.ui_preview_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted)
            }
        }
        DisplayRow(R.string.ui_groceries, R.string.ui_store, R.string.ui_groceries_amount, R.drawable.ic_basket)
    }

    when (sheet) {
        AppearanceSheet.Theme -> SettingsChoiceSheet(
            title = stringResource(R.string.ui_theme),
            options = themes,
            selectedIndex = theme,
            onSelect = { theme = it; sheet = null },
            onDismiss = { sheet = null },
        )
        AppearanceSheet.Language -> SettingsChoiceSheet(
            title = stringResource(R.string.ui_language),
            options = languages,
            selectedIndex = language,
            onSelect = { language = it; sheet = null },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceSettingsScreenPreview() { FinDetectTheme { AppearanceSettingsScreen() } }