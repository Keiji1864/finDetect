package com.ghostteam.findetect.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.component.FinBottomSheet
import com.ghostteam.findetect.core.designsystem.component.FinRadio
import com.ghostteam.findetect.core.designsystem.component.FinRow
import com.ghostteam.findetect.core.designsystem.component.FinSwitch
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/** Cream-панель с тумблером (DESIGN.md 5.7): "Напоминать об учёте", "Игровые элементы". */
@Composable
internal fun SettingsSwitchPanel(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FinTheme.shapes.r12)
            .background(FinTheme.colors.cream)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = FinTheme.typography.label,
            color = FinTheme.colors.navy,
        )
        FinSwitch(checked = checked, onCheckedChange = onCheckedChange)
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