package ua.constitution.domain.text

import ua.constitution.utils.Constants

/**
 * Pure highlight / underline / eraser styling engine for the reader.
 *
 * Rasterizes the existing styled ranges and the active tool's target ranges onto per-character
 * arrays, fills same-color gaps between adjacent styled runs (only across non-letter/digit
 * characters), then coalesces back into trimmed [StyledRange]s and merges adjacent ones.
 *
 * Extracted verbatim from the SegmentedTextWithEdits composable so this algorithm — and its
 * pinned quirks (same-color gap fill blocked by an intervening letter/digit, whitespace-only
 * ranges dropped, case-insensitive coalescing that keeps the first run's color casing) — is
 * unit-testable without Compose. Decomposed into small phase helpers to keep each well under the
 * cyclomatic-complexity budget; behavior is unchanged (pinned by RangeStylerTest).
 */
object RangeStyler {

    /**
     * Applies [tool] (marker / underline / eraser) at [colorHex] over [wordRanges] on top of
     * [existingRanges], returning the resulting merged styled ranges for [originalText].
     */
    fun applyStyle(
        originalText: String,
        existingRanges: List<StyledRange>,
        wordRanges: Collection<Pair<Int, Int>>,
        tool: String,
        colorHex: String
    ): List<StyledRange> {
        val buffer = CharStyleBuffer(originalText.length)
        buffer.applyRanges(existingRanges)
        buffer.applyTool(wordRanges, tool, colorHex)
        fillColorGaps(buffer.highlight, buffer.highlightColor, originalText)
        fillColorGaps(buffer.underscore, buffer.underscoreColor, originalText)
        return mergeAdjacentStyledRanges(originalText, coalesce(buffer, originalText))
    }

    /** Per-character highlight/underscore flags and colors, rasterized from ranges and the tool. */
    private class CharStyleBuffer(val len: Int) {
        val highlight = BooleanArray(len)
        val highlightColor = Array(len) { "#FFFFFF" }
        val underscore = BooleanArray(len)
        val underscoreColor = Array(len) { "#FFFFFF" }

        fun applyRanges(ranges: List<StyledRange>) {
            ranges.forEach { r ->
                for (i in r.start until r.end) {
                    if (i !in 0 until len) continue
                    if (r.highlight) { highlight[i] = true; highlightColor[i] = r.highlightColorHex }
                    if (r.underscore) { underscore[i] = true; underscoreColor[i] = r.underscoreColorHex }
                }
            }
        }

        fun applyTool(wordRanges: Collection<Pair<Int, Int>>, tool: String, colorHex: String) {
            wordRanges.forEach { (start, end) ->
                for (i in start until end) {
                    if (i in 0 until len) applyToolAt(i, tool, colorHex)
                }
            }
        }

        private fun applyToolAt(i: Int, tool: String, colorHex: String) {
            when (tool) {
                Constants.TOOL_MARKER -> { highlight[i] = true; highlightColor[i] = colorHex }
                Constants.TOOL_UNDERLINE -> { underscore[i] = true; underscoreColor[i] = colorHex }
                Constants.TOOL_ERASER -> { highlight[i] = false; underscore[i] = false }
            }
        }
    }

    /** Fills same-color gaps between adjacent styled runs, but only across non-letter/digit chars. */
    private fun fillColorGaps(flags: BooleanArray, colors: Array<String>, text: String) {
        var lastIdx = -1
        for (i in text.indices) {
            if (!flags[i]) continue
            if (lastIdx != -1 && lastIdx < i - 1 &&
                colors[lastIdx].lowercase() == colors[i].lowercase() &&
                isOnlyGapChars(text, lastIdx + 1, i)
            ) {
                for (j in (lastIdx + 1) until i) {
                    flags[j] = true
                    colors[j] = colors[lastIdx]
                }
            }
            lastIdx = i
        }
    }

    private fun isOnlyGapChars(text: String, from: Int, to: Int): Boolean {
        for (j in from until to) {
            if (text[j].isLetterOrDigit()) return false
        }
        return true
    }

    /** A maximal run of identically-styled characters (case-insensitive on colors). */
    private data class RunStyle(
        val highlight: Boolean,
        val highlightColor: String,
        val underscore: Boolean,
        val underscoreColor: String
    ) {
        fun hasStyle(): Boolean = highlight || underscore
        fun matches(other: RunStyle): Boolean =
            highlight == other.highlight &&
                highlightColor.lowercase() == other.highlightColor.lowercase() &&
                underscore == other.underscore &&
                underscoreColor.lowercase() == other.underscoreColor.lowercase()
    }

    /** Coalesces the per-character buffer into trimmed [StyledRange]s. */
    private fun coalesce(buffer: CharStyleBuffer, text: String): List<StyledRange> {
        val out = mutableListOf<StyledRange>()
        var startIdx = -1
        var current: RunStyle? = null
        for (i in text.indices) {
            val cell = RunStyle(buffer.highlight[i], buffer.highlightColor[i], buffer.underscore[i], buffer.underscoreColor[i])
            if (cell.hasStyle()) {
                if (current == null) {
                    startIdx = i; current = cell
                } else if (!current.matches(cell)) {
                    out.addTrimmedRange(text, startIdx, i, current); startIdx = i; current = cell
                }
            } else if (current != null) {
                out.addTrimmedRange(text, startIdx, i, current); startIdx = -1; current = null
            }
        }
        current?.let { out.addTrimmedRange(text, startIdx, text.length, it) }
        return out
    }

    /** Appends a [StyledRange] for [start, end) unless it is empty or contains only whitespace. */
    private fun MutableList<StyledRange>.addTrimmedRange(text: String, start: Int, end: Int, style: RunStyle) {
        if (start >= end) return
        if (text.substring(start, end).all { it.isWhitespace() }) return
        add(
            StyledRange(
                start = start,
                end = end,
                colorHex = if (style.highlight) style.highlightColor else style.underscoreColor,
                highlight = style.highlight,
                underscore = style.underscore,
                highlightColorHex = style.highlightColor,
                underscoreColorHex = style.underscoreColor
            )
        )
    }
}
