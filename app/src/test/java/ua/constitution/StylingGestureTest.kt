package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.constitution.domain.text.GestureAxis
import ua.constitution.domain.text.resolveGestureAxis

/** Pins the scroll-vs-style swipe decision extracted from SegmentedTextWithEdits' pointer loop. */
class StylingGestureTest {

    private val slop = 10f

    @Test
    fun below_slop_is_undecided() {
        assertNull(resolveGestureAxis(diffX = 3f, diffY = 3f, touchSlop = slop))
        assertNull(resolveGestureAxis(diffX = 0f, diffY = 0f, touchSlop = slop))
    }

    @Test
    fun vertical_dominant_move_past_slop_is_scroll() {
        assertEquals(GestureAxis.SCROLL, resolveGestureAxis(diffX = 2f, diffY = 40f, touchSlop = slop))
    }

    @Test
    fun horizontal_dominant_move_past_slop_is_style() {
        assertEquals(GestureAxis.STYLE, resolveGestureAxis(diffX = 40f, diffY = 2f, touchSlop = slop))
    }

    @Test
    fun equal_diagonal_past_slop_is_style() {
        // |dy| > |dx| is the scroll condition; equal magnitudes fall through to STYLE (verbatim).
        assertEquals(GestureAxis.STYLE, resolveGestureAxis(diffX = 30f, diffY = 30f, touchSlop = slop))
    }
}
