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
 * unit-testable without Compose.
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
        val len = originalText.length
        val charHighlight = BooleanArray(len)
        val charHighlightColor = Array(len) { "#FFFFFF" }
        val charUnderscore = BooleanArray(len)
        val charUnderscoreColor = Array(len) { "#FFFFFF" }

        // Populate arrays with existing ranges
        existingRanges.forEach { r ->
            for (i in r.start until r.end) {
                if (i in 0 until len) {
                    if (r.highlight) {
                        charHighlight[i] = true
                        charHighlightColor[i] = r.highlightColorHex
                    }
                    if (r.underscore) {
                        charUnderscore[i] = true
                        charUnderscoreColor[i] = r.underscoreColorHex
                    }
                }
            }
        }

        // Modify all specified ranges together!
        wordRanges.forEach { (start, end) ->
            for (i in start until end) {
                if (i in 0 until len) {
                    when (tool) {
                        Constants.TOOL_MARKER -> {
                            charHighlight[i] = true
                            charHighlightColor[i] = colorHex
                        }
                        Constants.TOOL_UNDERLINE -> {
                            charUnderscore[i] = true
                            charUnderscoreColor[i] = colorHex
                        }
                        Constants.TOOL_ERASER -> {
                            charHighlight[i] = false
                            charUnderscore[i] = false
                        }
                    }
                }
            }
        }

        // Post-process to fill styling gaps (spaces/punctuation between adjacent words styled with identical colors)
        var lastHighlightIdx = -1
        for (i in 0 until len) {
            if (charHighlight[i]) {
                if (lastHighlightIdx != -1 && lastHighlightIdx < i - 1) {
                    val color1 = charHighlightColor[lastHighlightIdx].lowercase()
                    val color2 = charHighlightColor[i].lowercase()
                    if (color1 == color2) {
                        var onlyGapChars = true
                        for (j in (lastHighlightIdx + 1) until i) {
                            val char = originalText[j]
                            if (char.isLetterOrDigit()) {
                                onlyGapChars = false
                                break
                            }
                        }
                        if (onlyGapChars) {
                            for (j in (lastHighlightIdx + 1) until i) {
                                charHighlight[j] = true
                                charHighlightColor[j] = charHighlightColor[lastHighlightIdx]
                            }
                        }
                    }
                }
                lastHighlightIdx = i
            }
        }

        var lastUnderscoreIdx = -1
        for (i in 0 until len) {
            if (charUnderscore[i]) {
                if (lastUnderscoreIdx != -1 && lastUnderscoreIdx < i - 1) {
                    val color1 = charUnderscoreColor[lastUnderscoreIdx].lowercase()
                    val color2 = charUnderscoreColor[i].lowercase()
                    if (color1 == color2) {
                        var onlyGapChars = true
                        for (j in (lastUnderscoreIdx + 1) until i) {
                            val char = originalText[j]
                            if (char.isLetterOrDigit()) {
                                onlyGapChars = false
                                break
                            }
                        }
                        if (onlyGapChars) {
                            for (j in (lastUnderscoreIdx + 1) until i) {
                                charUnderscore[j] = true
                                charUnderscoreColor[j] = charUnderscoreColor[lastUnderscoreIdx]
                            }
                        }
                    }
                }
                lastUnderscoreIdx = i
            }
        }

        // Convert back to structured StyledRanges with trimmer
        val newRanges = mutableListOf<StyledRange>()
        fun addRangeWithCheck(s: Int, e: Int, highlight: Boolean, hc: String, underscore: Boolean, uc: String) {
            if (s < e) {
                val sub = originalText.substring(s, e)
                if (sub.any { !it.isWhitespace() }) {
                    newRanges.add(
                        StyledRange(
                            start = s,
                            end = e,
                            colorHex = if (highlight) hc else uc,
                            highlight = highlight,
                            underscore = underscore,
                            highlightColorHex = hc,
                            underscoreColorHex = uc
                        )
                    )
                }
            }
        }

        var currentStart = -1
        var currentHighlight = false
        var currentHighlightColor = "#FFFFFF"
        var currentUnderscore = false
        var currentUnderscoreColor = "#FFFFFF"

        for (i in 0 until len) {
            val h = charHighlight[i]
            val hc = charHighlightColor[i]
            val u = charUnderscore[i]
            val uc = charUnderscoreColor[i]

            val hasStyle = h || u
            val matchesCurrent = currentStart != -1 &&
                                 currentHighlight == h &&
                                 currentHighlightColor.lowercase() == hc.lowercase() &&
                                 currentUnderscore == u &&
                                 currentUnderscoreColor.lowercase() == uc.lowercase()

            if (hasStyle) {
                if (currentStart == -1) {
                    currentStart = i
                    currentHighlight = h
                    currentHighlightColor = hc
                    currentUnderscore = u
                    currentUnderscoreColor = uc
                } else if (!matchesCurrent) {
                    addRangeWithCheck(
                        currentStart,
                        i,
                        currentHighlight,
                        currentHighlightColor,
                        currentUnderscore,
                        currentUnderscoreColor
                    )
                    currentStart = i
                    currentHighlight = h
                    currentHighlightColor = hc
                    currentUnderscore = u
                    currentUnderscoreColor = uc
                }
            } else {
                if (currentStart != -1) {
                    addRangeWithCheck(
                        currentStart,
                        i,
                        currentHighlight,
                        currentHighlightColor,
                        currentUnderscore,
                        currentUnderscoreColor
                    )
                    currentStart = -1
                }
            }
        }
        if (currentStart != -1) {
            addRangeWithCheck(
                currentStart,
                len,
                currentHighlight,
                currentHighlightColor,
                currentUnderscore,
                currentUnderscoreColor
            )
        }
        return mergeAdjacentStyledRanges(originalText, newRanges)
    }
}
