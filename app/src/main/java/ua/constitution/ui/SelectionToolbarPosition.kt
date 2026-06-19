package ua.constitution.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/**
 * Pure placement math for the selection-toolbar popup: centers it horizontally over the selection
 * rect, prefers placing it above and flips below when it would clip the top [bufferPx], then clamps
 * into the window with an 8px margin. NaN/Infinite selection-rect edges are treated as 0.
 *
 * Pixel distances ([gapPx], [bufferPx]) are passed in so the function is density-free and
 * unit-testable. Extracted verbatim from the PopupPositionProvider in SegmentedTextWithEdits.
 */
fun selectionToolbarOffset(
    selectionRect: Rect,
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    gapPx: Float,
    bufferPx: Float
): IntOffset {
    val safeLeft = sanitizeEdge(selectionRect.left)
    val safeRight = sanitizeEdge(selectionRect.right)
    val safeTop = sanitizeEdge(selectionRect.top)
    val safeBottom = sanitizeEdge(selectionRect.bottom)

    val x = anchorBounds.left + (safeLeft + safeRight) / 2 - popupContentSize.width / 2
    var y = anchorBounds.top + safeTop - popupContentSize.height - gapPx

    if (y < bufferPx) {
        y = anchorBounds.top + safeBottom + gapPx
    }

    val maxX = (windowSize.width - popupContentSize.width - 8).toFloat()
    val finalX = if (8f >= maxX) 8f else x.coerceIn(8f, maxX)

    val maxY = (windowSize.height - popupContentSize.height - 8).toFloat()
    val finalY = if (8f >= maxY) 8f else y.coerceIn(8f, maxY)

    return IntOffset(finalX.toInt(), finalY.toInt())
}

/** Treats a NaN/Infinite selection-rect edge as 0 (otherwise returns the value unchanged). */
private fun sanitizeEdge(value: Float): Float = if (value.isNaN() || value.isInfinite()) 0f else value
