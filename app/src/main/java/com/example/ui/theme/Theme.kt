package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = lightColorScheme(
    primary = SovereignBlue,
    secondary = SkyBlueLight,
    tertiary = SunflowerYellow,
    background = Color(0xFFFFFDE7), // Modern premium soft yellow canvas
    surface = Color(0xFFFFFFFF),     // Pure white sheet for content cards
    onPrimary = Color.White,
    onSecondary = SovereignBlue,
    onBackground = SovereignBlue,   // Royal blue text on yellow
    onSurface = SovereignBlue,      // Royal blue text on cards
    surfaceVariant = GoldAccentBorder,
    onSurfaceVariant = SovereignBlue
)

private val LightColorScheme = lightColorScheme(
    primary = SovereignBlue,
    secondary = SkyBlueLight,
    tertiary = SunflowerYellow,
    background = Color(0xFFFFFDE7), // Modern premium soft yellow canvas
    surface = Color(0xFFFFFFFF),     // Pure white sheet for content cards
    onPrimary = Color.White,
    onSecondary = SovereignBlue,
    onBackground = SovereignBlue,   // Royal blue text on yellow
    onSurface = SovereignBlue,      // Royal blue text on cards
    surfaceVariant = GoldAccentBorder,
    onSurfaceVariant = SovereignBlue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable system dynamic color to enforce our premium national theme 
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
