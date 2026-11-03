package com.ghostteam.findetect.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme

@Composable
fun FinIconTile(
    modifier: Modifier = Modifier,
    size: Dp = FinDimens.rowIconTile,
    background: Color = FinTheme.colors.cream,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(FinTheme.shapes.r12)
            .background(background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Строка списка (DESIGN.md 5.3): [плитка] [название + подпись] [значение справа].
 * Операция расхода: trailing = Text("− 980 ₽"). Доход: зелёный. Счёт: справа баланс.
 * onClick = null делает строку некликабельной (так на "Последних операциях" на Главном).
 * inSheet = true даёт фон paper вместо белого (строки внутри шторки).
 */
@Composable
fun FinRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false,
    inSheet: Boolean = false,
) {
    val c = FinTheme.colors
    val shape = FinTheme.shapes.r12
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (inSheet) c.paper else c.white)
            .then(clickMod)
            .padding(
                start = 14.dp,
                end = 16.dp,
                top = if (compact) 10.dp else 14.dp,
                bottom = if (compact) 10.dp else 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FinTheme.typography.label,
                color = c.navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = FinTheme.typography.caption,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** Плашка-подсказка (cream, без обводки): "Нет интернета", подсказки, предупреждения. */
@Composable
fun FinNotice(
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FinTheme.shapes.r12)
            .background(FinTheme.colors.cream)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = FinTheme.typography.label, color = FinTheme.colors.navy)
        if (text != null) {
            Text(text, style = FinTheme.typography.caption, color = FinTheme.colors.muted)
        }
    }
}

/** Совет ИИ: то же, что Notice, но с обводкой 1 dp line и отступом 16. Под ним ставим вторичную кнопку "Совет полезен". */
@Composable
fun FinAdviceCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    caption: String? = null, // например "Финансовый совет"
    pager: String? = null,   // например "1 из 3" (шевроны рисуем снаружи через trailing при необходимости)
    onClick: (() -> Unit)? = null,
) {
    val c = FinTheme.colors
    val shape = FinTheme.shapes.r12
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.cream)
            .border(FinDimens.borderWidth, c.line, shape)
            .then(clickMod)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (caption != null || pager != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (caption != null) Text(caption, style = FinTheme.typography.captionMedium, color = c.navy)
                if (pager != null) Text(pager, style = FinTheme.typography.captionMedium, color = c.navy)
            }
        }
        Text(title, style = FinTheme.typography.label, color = c.navy)
        Text(text, style = FinTheme.typography.caption, color = c.muted)
    }
}

/**
 * Чип периода (DESIGN.md 5.5): cream, скругление 10, текст 13 Medium + шеврон вниз.
 * selected = true (выбран месяц целиком внутри шторки) красит в yellow.
 */
@Composable
fun FinPeriodChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val c = FinTheme.colors
    Row(
        modifier = modifier
            .clip(FinTheme.shapes.chip)
            .background(if (selected) c.yellow else c.cream)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = FinTheme.typography.chip, color = c.navy)
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = c.navy,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** Белая карточка-контейнер (скругление 12, без тени, внутри отступ 16). Для блоков на Главном и в Профиле. */
@Composable
fun FinCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = FinTheme.colors.white,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val shape = FinTheme.shapes.r12
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val borderMod = if (borderColor != null) Modifier.border(FinDimens.borderWidth, borderColor, shape) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(borderMod)
            .then(clickMod)
            .padding(FinDimens.cardPadding),
    ) { content() }
}