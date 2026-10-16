package com.sushiomsky.proxmark.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand: "lab instrument" — phosphor green on deep slate.
// The dark scheme is the signature look: a professional RFID workbench,
// not a generic purple Material baseline.
private val BrandGreen = Color(0xFF4ADE80)
private val BrandGreenDark = Color(0xFF15803D)
private val BrandAmber = Color(0xFFFBBF24)
private val BrandAmberDark = Color(0xFFB45309)
private val BrandCyan = Color(0xFF22D3EE)
private val BrandCyanDark = Color(0xFF0E7490)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = BrandCyan,
    onSecondary = Color(0xFF062A33),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = BrandAmber,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF0A0E0C),
    onBackground = Color(0xFFE3EDE3),
    surface = Color(0xFF101512),
    onSurface = Color(0xFFE3EDE3),
    surfaceVariant = Color(0xFF1B241D),
    onSurfaceVariant = Color(0xFFB9C8B9)
)

private val LightColors = lightColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = BrandCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = BrandAmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFECACA),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF5F8F5),
    onBackground = Color(0xFF111811),
    surface = Color.White,
    onSurface = Color(0xFF111811),
    surfaceVariant = Color(0xFFE4ECE4),
    onSurfaceVariant = Color(0xFF3F4A3F)
)

@Composable
fun ProxmarkTheme(
    darkTheme: Boolean? = null,
    // Off by default: dynamic wallpaper colors would erase the app's
    // brand identity. Users pick Light/Dark/System in Settings instead.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        isDark -> DarkColors
        else -> LightColors
    }

    // Keep system bars legible over edge-to-edge content.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
