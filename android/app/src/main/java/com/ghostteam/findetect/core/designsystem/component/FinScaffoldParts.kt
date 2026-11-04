package com.ghostteam.findetect.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ghostteam.findetect.core.designsystem.theme.FinDimens
import com.ghostteam.findetect.core.designsystem.theme.FinTheme
import androidx.compose.ui.text.TextStyle

/**
 * App bar (DESIGN.md 5.10)
 * Слева название экрана (Heading 22 Semi Bold), справа на главных вкладках иконки графика и настроек.
 * На экранах со стрелкой "назад" actions пустые.
 */
@Composable
fun FinTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleStyle: TextStyle = FinTheme.typography.heading,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FinDimens.appBarHeight)
            .padding(start = if (onBack != null) 12.dp else FinDimens.screenPadding, end = FinDimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                // TODO: заменить на векторную иконку Icon/back из UI-kit (res/drawable)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = FinTheme.colors.navy,
                    modifier = Modifier.size(FinDimens.iconSize),
                )
            }
        }
        Text(
            text = title,
            style = titleStyle,
            color = FinTheme.colors.navy,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Иконка в app bar (график, настройки): 24 dp, цвет navy, под ней кликабельная область. */
@Composable
fun FinTopBarIcon(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    contentDescription: String?,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = FinTheme.colors.navy,
        modifier = Modifier
            .size(FinDimens.iconSize)
            .clickable(onClick = onClick),
    )
}

/** Вкладка с ресурсной иконкой или стандартным вектором Material. */
data class FinTab(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int? = null,
    val icon: ImageVector? = null,
)

/** Нижняя навигация (DESIGN.md 5.11): 4 вкладки, высота 72, обводка сверху 1 dp, индикатор 28 x 3 yellow. */
@Composable
fun FinBottomBar(
    tabs: List<FinTab>,
    selectedRoute: String?,
    onSelect: (FinTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FinTheme.colors
    Column(modifier = modifier.background(c.white).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(FinDimens.borderWidth).background(c.line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FinDimens.bottomBarHeight)
                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
        ) {
            tabs.forEach { tab ->
                val active = tab.route == selectedRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tint = if (active) c.navy else c.muted
                    if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(FinDimens.iconSize),
                        )
                    } else if (tab.iconRes != null) {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(FinDimens.iconSize),
                        )
                    }
                    Text(
                        text = tab.label,
                        style = if (active) FinTheme.typography.captionMedium else FinTheme.typography.caption,
                        color = if (active) c.navy else c.muted,
                    )
                    Box(
                        Modifier
                            .width(FinDimens.navIndicatorWidth)
                            .height(FinDimens.navIndicatorHeight)
                            .background(if (active) c.yellow else Color.Transparent, FinTheme.shapes.tiny),
                    )
                }
            }
        }
    }
}

/**
 * Плавающая панель действия (DESIGN.md 5.12): только на "Главный", над нижней навигацией.
 * Фон paper, отступы 24 по бокам и 12 сверху и снизу, тень вверх.
 * Контенту под ней нужен нижний отступ FinDimens.floatingPanelContentInset (96 dp).
 * Тень здесь — приближение стандартной Compose-тенью (в Figma: 0/-4, blur 14, navy 16%).
 */
@Composable
fun FinFloatingActionPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = FinTheme.colors
    val shadowColor = c.navy.copy(alpha = 0.16f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RectangleShape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
            .background(c.paper)
            .padding(horizontal = FinDimens.screenPadding, vertical = 12.dp),
        content = content,
    )
}

/**
 * Шторка (DESIGN.md 5.14): белая, верхние углы 24, ручка 40 x 4 slate, затемнение navy 40%,
 * отступы 24 по бокам и 28 снизу, промежуток между элементами 12.
 * Закрывается свайпом вниз, тапом по затемнению и системным "назад" (всё это делает ModalBottomSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = FinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = FinTheme.shapes.sheet,
        containerColor = c.white,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(FinDimens.sheetHandleWidth, FinDimens.sheetHandleHeight)
                    .background(c.slate, FinTheme.shapes.tiny),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FinDimens.screenPadding, end = FinDimens.screenPadding, bottom = FinDimens.sheetBottomPadding),
            verticalArrangement = Arrangement.spacedBy(FinDimens.blockGap),
            content = content,
        )
    }
}
