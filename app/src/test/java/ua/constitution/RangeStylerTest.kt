package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.domain.text.RangeStyler
import ua.constitution.domain.text.StyledRange
import ua.constitution.utils.Constants

/**
 * Pins the highlight/underline/eraser styling engine extracted from SegmentedTextWithEdits in G2.
 * Its body was previously untestable (a remember{} lambda inside a composable); these characterize
 * the gap-fill, whitespace-trim and case-insensitive coalescing quirks so the extraction is faithful.
 */
class RangeStylerTest {

    private fun highlight(start: Int, end: Int, color: String) =
        StyledRange(start, end, color, highlight = true, underscore = false)

    @Test
    fun `marker over a word produces one highlight range`() {
        val out = RangeStyler.applyStyle("ab cd", emptyList(), listOf(0 to 2), Constants.TOOL_MARKER, "#FFF59D")
        assertEquals(1, out.size)
        assertEquals(0 to 2, out[0].start to out[0].end)
        assertTrue(out[0].highlight)
        assertEquals("#FFF59D", out[0].colorHex)
    }

    @Test
    fun `underline produces an underscore range carrying the chosen color`() {
        val out = RangeStyler.applyStyle("ab", emptyList(), listOf(0 to 2), Constants.TOOL_UNDERLINE, "#F57F17")
        assertEquals(1, out.size)
        assertTrue(out[0].underscore)
        assertEquals("#F57F17", out[0].colorHex)
    }

    @Test
    fun `same-color gap of only punctuation between two marked words is filled into one range`() {
        val out = RangeStyler.applyStyle("ab cd", emptyList(), listOf(0 to 2, 3 to 5), Constants.TOOL_MARKER, "#FFF59D")
        assertEquals(1, out.size)
        assertEquals(0 to 5, out[0].start to out[0].end)
    }

    @Test
    fun `a gap containing a letter or digit blocks the fill, leaving two ranges`() {
        val out = RangeStyler.applyStyle("a1b", emptyList(), listOf(0 to 1, 2 to 3), Constants.TOOL_MARKER, "#FFF59D")
        assertEquals(listOf(0 to 1, 2 to 3), out.map { it.start to it.end })
    }

    @Test
    fun `a whitespace-only run is dropped`() {
        val out = RangeStyler.applyStyle("   ", emptyList(), listOf(0 to 3), Constants.TOOL_MARKER, "#FFF59D")
        assertTrue(out.isEmpty())
    }

    @Test
    fun `eraser clears an existing highlight`() {
        val existing = listOf(highlight(0, 2, "#FFF59D"))
        val out = RangeStyler.applyStyle("ab", existing, listOf(0 to 2), Constants.TOOL_ERASER, "#FFF59D")
        assertTrue(out.isEmpty())
    }

    @Test
    fun `adjacent same-color marks coalesce case-insensitively, keeping the first run's casing`() {
        val existing = listOf(highlight(0, 2, "#ffd700"))
        val out = RangeStyler.applyStyle("abcd", existing, listOf(2 to 4), Constants.TOOL_MARKER, "#FFD700")
        assertEquals(1, out.size)
        assertEquals(0 to 4, out[0].start to out[0].end)
        assertEquals("#ffd700", out[0].colorHex)
    }
}
