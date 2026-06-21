package ua.constitution.ui.article

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult

/**
 * Computes the per-line rectangles a styled range [rangeStart, rangeEnd) (in ORIGINAL-text
 * coordinates) covers within a laid-out [layoutResult] (FORMATTED-text coordinates).
 *
 * Extracted verbatim from the two near-identical geometry loops that lived inside
 * SegmentedTextWithEdits.drawStyledOverlay (one for the highlight fill, one for the underline).
 * Both render paths now iterate these rectangles: the highlight fills each rect, the underline
 * draws along each rect's bottom edge. Each Rect has left = min(left, right) and right = max(...),
 * so callers don't repeat the direction-normalisation the originals did inline.
 *
 * Returns an empty list when the mapped range is empty (start >= end), exactly as the former
 * `if (mappedSelectStart < mappedSelectEnd)` guard did.
 */
fun styledLineRects(
    layoutResult: TextLayoutResult,
    rangeStart: Int,
    rangeEnd: Int,
    originalTextLength: Int,
    origToFormMapping: IntArray,
): List<Rect> {
    val safeStart = rangeStart.coerceIn(0, originalTextLength)
    val safeEnd = rangeEnd.coerceIn(0, originalTextLength)
    val rawMappedStart = origToFormMapping.getOrElse(safeStart) { safeStart }
    val rawMappedEnd = origToFormMapping.getOrElse(safeEnd) { safeEnd }
    val textLen = layoutResult.layoutInput.text.length
    val mappedStart = rawMappedStart.coerceIn(0, textLen)
    val mappedEnd = rawMappedEnd.coerceIn(0, textLen)
    if (mappedStart >= mappedEnd) return emptyList()

    val rects = mutableListOf<Rect>()
    val startLine = layoutResult.getLineForOffset(mappedStart)
    val endLine = layoutResult.getLineForOffset(maxOf(0, mappedEnd - 1))
    for (line in startLine..endLine) {
        val lineStart = layoutResult.getLineStart(line)
        val lineEnd = layoutResult.getLineEnd(line)
        val segmentStart = maxOf(mappedStart, lineStart)
        val segmentEnd = minOf(mappedEnd, lineEnd)
        if (segmentStart < segmentEnd) {
            val left = if (segmentStart == lineStart) {
                layoutResult.getLineLeft(line)
            } else {
                layoutResult.getHorizontalPosition(segmentStart, usePrimaryDirection = true)
            }
            val right = if (segmentEnd == lineEnd) {
                layoutResult.getLineRight(line)
            } else {
                layoutResult.getHorizontalPosition(segmentEnd, usePrimaryDirection = true)
            }
            rects.add(
                Rect(
                    left = minOf(left, right),
                    top = layoutResult.getLineTop(line),
                    right = maxOf(left, right),
                    bottom = layoutResult.getLineBottom(line),
                )
            )
        }
    }
    return rects
}
