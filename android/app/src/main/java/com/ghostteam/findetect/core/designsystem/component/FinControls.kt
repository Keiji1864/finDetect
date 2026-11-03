package com.ghostteam.findetect.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

/** Тумблер 52 x 32 (DESIGN.md 5.7). ВКЛ: жёлтый трек, ползунок справа. ВЫКЛ: серый трек, ползунок слева. */
@Composable
fun FinSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = FinTheme.colors
    val track by animateColorAsState(if (checked) c.yellow else c.line, label = "switchTrack")
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) FinDimens.toggleWidth - FinDimens.toggleThumb - 4.dp else 4.dp,
        label = "switchThumb",
    )
    Box(
        modifier = modifier
            .size(FinDimens.toggleWidth, FinDimens.toggleHeight)
            .clip(FinTheme.shapes.round)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(FinDimens.toggleThumb)
                .shadow(2.dp, FinTheme.shapes.round)
                .background(c.white, FinTheme.shapes.round),
        )
    }
}

/** Радио 24 dp (DESIGN.md 5.8). ВКЛ: кольцо navy + точка 12 dp. ВЫКЛ: кольцо slate. */
@Composable
fun FinRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = FinTheme.colors
    val ring = if (selected) c.navy else c.slate
    val base = modifier
        .size(FinDimens.radioSize)
        .border(2.dp, ring, FinTheme.shapes.round)
    Box(
        modifier = if (onClick != null) {
            base.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        } else base,
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(12.dp).background(c.navy, FinTheme.shapes.round))
        }
    }
}

/**
 * Переключатель типа операции "Расходы | Доходы" (DESIGN.md 5.15).
 * Показывается ТОЛЬКО на шторке с экрана "Главный" (AddTransactionMode.Switchable).
 */
@Composable
fun <T> FinSegmentedSwitch(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FinTheme.shapes.r12)
            .background(c.paper)
            .border(FinDimens.borderWidth, c.line, FinTheme.shapes.r12)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, title) ->
            Segment(title = title, active = key == selected) { onSelect(key) }
        }
    }
}

@Composable
private fun RowScope.Segment(title: String, active: Boolean, onClick: () -> Unit) {
    val c = FinTheme.colors
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(FinTheme.shapes.segment)
            .background(if (active) c.yellow else Color.Transparent)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = FinTheme.typography.label,
            color = if (active) c.navy else c.muted,
            textAlign = TextAlign.Center,
        )
    }
}