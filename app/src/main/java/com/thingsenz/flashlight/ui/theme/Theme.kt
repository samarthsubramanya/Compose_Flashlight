package com.thingsenz.flashlight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlashlightColorScheme = darkColorScheme(
    primary = BeamAmber,
    onPrimary = VoidBlack,
    secondary = BeamAmberBright,
    onSecondary = VoidBlack,
    tertiary = StrobeActive,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = PanelBlack,
    onSurface = TextPrimary,
    surfaceVariant = PanelBlackElevated,
    onSurfaceVariant = TextMuted,
    outline = OutlineDim,
    error = SosIdle,
)

/**
 * Always dark: this is a dedicated flashlight UI, not a general-purpose app,
 * so it intentionally ignores system light/dark and dynamic color.
 */
@Composable
fun FlashlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlashlightColorScheme,
        typography = FlashlightTypography,
        shapes = FlashlightShapes,
        content = content
    )
}
