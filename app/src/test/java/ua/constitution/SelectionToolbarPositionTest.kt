package ua.constitution

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.ui.selectionToolbarOffset

/** Pins the pure selection-toolbar placement math extracted from SegmentedTextWithEdits in J2. */
class SelectionToolbarPositionTest {

    private val anchor = IntRect(0, 0, 1000, 2000)
    private val popup = IntSize(80, 40)
    private val window = IntSize(1080, 2000)

    @Test
    fun `centers horizontally and places above the selection when there is room`() {
        val out = selectionToolbarOffset(
            selectionRect = Rect(100f, 500f, 200f, 540f),
            anchorBounds = anchor, windowSize = window, popupContentSize = popup,
            gapPx = 8f, bufferPx = 60f
        )
        // x = (100+200)/2 - 80/2 = 110 ; y = 500 - 40 - 8 = 452
        assertEquals(IntOffset(110, 452), out)
    }

    @Test
    fun `flips below the selection when placing above would clip the top buffer`() {
        val out = selectionToolbarOffset(
            selectionRect = Rect(100f, 5f, 200f, 45f),
            anchorBounds = anchor, windowSize = window, popupContentSize = popup,
            gapPx = 8f, bufferPx = 60f
        )
        // above y = 5 - 40 - 8 = -43 < 60 -> flip: y = bottom(45) + 8 = 53
        assertEquals(IntOffset(110, 53), out)
    }

    @Test
    fun `clamps x within the window margin when the popup overflows the right edge`() {
        val out = selectionToolbarOffset(
            selectionRect = Rect(1000f, 500f, 1000f, 540f),
            anchorBounds = anchor, windowSize = IntSize(1000, 2000), popupContentSize = popup,
            gapPx = 8f, bufferPx = 60f
        )
        // x = 1000 - 40 = 960 ; maxX = 1000 - 80 - 8 = 912 -> clamped to 912
        assertEquals(912, out.x)
    }

    @Test
    fun `falls back to the 8px margin when the window is smaller than the popup`() {
        val out = selectionToolbarOffset(
            selectionRect = Rect(100f, 500f, 200f, 540f),
            anchorBounds = anchor, windowSize = IntSize(50, 50), popupContentSize = popup,
            gapPx = 8f, bufferPx = 60f
        )
        assertEquals(IntOffset(8, 8), out)
    }

    @Test
    fun `treats NaN and Infinite selection edges as zero without crashing`() {
        val out = selectionToolbarOffset(
            selectionRect = Rect(Float.NaN, Float.NaN, Float.POSITIVE_INFINITY, Float.NaN),
            anchorBounds = anchor, windowSize = window, popupContentSize = popup,
            gapPx = 8f, bufferPx = 60f
        )
        // all edges -> 0 ; x = -40 -> clamp 8 ; above y = -48 < 60 -> flip y = 0 + 8 = 8
        assertEquals(IntOffset(8, 8), out)
    }
}
