package com.ghostteam.findetect.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/** Shared four-digit indicator and keypad. PIN digits are never exposed in semantics or saved state. */
@Composable
fun FinPinInput(
    title: String, filled: Int, onDigit: (Char) -> Unit, onDelete: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    val description = stringResource(R.string.pin_dots_description, filled)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = FinTheme.typography.heading, color = FinTheme.colors.navy, textAlign = TextAlign.Center)
        Text(stringResource(R.string.ui_pin_hint), style = FinTheme.typography.caption, color = FinTheme.colors.muted, textAlign = TextAlign.Center)
        Row(Modifier.padding(vertical = 16.dp).clearAndSetSemantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val dot = Modifier.size(20.dp)
                Box(if (index < filled) dot.background(FinTheme.colors.navy, FinTheme.shapes.round)
                    else dot.border(3.dp, FinTheme.colors.navy, FinTheme.shapes.round))
            }
        }
        listOf("123", "456", "789", " 0 ").forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { column, digit ->
                    when {
                        rowIndex == 3 && column == 0 -> Spacer(Modifier.weight(1f))
                        rowIndex == 3 && column == 2 -> PinKey(Modifier.weight(1f), enabled, onDelete) {
                            Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.ui_pin_delete), tint = FinTheme.colors.navy, modifier = Modifier.size(24.dp))
                        }
                        else -> PinKey(Modifier.weight(1f), enabled, { onDigit(digit) }) {
                            Text(digit.toString(), style = FinTheme.typography.pinDigit, color = FinTheme.colors.navy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(modifier: Modifier, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.height(56.dp).clip(FinTheme.shapes.r12).background(FinTheme.colors.white)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinPinInputPreview() {
    FinDetectTheme { FinPinInput(stringResource(R.string.ui_pin_create), 2, {}, {}) }
}
