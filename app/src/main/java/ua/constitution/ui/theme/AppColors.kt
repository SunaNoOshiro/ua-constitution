package ua.constitution.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware semantic colours that flip between light and dark mode. Light mode reuses the existing
 * palette, so migrating a surface to [LocalAppColors] is a visual no-op in light; only dark mode
 * changes. The flag/coat-of-arms/anthem SYMBOL visuals keep their own fixed colours and are not here.
 */
@Immutable
data class AppColors(
    val canvas: Color,        // screen background behind the content
    val cardSurface: Color,   // article / content card background
    val cardBorder: Color,    // card outline
    val textPrimary: Color,   // body reading text
    val textSecondary: Color, // muted / secondary text
    val textHeading: Color,   // article numbers/titles + section headers (the brand-blue text)
    val brandSurface: Color,  // top header bar + bottom navigation background
    val onBrand: Color,       // text / icons sitting on [brandSurface]
    val navIndicator: Color,  // selected bottom-nav item highlight
    val danger: Color,        // destructive-action text (e.g. confirm-delete button)
)

val LightAppColors = AppColors(
    canvas = AppCanvasYellow,
    cardSurface = Color.White,
    cardBorder = SovereignBlue,
    textPrimary = RichNavyText,
    textSecondary = SovereignBlue,
    textHeading = SovereignBlue,
    brandSurface = SunflowerYellow,
    onBrand = SovereignBlue,
    navIndicator = NationalYellowBg,
    danger = ErrorRedStrong,
)

// Dark mode: navy surfaces, light body text, a soft-blue heading colour, and a dark header/nav with
// flag-yellow content (the blue-on-yellow brand inverts to yellow-on-dark).
val DarkAppColors = AppColors(
    canvas = Color(0xFF0E1621),
    cardSurface = Color(0xFF18232F),
    cardBorder = Color(0xFF2E4257),
    textPrimary = Color(0xFFE6EDF5),
    textSecondary = Color(0xFF9FB4C9),
    textHeading = Color(0xFF8FC1F0),
    brandSurface = Color(0xFF12202E),
    onBrand = SunflowerYellow,
    navIndicator = Color(0xFF2A3F54),
    danger = ErrorRed, // brighter than ErrorRedStrong so the destructive action stays legible on navy
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
