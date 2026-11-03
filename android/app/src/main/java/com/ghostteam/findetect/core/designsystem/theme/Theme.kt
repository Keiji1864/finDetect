package com.ghostteam.findetect.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Корневая тема приложения
 * Тёмной темы нет
 */
@Composable
fun FinDetectTheme(content: @Composable () -> Unit) {
    val colors = LightFinColors
    val typography = FinTypography()
    val shapes = FinShapes()

    val materialScheme = lightColorScheme(
        primary = colors.yellow,
        onPrimary = colors.navy,
        secondary = colors.navy,
        onSecondary = colors.white,
        secondaryContainer = colors.cream,
        onSecondaryContainer = colors.navy,
        background = colors.paper,
        onBackground = colors.navy,
        surface = colors.white,
        onSurface = colors.navy,
        surfaceVariant = colors.paper,
        onSurfaceVariant = colors.muted,
        outline = colors.line,
        outlineVariant = colors.line,
        error = colors.red,
        onError = colors.white,
        scrim = colors.navy,
    )

    CompositionLocalProvider(
        LocalFinColors provides colors,
        LocalFinTypography provides typography,
        LocalFinShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = typography.toMaterial(),
            shapes = shapes.toMaterial(),
            content = content,
        )
    }
}

object FinTheme {
    val colors: FinColors
        @Composable @ReadOnlyComposable get() = LocalFinColors.current
    val typography: FinTypography
        @Composable @ReadOnlyComposable get() = LocalFinTypography.current
    val shapes: FinShapes
        @Composable @ReadOnlyComposable get() = LocalFinShapes.current
}