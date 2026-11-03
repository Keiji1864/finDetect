package com.ghostteam.findetect.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Палитра FinDetect (Figma: FinDetect / Primitives, режим Light).
 * Тёмной темы нет
 */
@Immutable
data class FinColors(
    val navy: Color,    // основной текст, иконки, активная вкладка, точки PIN
    val yellow: Color,  // главная кнопка, акцент, тумблер ВКЛ, прогресс
    val paper: Color,   // фон экранов, фон панели с плавающей кнопкой
    val white: Color,   // карточки, поля, шторки, нижняя навигация
    val muted: Color,   // второстепенный текст, неактивные вкладки
    val line: Color,    // обводки, линии сетки, выключенный тумблер
    val cream: Color,   // плашки-подсказки, иконки-плитки, чипы, диапазон в календаре
    val slate: Color,   // ручка шторки, выключенный радио
    val green: Color,   // доходы (+)
    val red: Color,     // удаление, ошибки
    val scrim: Color,   // затемнение под шторкой (navy 40%)
)

val LightFinColors = FinColors(
    navy = Color(0xFF1E2E47),
    yellow = Color(0xFFFFDB46),
    paper = Color(0xFFF8F8F4),
    white = Color(0xFFFFFFFF),
    muted = Color(0xFF6E7784),
    line = Color(0xFFE5E6E5),
    cream = Color(0xFFFFF7D7),
    slate = Color(0xFF9DAAC0),
    green = Color(0xFF16835D),
    red = Color(0xFFD94040),
    scrim = Color(0x661E2E47), // 0x66 = 40% альфа
)

val LocalFinColors = staticCompositionLocalOf { LightFinColors }

/**
 * Цвета категорий для диаграмм и легенд.
 * Для "Другое" (cream) ОБЯЗАТЕЛЬНО рисовать обводку 1 dp line, иначе сливается с белым.
 */
object FinCategoryColors {
    val Products = Color(0xFFFFDB46)
    val Cafe = Color(0xFF1E2E47)
    val Transport = Color(0xFF9DAAC0)
    val Other = Color(0xFFFFF7D7)
    val OtherBorder = Color(0xFFE5E6E5)
}