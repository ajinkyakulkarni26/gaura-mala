package com.gauramala.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val GauraColorScheme = ColorScheme(
    primary = GauraGold,
    primaryContainer = GauraAmberDark,
    onPrimary = BackgroundBlack,
    onPrimaryContainer = OnSurfaceWhite,
    secondary = GauraGoldLight,
    onSecondary = BackgroundBlack,
    tertiary = GauraSaffron,
    onTertiary = BackgroundBlack,
    background = BackgroundBlack,
    onBackground = OnSurfaceWhite,
    surfaceContainer = SurfaceDark,
    onSurface = OnSurfaceWhite,
    onSurfaceVariant = OnSurfaceMuted,
    outline = GauraAmberDark
)

@Composable
fun GauraMalaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GauraColorScheme,
        typography = GauraTypography,
        content = content
    )
}
