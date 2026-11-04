package com.ghostteam.findetect.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/**
 * Каркас экрана второго уровня: стрелка "назад" и название (Title 28 Bold, как в Figma v2),
 * справа иконок нет, нижней навигации нет (DESIGN.md 5.10, 5.11).
 * Контент скроллится: отступы 24 по бокам, промежуток между блоками 12.
 */
@Composable
fun FinSubScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        FinTopBar(title = title, onBack = onBack, titleStyle = FinTheme.typography.title)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = FinDimens.screenPadding,
                    end = FinDimens.screenPadding,
                    top = FinDimens.blockGap,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
            content = content,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 600)
@Preview(showBackground = true, widthDp = 412, heightDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinSubScreenPreview() {
    FinDetectTheme {
        FinSubScreen(title = stringResource(R.string.ui_settings), onBack = {}) {
            FinRow(title = stringResource(R.string.ui_settings_currency), subtitle = stringResource(R.string.ui_settings_currency_hint))
        }
    }
}