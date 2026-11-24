package com.chibi.photo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val bgBackdrop: Color,
    val surfaceRaisedActive: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        bgBackdrop = LightBgBackdrop,
        surfaceRaisedActive = LightSurfaceRaisedActive,
        textMuted = LightTextMuted,
        isDark = false
    )
}

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimaryBlueActive,
    secondary = LightPrimaryBlueActive,
    onSecondary = Color.White,
    background = LightBgMain,
    onBackground = LightTextPrimary,
    surface = LightSurfaceCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderHairline,
    outlineVariant = LightBorderStrong,
    error = SemanticRed
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = Color(0xFF070E1A),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkPrimaryBlueActive,
    onSecondary = Color(0xFF070E1A),
    background = DarkBgMain,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderHairline,
    outlineVariant = DarkBorderStrong,
    error = SemanticRed
)

@Composable
fun ChibiPhotoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            bgBackdrop = DarkBgBackdrop,
            surfaceRaisedActive = DarkSurfaceRaisedActive,
            textMuted = DarkTextMuted,
            isDark = true
        )
    } else {
        ExtendedColors(
            bgBackdrop = LightBgBackdrop,
            surfaceRaisedActive = LightSurfaceRaisedActive,
            textMuted = LightTextMuted,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object StudioTheme {
    val extended: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
