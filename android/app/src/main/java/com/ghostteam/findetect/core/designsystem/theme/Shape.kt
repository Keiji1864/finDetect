package com.ghostteam.findetect.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Скругления из DESIGN.md (раздел 4). Переменные Figma radius/: 8, 12, 16, 24. */
@Immutable
data class FinShapes(
    val r8: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** Кнопки, поля, карточки, плашки, строки списка, переключатель типа, иконка-плитка 40 dp */
    val r12: RoundedCornerShape = RoundedCornerShape(12.dp),
    val r16: RoundedCornerShape = RoundedCornerShape(16.dp),
    /** Шторка, только верхние углы */
    val sheet: RoundedCornerShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp
    ),
    /** Сегмент переключателя типа операции */
    val segment: RoundedCornerShape = RoundedCornerShape(9.dp),
    /** Чип периода, плитка-галочка 32 dp */
    val chip: RoundedCornerShape = RoundedCornerShape(10.dp),
    /** Индикатор вкладки, ручка шторки, полоса прогресса */
    val tiny: RoundedCornerShape = RoundedCornerShape(2.dp),
    val round: Shape = CircleShape,
)

val LocalFinShapes = staticCompositionLocalOf { FinShapes() }

fun FinShapes.toMaterial() = Shapes(
    extraSmall = r8,
    small = r8,
    medium = r12,
    large = r16,
    extraLarge = sheet,
)