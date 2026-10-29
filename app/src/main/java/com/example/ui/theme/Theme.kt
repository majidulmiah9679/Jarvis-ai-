package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisObsidian,
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = JarvisIceWhite,
    secondary = JarvisElectricBlue,
    onSecondary = JarvisIceWhite,
    secondaryContainer = JarvisDeepNavy,
    onSecondaryContainer = JarvisCyan,
    tertiary = JarvisEmerald,
    onTertiary = JarvisObsidian,
    background = JarvisObsidian,
    onBackground = JarvisIceWhite,
    surface = JarvisDeepNavy,
    onSurface = JarvisIceWhite,
    surfaceVariant = JarvisSurfaceCard,
    onSurfaceVariant = JarvisMutedCyan,
    error = JarvisCrimson,
    onError = Color.White,
    outline = JarvisCyanDim
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
