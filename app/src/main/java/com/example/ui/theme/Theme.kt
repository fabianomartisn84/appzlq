package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanMinimalColorScheme = darkColorScheme(
    primary = MinimalSkyBlue,
    onPrimary = ContrastNavy,
    primaryContainer = SlateBlueGray,
    onPrimaryContainer = TextLightGray,
    secondary = SlateBlueGray,
    onSecondary = TextLightGray,
    surface = BackgroundDark,
    onSurface = TextLightGray,
    background = BackgroundDark,
    onBackground = TextLightGray,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force clean dark theme by default for camera contrast alignment
    dynamicColor: Boolean = false, // Disable to respect user requested custom design theme colors perfectly
    content: @Composable () -> Unit,
) {
    // We use the custom clean minimal design scheme to guarantee perfect color matching on all devices.
    val colorScheme = CleanMinimalColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
