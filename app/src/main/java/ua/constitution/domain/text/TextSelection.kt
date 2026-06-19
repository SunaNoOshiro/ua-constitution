package ua.constitution.domain.text

/** Pure text-selection model and word-snapping / range-merging logic for the reader's
 * highlight & underline tools. Extracted from MainActivity. Decomposed into small helpers so each
 * function stays within the cyclomatic-complexity budget; behavior is verbatim, pinned by
 * TextSelectionLogicTest. */

data class StyledRange(
    val start: Int,
    val end: Int,
    val colorHex: String,
    val highlight: Boolean,
    val underscore: Boolean,
    val highlightColorHex: String = colorHex,
    val underscoreColorHex: String = colorHex
)

fun getWordRangeAtOffset(text: String, offset: Int): Pair<Int, Int>? {
    if (offset < 0 || offset >= text.length) return null
    if (text[offset].isWhitespace()) return null

    val (start, end) = trimToLettersOrDigits(text, wordStart(text, offset), wordEnd(text, offset))
    return if (start < end) Pair(start, end) else null
}

/** Walks left from [offset] to the start of the non-whitespace run. */
private fun wordStart(text: String, offset: Int): Int {
    var start = offset
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    return start
}

/** Walks right from [offset] to the end of the non-whitespace run. */
private fun wordEnd(text: String, offset: Int): Int {
    var end = offset
    while (end < text.length && !text[end].isWhitespace()) end++
    return end
}

/** Trims leading/trailing non-letter/digit characters from [start, end). */
private fun trimToLettersOrDigits(text: String, start: Int, end: Int): Pair<Int, Int> {
    var s = start
    var e = end
    while (s < e && !text[s].isLetterOrDigit()) s++
    while (e > s && !text[e - 1].isLetterOrDigit()) e--
    return s to e
}

fun getWordSnappedRange(text: String, offset1: Int, offset2: Int): Pair<Int, Int>? {
    val o1 = offset1.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val o2 = offset2.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val finalStart = snapStart(text, minOf(o1, o2))
    val finalEnd = snapEnd(text, maxOf(o1, o2))
    return if (finalStart < finalEnd) Pair(finalStart, finalEnd) else null
}

/** The start of the word at [minO], else the nearest word scanning forward then backward, else [minO]. */
private fun snapStart(text: String, minO: Int): Int {
    getWordRangeAtOffset(text, minO)?.let { return it.first }
    return firstWordStart(text, minO until text.length)
        ?: firstWordStart(text, minO downTo 0)
        ?: minO
}

/** The end of the word at [maxO], else the nearest word scanning backward then forward, else [maxO]. */
private fun snapEnd(text: String, maxO: Int): Int {
    getWordRangeAtOffset(text, maxO)?.let { return it.second }
    return firstWordEnd(text, maxO downTo 0)
        ?: firstWordEnd(text, maxO until text.length)
        ?: maxO
}

private fun firstWordStart(text: String, indices: IntProgression): Int? {
    for (i in indices) getWordRangeAtOffset(text, i)?.let { return it.first }
    return null
}

private fun firstWordEnd(text: String, indices: IntProgression): Int? {
    for (i in indices) getWordRangeAtOffset(text, i)?.let { return it.second }
    return null
}

fun mergeAdjacentStyledRanges(text: String, ranges: List<StyledRange>): List<StyledRange> {
    if (ranges.size <= 1) return ranges

    val result = mutableListOf<StyledRange>()
    for (range in ranges.sortedBy { it.start }) {
        val last = result.lastOrNull()
        if (last != null && sameStyleSpec(last, range) && canMerge(text, last, range)) {
            result[result.size - 1] = last.copy(start = last.start, end = maxOf(last.end, range.end))
        } else {
            result.add(range)
        }
    }
    return result
}

/** True when two ranges carry the same highlight/underscore styling (case-insensitive on colors). */
private fun sameStyleSpec(a: StyledRange, b: StyledRange): Boolean =
    a.highlight == b.highlight &&
        a.underscore == b.underscore &&
        a.highlightColorHex.lowercase() == b.highlightColorHex.lowercase() &&
        a.underscoreColorHex.lowercase() == b.underscoreColorHex.lowercase() &&
        a.colorHex.lowercase() == b.colorHex.lowercase()

/** True when [range] abuts [last] or the gap between them holds only whitespace/punctuation. */
private fun canMerge(text: String, last: StyledRange, range: StyledRange): Boolean {
    if (range.start <= last.end) return true
    return text.substring(last.end, range.start).all { it.isWhitespace() || !it.isLetterOrDigit() }
}
