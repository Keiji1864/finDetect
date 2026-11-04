package com.ghostteam.findetect.feature.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.R
import com.ghostteam.findetect.core.designsystem.component.FinSubScreen
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import kotlinx.coroutines.delay

private const val PinLength = 4

private val keypadRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
)

/**
 * Безопасность = экран создания PIN-кода (DESIGN.md 7: "экран не нарисован, использовать PIN-экран").
 * Макет: ввод 4 цифр, затем повтор, затем возврат назад. PIN нигде не сохраняется и не сверяется.
 */
@Composable
fun SecuritySettingsScreen(onBack: () -> Unit = {}) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var pin by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pin) {
        if (pin.length == PinLength) {
            delay(250) // даём увидеть четвёртую точку
            if (step == 0) {
                step = 1
                pin = ""
            } else {
                onBack()
            }
        }
    }

    FinSubScreen(title = stringResource(R.string.ui_settings_security), onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(if (step == 0) R.string.ui_pin_create else R.string.ui_pin_repeat),
                style = FinTheme.typography.heading,
                color = FinTheme.colors.navy,
            )
            Text(
                text = stringResource(R.string.ui_pin_hint),
                style = FinTheme.typography.caption,
                color = FinTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        PinDots(filled = pin.length, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp))
        PinKeypad(
            onDigit = { if (pin.length < PinLength) pin += it },
            onDelete = { pin = pin.dropLast(1) },
        )
    }
}

/** 4 точки 20 dp: заполненная navy, пустая с обводкой 3 dp navy (DESIGN.md 5.19). */
@Composable
private fun PinDots(filled: Int, modifier: Modifier = Modifier) {
    val c = FinTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PinLength) { index ->
            val dot = Modifier.size(20.dp)
            Box(
                if (index < filled) dot.background(c.navy, FinTheme.shapes.round)
                else dot.border(3.dp, c.navy, FinTheme.shapes.round),
            )
        }
    }
}

/** Клавиатура 3 x 4: нижний ряд "пусто, 0, удалить" (DESIGN.md 5.19). */
@Composable
private fun PinKeypad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keypadRows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    PinKey(Modifier.weight(1f), onClick = { onDigit(digit) }) { PinDigit(digit) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.weight(1f))
            PinKey(Modifier.weight(1f), onClick = { onDigit('0') }) { PinDigit('0') }
            PinKey(Modifier.weight(1f), onClick = onDelete) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.ui_pin_delete),
                    tint = FinTheme.colors.navy,
                    modifier = Modifier.size(FinDimens.iconSize),
                )
            }
        }
    }
}

@Composable
private fun PinDigit(digit: Char) {
    Text(digit.toString(), style = FinTheme.typography.pinDigit, color = FinTheme.colors.navy)
}

@Composable
private fun PinKey(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(FinTheme.shapes.r12)
            .background(FinTheme.colors.white)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 892)
@Preview(showBackground = true, widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecuritySettingsScreenPreview() { FinDetectTheme { SecuritySettingsScreen() } }