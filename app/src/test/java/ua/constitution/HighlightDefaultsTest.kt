package ua.constitution
import ua.constitution.ui.theme.HighlightPalette

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drift guard for the default highlight colors. ArticleCard's default marker/underline color
 * parameters now reference these constants (N6) instead of duplicating the hex literals; this pins
 * the user-visible default values so a change to the constants is a deliberate, reviewed decision.
 */
class HighlightDefaultsTest {

    @Test
    fun `default marker and underline colors are unchanged`() {
        assertEquals("#FFF59D", HighlightPalette.DEFAULT_MARKER)
        assertEquals("#F57F17", HighlightPalette.DEFAULT_UNDERLINE)
    }
}
