package ua.constitution.ui

import androidx.compose.ui.graphics.Color

/** Parses a hex color string into a Compose Color, falling back to a default on any error. */

fun safeParseColor(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        val normalized = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(normalized))
    } catch (e: Exception) {
        default
    }
}
