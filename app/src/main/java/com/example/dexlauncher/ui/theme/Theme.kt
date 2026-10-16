package com.example.dexlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DeXDarkPalette.PrimaryBlue,
    onPrimary = DeXDarkPalette.TextPrimary,
    secondary = DeXDarkPalette.ActiveCyan,
    onSecondary = DeXDarkPalette.Background,
    background = DeXDarkPalette.Background,
    onBackground = DeXDarkPalette.TextPrimary,
    surface = DeXDarkPalette.Surface,
    onSurface = DeXDarkPalette.TextPrimary,
    surfaceVariant = DeXDarkPalette.SurfaceVariant,
    onSurfaceVariant = DeXDarkPalette.TextSecondary
)

@Composable
fun DeXLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = DeXTypography,
        content = content
    )
}
