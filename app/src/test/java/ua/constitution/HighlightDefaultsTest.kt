package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.utils.Constants

/**
 * Drift guard for the default highlight colors. ArticleCard's default marker/underline color
 * parameters now reference these constants (N6) instead of duplicating the hex literals; this pins
 * the user-visible default values so a change to the constants is a deliberate, reviewed decision.
 */
class HighlightDefaultsTest {

    @Test
    fun `default marker and underline colors are unchanged`() {
        assertEquals("#FFF59D", Constants.COLOR_DEFAULT_MARKER)
        assertEquals("#F57F17", Constants.COLOR_DEFAULT_UNDERLINE)
    }
}
