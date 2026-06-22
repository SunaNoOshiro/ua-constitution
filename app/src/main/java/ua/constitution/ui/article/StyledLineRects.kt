package ua.constitution.ui.article

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ua.constitution.domain.text.StyledRange
import ua.constitution.ui.safeParseColor

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

/**
 * Draws the highlight/underline overlay for [ranges] over a laid-out text. [getLayout] is invoked
 * inside the draw phase so the latest [TextLayoutResult] is used. Highlights fill behind the text;
 * underlines are drawn on top after `drawContent()`. Both passes swallow exceptions to avoid
 * crashing the Compose drawing thread. Extracted verbatim from `SegmentedTextWithEdits`; shared by
 * its editable and read-only render paths.
 */
fun Modifier.styledOverlay(
    getLayout: () -> TextLayoutResult?,
    ranges: List<StyledRange>,
    originalTextLength: Int,
    origToFormMapping: IntArray,
): Modifier = drawWithContent {
    try {
        getLayout()?.let { drawHighlightRects(it, ranges, originalTextLength, origToFormMapping) }
    } catch (e: Exception) {
        // ignore drawing errors to avoid crashing the Compose drawing thread
    }
    drawContent()
    try {
        getLayout()?.let { drawUnderlineRects(it, ranges, originalTextLength, origToFormMapping) }
    } catch (e: Exception) {
        // ignore drawing errors to avoid crashing the Compose drawing thread
    }
}

private fun DrawScope.drawHighlightRects(
    layout: TextLayoutResult,
    ranges: List<StyledRange>,
    originalTextLength: Int,
    origToFormMapping: IntArray,
) {
    ranges.forEach { range ->
        if (!range.highlight) return@forEach
        val color = safeParseColor(range.highlightColorHex, Color.Yellow).copy(alpha = 0.85f)
        styledLineRects(layout, range.start, range.end, originalTextLength, origToFormMapping).forEach { rect ->
            drawRect(
                color = color,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
            )
        }
    }
}

private fun DrawScope.drawUnderlineRects(
    layout: TextLayoutResult,
    ranges: List<StyledRange>,
    originalTextLength: Int,
    origToFormMapping: IntArray,
) {
    ranges.forEach { range ->
        if (!range.underscore) return@forEach
        val color = safeParseColor(range.underscoreColorHex, Color.Red)
        styledLineRects(layout, range.start, range.end, originalTextLength, origToFormMapping).forEach { rect ->
            val lineY = rect.bottom - 2.dp.toPx()
            drawLine(
                color = color,
                start = Offset(rect.left, lineY),
                end = Offset(rect.right, lineY),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}
