package com.ghostteam.findetect.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

enum class FinButtonStyle { Primary, Secondary, Danger }

/**
 * Кнопка 48 dp на всю ширину контента (DESIGN.md 5.1).
 * Состояния: обычное, недоступное (enabled = false), загрузка (loading = true, ширина сохраняется).
 */

@Composable
fun FinButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FinButtonStyle = FinButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = FinTheme.colors
    val container = when (style) {
        FinButtonStyle.Primary -> c.yellow
        FinButtonStyle.Secondary -> c.white
        FinButtonStyle.Danger -> c.red
    }
    val content = when (style) {
        FinButtonStyle.Primary, FinButtonStyle.Secondary -> c.navy
        FinButtonStyle.Danger -> c.white
    }
    val border = if (style == FinButtonStyle.Secondary && enabled) BorderStroke(1.dp, c.navy) else null

    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(FinDimens.buttonHeight),
        shape = FinTheme.shapes.r12,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = c.line,
            disabledContentColor = c.muted,
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.CenterVertically),
                color = content,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, style = FinTheme.typography.label, textAlign = TextAlign.Center)
        }
    }
}