package com.thingsenz.flashlight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BeamAmberOnDark,
    onPrimary = VoidBlack,
    secondary = BeamAmberBright,
    onSecondary = VoidBlack,
    tertiary = StrobeActive,
    background = VoidBlack,
    onBackground = TextPrimaryDark,
    surface = PanelBlack,
    onSurface = TextPrimaryDark,
    surfaceVariant = PanelBlackElevated,
    onSurfaceVariant = TextMutedDark,
    outline = OutlineDim,
    error = SosIdle,
)

private val LightColorScheme = lightColorScheme(
    primary = BeamAmberOnLight,
    onPrimary = PanelWhite,
    secondary = BeamAmberOnLight,
    onSecondary = PanelWhite,
    tertiary = StrobeIdle,
    background = PaperWhite,
    onBackground = TextPrimaryLight,
    surface = PanelWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = PanelWhiteDim,
    onSurfaceVariant = TextMutedLight,
    outline = OutlineLight,
    error = SosIdle,
)

/**
 * Follows the system light/dark setting automatically — no in-app toggle.
 * Doesn't use dynamic color: the torch/amber palette is intentional branding,
 * not derived from the user's wallpaper.
 */
@Composable
fun FlashlightTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FlashlightTypography,
        shapes = FlashlightShapes,
        content = content
    )
}
