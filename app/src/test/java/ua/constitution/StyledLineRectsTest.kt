package ua.constitution

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ua.constitution.ui.article.styledLineRects

/**
 * Pins the per-line geometry helper extracted from SegmentedTextWithEdits.drawStyledOverlay. Uses a
 * real laid-out BasicText so the layout queries (getLineForOffset/getLineStart/...) are exercised,
 * and asserts the structural decisions that carry the complexity: the empty-range guard, offset
 * clamping, and one-rect-per-spanned-line. Exact pixel widths are not asserted (they depend on the
 * font/measurement and are not part of the contract).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class StyledLineRectsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun layoutOf(text: String): TextLayoutResult {
        lateinit var captured: TextLayoutResult
        composeTestRule.setContent {
            BasicText(text = text, onTextLayout = { captured = it })
        }
        composeTestRule.waitForIdle()
        return captured
    }

    private fun identityMapping(len: Int) = IntArray(len + 1) { it }

    @Test
    fun empty_mapped_range_yields_no_rects() {
        val text = "Hello world"
        val layout = layoutOf(text)
        val rects = styledLineRects(layout, 3, 3, text.length, identityMapping(text.length))
        assertTrue("a collapsed range covers nothing", rects.isEmpty())
    }

    @Test
    fun single_line_range_yields_one_rect_with_sane_bounds() {
        val text = "Hello"
        val layout = layoutOf(text)
        val rects = styledLineRects(layout, 0, text.length, text.length, identityMapping(text.length))
        assertEquals("short text lays out on one line -> one rect", 1, rects.size)
        val r = rects[0]
        assertTrue("left must not exceed right", r.left <= r.right)
        assertTrue("top must not exceed bottom", r.top <= r.bottom)
    }

    @Test
    fun out_of_range_offsets_are_clamped_without_crashing() {
        val text = "Hello"
        val layout = layoutOf(text)
        val rects = styledLineRects(layout, -5, 999, text.length, identityMapping(text.length))
        assertTrue("clamped range still covers the visible text", rects.isNotEmpty())
    }
}
