package ua.constitution.ui.theme

import android.os.Build
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light mode: the established fixed national palette (yellow canvas, blue text/accents).
private val LightColorScheme = lightColorScheme(
    primary = SovereignBlue,
    secondary = SkyBlueLight,
    tertiary = SunflowerYellow,
    background = AppCanvasYellow, // Modern premium soft yellow canvas
    surface = Color(0xFFFFFFFF),     // Pure white sheet for content cards
    onPrimary = Color.White,
    onSecondary = SovereignBlue,
    onBackground = SovereignBlue,   // Royal blue text on yellow
    onSurface = SovereignBlue,      // Royal blue text on cards
    surfaceVariant = GoldAccentBorder,
    onSurfaceVariant = SovereignBlue
)

// Dark mode: navy reading surfaces with light text. The flag/brand accents (blue, yellow) are kept
// for national identity; only the canvas/cards/text adapt (see AppColors).
private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    secondary = SkyBlueLight,
    tertiary = SunflowerYellow,
    background = DarkAppColors.canvas,
    surface = DarkAppColors.cardSurface,
    onPrimary = Color.White,
    onSecondary = DarkAppColors.textPrimary,
    onBackground = DarkAppColors.textPrimary,
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.cardSurface,
    onSurfaceVariant = DarkAppColors.textSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    fontScale: Float = 1f,
    // Disable system dynamic color to enforce our national theme.
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
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    // Theme-aware text-selection highlight (the default light selection looks washed-out on dark).
    val selectionColors = TextSelectionColors(
        handleColor = appColors.textHeading,
        backgroundColor = appColors.textHeading.copy(alpha = 0.35f),
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.scaledBy(fontScale),
            content = content,
        )
    }
}
