package com.chibi.photo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlatBlueColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryBlueLight,
    onSecondary = BlueDeepest,
    background = BlueBackground,
    onBackground = TextPrimary,
    surface = BlueCard,
    onSurface = TextPrimary,
    surfaceVariant = BlueRaised,
    onSurfaceVariant = TextSecondary,
    outline = BlueHairline,
    outlineVariant = BlueHairlineStrong,
    error = SemanticRed
)

@Composable
fun ChibiPhotoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlatBlueColorScheme,
        typography = Typography,
        content = content
    )
}
