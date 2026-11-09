package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AndroidSystemDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = JarvisSurfaceElevated,
    onPrimaryContainer = JarvisIceWhite,
    secondary = JarvisEmerald,
    onSecondary = Color(0xFF0A3818),
    secondaryContainer = JarvisDeepNavy,
    onSecondaryContainer = JarvisIceWhite,
    tertiary = JarvisElectricBlue,
    onTertiary = Color.White,
    background = JarvisObsidian,
    onBackground = JarvisIceWhite,
    surface = JarvisDeepNavy,
    onSurface = JarvisIceWhite,
    surfaceVariant = JarvisSurfaceCard,
    onSurfaceVariant = JarvisMutedCyan,
    error = JarvisCrimson,
    onError = Color(0xFF601410),
    outline = JarvisCyanDim
)

@Composable
fun MyApplicationTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        AndroidSystemDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
