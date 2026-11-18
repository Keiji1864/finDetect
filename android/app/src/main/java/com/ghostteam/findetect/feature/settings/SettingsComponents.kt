package com.ghostteam.findetect.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinButton
import com.ghostteam.findetect.core.designsystem.component.FinNotice
import com.ghostteam.findetect.core.designsystem.component.FinBottomSheet
import com.ghostteam.findetect.core.designsystem.component.FinRadio
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme

/** Shared loading and persistence errors for settings pages, without replacing stored values with defaults. */
@Composable
internal fun SettingsStatus(state: SettingsUiState, onRetry: () -> Unit) {
    when {
        state.readError -> {
            FinNotice(stringResource(R.string.settings_read_error))
            FinButton(stringResource(R.string.finance_retry), onRetry)
        }
        state.settings == null -> Text(stringResource(R.string.finance_loading), color = FinTheme.colors.muted, style = FinTheme.typography.caption)
    }
    if (state.writeError) FinNotice(stringResource(R.string.settings_write_error))
}

@Composable
internal fun RefreshNotificationPermission(refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val currentRefresh by rememberUpdatedState(refresh)
    DisposableEffect(owner) {
        currentRefresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) currentRefresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Шторка выбора одного варианта: заголовок и строки с радио (по образцу шторки периода).
 * Выбор применяется сразу, вызывающий код сам закрывает шторку.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsChoiceSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    FinBottomSheet(onDismiss = onDismiss) {
        Text(text = title, style = FinTheme.typography.sheetTitle, color = FinTheme.colors.navy)
        options.forEachIndexed { index, option ->
            FinRow(
                title = option,
                inSheet = true,
                onClick = { onSelect(index) },
                trailing = { FinRadio(selected = index == selectedIndex, onClick = null) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsStatusPreview() {
    FinDetectTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { SettingsStatus(SettingsUiState(readError = true), {}) }
    }
}
