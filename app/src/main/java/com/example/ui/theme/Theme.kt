package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HhpDarkColorScheme = darkColorScheme(
    primary = RadioCyanHighlight,
    onPrimary = Color(0xFF00363A),
    primaryContainer = RadioNavyElevated,
    onPrimaryContainer = RadioCyanHighlight,
    secondary = RadioBlueElectric,
    onSecondary = Color.White,
    secondaryContainer = RadioNavyCard,
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = RadioTXGreen,
    onTertiary = Color(0xFF003915),
    background = RadioNavyDark,
    onBackground = RadioTextPrimary,
    surface = RadioNavySurface,
    onSurface = RadioTextPrimary,
    surfaceVariant = RadioNavyCard,
    onSurfaceVariant = RadioTextSecondary,
    outline = RadioOutline,
    error = RadioBusyRed,
    onError = Color.White
)

@Composable
fun HhpRadioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HhpDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HhpRadioTheme(content = content)
}
