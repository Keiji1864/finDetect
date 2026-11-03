package com.ghostteam.findetect.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ghostteam.findetect.R

/**
 * Шрифт Inter. Файлы кладём в res/font (имена строго строчными):
 * inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(weight: FontWeight, size: TextUnit, line: TextUnit) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = 0.sp,
)

/** Стили из Figma: FinDetect/<имя>, плюс дополнительные размеры из DESIGN.md (раздел 3). */
@Immutable
data class FinTypography(
    // --- основные текстовые стили Figma ---
    val balance: TextStyle = style(FontWeight.Bold, 40.sp, 48.sp),      // крупные суммы "24 850 ₽"
    val title: TextStyle = style(FontWeight.Bold, 28.sp, 34.sp),        // "Потрачено сегодня"
    val heading: TextStyle = style(FontWeight.SemiBold, 22.sp, 28.sp),  // название экрана в app bar, имя в профиле
    val subheading: TextStyle = style(FontWeight.SemiBold, 18.sp, 24.sp), // "7 дней учёта"
    val body: TextStyle = style(FontWeight.Normal, 16.sp, 24.sp),       // значение в поле ввода
    val label: TextStyle = style(FontWeight.Medium, 14.sp, 20.sp),      // название строки, текст кнопки
    val caption: TextStyle = style(FontWeight.Normal, 12.sp, 16.sp),    // подписи

    // --- дополнительные размеры ---
    val captionMedium: TextStyle = style(FontWeight.Medium, 12.sp, 16.sp), // пейджер "1 из 3", подпись вкладки активной
    val sheetTitle: TextStyle = style(FontWeight.Bold, 24.sp, 30.sp),   // заголовок шторки
    val dialogTitle: TextStyle = style(FontWeight.Bold, 20.sp, 26.sp),  // заголовок диалога
    val amountInput: TextStyle = style(FontWeight.Bold, 40.sp, 48.sp),  // сумма в шторке добавления
    val sectionTitle: TextStyle = style(FontWeight.SemiBold, 16.sp, 24.sp), // "Последние операции"
    val timeValue: TextStyle = style(FontWeight.SemiBold, 16.sp, 24.sp),    // "20:00" в напоминаниях
    val pinDigit: TextStyle = style(FontWeight.Medium, 20.sp, 24.sp),   // цифры PIN-клавиатуры
    val chip: TextStyle = style(FontWeight.Medium, 13.sp, 18.sp),       // текст чипа периода
    val axis: TextStyle = style(FontWeight.Normal, 11.sp, 14.sp),       // подписи осей графика
)

val LocalFinTypography = staticCompositionLocalOf { FinTypography() }

/** Маппинг на Material Typography, чтобы стандартные компоненты Material3 не выбивались из шрифта. */
fun FinTypography.toMaterial() = Typography(
    displayLarge = balance,
    headlineLarge = title,
    headlineMedium = heading,
    titleLarge = heading,
    titleMedium = subheading,
    titleSmall = sectionTitle,
    bodyLarge = body,
    bodyMedium = label.copy(fontWeight = FontWeight.Normal),
    bodySmall = caption,
    labelLarge = label,
    labelMedium = label,
    labelSmall = caption,
)