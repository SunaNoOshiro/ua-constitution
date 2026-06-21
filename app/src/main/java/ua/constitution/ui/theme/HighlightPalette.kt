package ua.constitution.ui.theme

/**
 * The highlight/underline color palette, kept as hex strings (not Compose [androidx.compose.ui.graphics.Color])
 * because these values are persisted in the bookmark edits JSON, parsed back via safeParseColor, and
 * compared as strings in RangeStyler.
 *
 * Lives in the theme package so ALL color tokens have one home: Color.kt holds the Compose brand
 * palette, this holds the serialized highlight palette. (Moved out of utils/Constants, which was a
 * general-purpose bucket, not the place for presentation color tokens.)
 */
object HighlightPalette {
    const val DEFAULT_MARKER = "#FFF59D"
    const val DEFAULT_UNDERLINE = "#F57F17"

    val MARKER_COLORS = listOf(
        "#FFF59D", // Soft Yellow
        "#FFE0B2", // Soft Orange
        "#C8E6C9", // Soft Green
        "#B2DFDB", // Soft Teal
        "#BBDEFB", // Soft Blue
        "#E1BEE7", // Soft Purple
        "#FFCDD2"  // Soft Red
    )

    val UNDERLINE_COLORS = listOf(
        "#F57F17", // Deep Gold
        "#E65100", // Deep Orange
        "#1B5E20", // Deep Green
        "#004D40", // Deep Teal
        "#0D47A1", // Deep Blue
        "#4A148C", // Deep Purple
        "#B71C1C"  // Deep Red
    )
}
