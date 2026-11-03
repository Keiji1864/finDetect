package com.ghostteam.findetect.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Переменные Figma space/: 4, 8, 12, 16, 24, 32. */
object FinSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Размеры и сетка из DESIGN.md (раздел 1 и компоненты). Фрейм макета 412 x 915 dp. */
object FinDimens {
    val screenPadding = 24.dp      // горизонтальные поля экрана
    val blockGap = 12.dp           // шаг между блоками
    val headerGap = 8.dp           // шаг внутри заголовочного блока
    val cardPadding = 16.dp        // отступ внутри карточек

    val buttonHeight = 48.dp
    val fieldMinHeight = 76.dp
    val rowIconTile = 40.dp        // иконка-плитка в строке
    val smallTile = 32.dp          // плитка-галочка серии
    val iconSize = 24.dp
    val appBarHeight = 40.dp
    val bottomBarHeight = 72.dp
    val navIndicatorWidth = 28.dp
    val navIndicatorHeight = 3.dp
    val toggleWidth = 52.dp
    val toggleHeight = 32.dp
    val toggleThumb = 24.dp
    val radioSize = 24.dp
    val sheetHandleWidth = 40.dp
    val sheetHandleHeight = 4.dp
    val sheetBottomPadding = 28.dp
    val floatingPanelContentInset = 96.dp // нижний отступ контента под плавающей панелью
    val avatarProfile = 88.dp
    val borderWidth = 1.dp
    val borderFocused = 1.5.dp
}