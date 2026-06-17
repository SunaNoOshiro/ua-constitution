package ua.constitution

import androidx.compose.ui.graphics.Color
import ua.constitution.ui.safeParseColor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterizes safeParseColor (MainActivity.kt). Needs Robolectric because it calls
 * android.graphics.Color.parseColor and wraps the result in a Compose Color.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorParsingTest {

    private val fallback = Color.Black

    @Test
    fun `null blank and whitespace input return the default`() {
        assertEquals(fallback, safeParseColor(null, fallback))
        assertEquals(fallback, safeParseColor("", fallback))
        assertEquals(fallback, safeParseColor("   ", fallback))
    }

    @Test
    fun `invalid hex returns the default`() {
        assertEquals(fallback, safeParseColor("GGGGGG", fallback))
        assertEquals(fallback, safeParseColor("nonsense", fallback))
    }

    @Test
    fun `valid hex parses to the expected color`() {
        val expected = Color(android.graphics.Color.parseColor("#FF0000"))
        assertEquals(expected, safeParseColor("#FF0000", fallback))
    }

    @Test
    fun `a missing leading hash is tolerated`() {
        assertEquals(safeParseColor("#00FF00", fallback), safeParseColor("00FF00", fallback))
    }
}
