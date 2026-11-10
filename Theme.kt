package com.lugmety.orderassistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Premium Dark Navy Theme with Blue and Cyan Accents
private val DarkNavyColorScheme = darkColorScheme(
    primary = Color(0xFF48CAE4),          // Bright Cyan
    onPrimary = Color(0xFF0B132B),
    primaryContainer = Color(0xFF0077B6), // Deep Blue Accent
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00F5D4),        // Neon Cyan / Teal
    onSecondary = Color(0xFF0B132B),
    background = Color(0xFF0B132B),       // Deep Midnight Navy
    surface = Color(0xFF1C2541),          // Card Navy
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFF43F5E),
    onError = Color.White
)

private val LightNavyColorScheme = lightColorScheme(
    primary = Color(0xFF0077B6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF03045E),
    secondary = Color(0xFF0096C7),
    onSecondary = Color.White,
    background = Color(0xFFF0F4F8),
    surface = Color.White,
    onBackground = Color(0xFF0B132B),
    onSurface = Color(0xFF0B132B),
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun LugmetyTheme(
    darkTheme: Boolean = true, // Default to premium dark navy theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkNavyColorScheme else LightNavyColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
