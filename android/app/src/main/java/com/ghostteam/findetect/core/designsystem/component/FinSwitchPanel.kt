package com.ghostteam.findetect.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/**
 * Cream-панель с названием и тумблером справа (DESIGN.md 5.7):
 * "Учитывать в прогнозе", "Напоминать об учёте", "Игровые элементы".
 * Состояние хранит вызывающий код.
 */
@Composable
fun FinSwitchPanel(
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

@Preview(showBackground = true, widthDp = 364)
@Preview(showBackground = true, widthDp = 364, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinSwitchPanelPreview() {
    FinDetectTheme {
        var checked by remember { mutableStateOf(true) }
        FinSwitchPanel(title = "Учитывать в прогнозе", checked = checked, onCheckedChange = { checked = it })
    }
}