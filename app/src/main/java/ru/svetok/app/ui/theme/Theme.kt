package ru.svetok.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueTertiaryContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    error = BlueError,
    onError = BlueOnError,
    errorContainer = BlueErrorContainer,
    onErrorContainer = BlueOnErrorContainer,
    background = BlueBackground,
    onBackground = BlueOnBackground,
    surface = BlueSurface,
    onSurface = BlueOnSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BlueOnSurfaceVariant,
    outline = BlueOutline,
    inverseSurface = BlueInverseSurface,
    inverseOnSurface = BlueInverseOnSurface,
    inversePrimary = BlueInversePrimary,
    surfaceTint = BlueSurfaceTint,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = DarkBlueOnPrimary,
    primaryContainer = DarkBluePrimaryContainer,
    onPrimaryContainer = DarkBlueOnPrimaryContainer,
    secondary = DarkBlueSecondary,
    onSecondary = DarkBlueOnSecondary,
    secondaryContainer = DarkBlueSecondaryContainer,
    onSecondaryContainer = DarkBlueOnSecondaryContainer,
    tertiary = DarkBlueTertiary,
    onTertiary = DarkBlueOnTertiary,
    tertiaryContainer = DarkBlueTertiaryContainer,
    onTertiaryContainer = DarkBlueOnTertiaryContainer,
    error = DarkBlueError,
    onError = DarkBlueOnError,
    errorContainer = DarkBlueErrorContainer,
    onErrorContainer = DarkBlueOnErrorContainer,
    background = DarkBlueBackground,
    onBackground = DarkBlueOnBackground,
    surface = DarkBlueSurface,
    onSurface = DarkBlueOnSurface,
    surfaceVariant = DarkBlueSurfaceVariant,
    onSurfaceVariant = DarkBlueOnSurfaceVariant,
    outline = DarkBlueOutline,
    inverseSurface = DarkBlueInverseSurface,
    inverseOnSurface = DarkBlueInverseOnSurface,
    inversePrimary = DarkBlueInversePrimary,
    surfaceTint = DarkBlueSurfaceTint,
)

@Composable
fun SvetOkTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
