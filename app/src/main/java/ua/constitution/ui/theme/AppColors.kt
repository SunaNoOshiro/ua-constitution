package ua.constitution.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware semantic colours that flip between light and dark mode. Only the *reading surfaces*
 * adapt here — the national-identity brand colours (FlagBlue/FlagGold, SunflowerYellow header, the
 * coat-of-arms gradient, the bottom-nav accents) deliberately stay fixed in both modes, so they are
 * NOT part of this holder. Provided via [LocalAppColors] by `MyApplicationTheme`.
 */
@Immutable
data class AppColors(
    val canvas: Color,        // screen background behind the content
    val cardSurface: Color,   // article / content card background
    val cardBorder: Color,    // card outline
    val textPrimary: Color,   // body + title reading text
    val textSecondary: Color, // muted / secondary text
)

/** Light mode reuses the established palette, so migrating a surface to [LocalAppColors] is a no-op
 *  visually in light mode (only dark mode changes). */
val LightAppColors = AppColors(
    canvas = AppCanvasYellow,
    cardSurface = Color.White,
    cardBorder = SovereignBlue,
    textPrimary = RichNavyText,
    textSecondary = SovereignBlue,
)

val DarkAppColors = AppColors(
    canvas = Color(0xFF0E1621),       // deep navy canvas
    cardSurface = Color(0xFF18232F),  // raised navy card
    cardBorder = Color(0xFF2E4257),   // subtle navy outline
    textPrimary = Color(0xFFE6EDF5),  // near-white body text
    textSecondary = Color(0xFF9FB4C9),// muted blue-grey
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
