package com.ghostteam.findetect.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ghostteam.findetect.core.designsystem.component.FinBottomSheet
import com.ghostteam.findetect.core.designsystem.component.FinRadio
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

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