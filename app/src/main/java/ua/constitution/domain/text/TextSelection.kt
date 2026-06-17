package ua.constitution.domain.text

/** Pure text-selection model and word-snapping / range-merging logic for the reader's
 * highlight & underline tools. Extracted from MainActivity. */

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

    var start = offset
    while (start > 0 && !text[start - 1].isWhitespace()) {
        start--
    }

    var end = offset
    while (end < text.length && !text[end].isWhitespace()) {
        end++
    }

    // Trim leading punctuation
    while (start < end && !text[start].isLetterOrDigit()) {
        start++
    }

    // Trim trailing punctuation
    while (end > start && !text[end - 1].isLetterOrDigit()) {
        end--
    }

    return if (start < end) Pair(start, end) else null
}

fun getWordSnappedRange(text: String, offset1: Int, offset2: Int): Pair<Int, Int>? {
    val o1 = offset1.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val o2 = offset2.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val minO = minOf(o1, o2)
    val maxO = maxOf(o1, o2)

    var finalStart = minO
    val startWord = getWordRangeAtOffset(text, minO)
    if (startWord != null) {
        finalStart = startWord.first
    } else {
        var found = false
        for (i in minO until text.length) {
            val word = getWordRangeAtOffset(text, i)
            if (word != null) {
                finalStart = word.first
                found = true
                break
            }
        }
        if (!found) {
            for (i in minO downTo 0) {
                val word = getWordRangeAtOffset(text, i)
                if (word != null) {
                    finalStart = word.first
                    break
                }
            }
        }
    }

    var finalEnd = maxO
    val endWord = getWordRangeAtOffset(text, maxO)
    if (endWord != null) {
        finalEnd = endWord.second
    } else {
        var found = false
        for (i in maxO downTo 0) {
            val word = getWordRangeAtOffset(text, i)
            if (word != null) {
                finalEnd = word.second
                found = true
                break
            }
        }
        if (!found) {
            for (i in maxO until text.length) {
                val word = getWordRangeAtOffset(text, i)
                if (word != null) {
                    finalEnd = word.second
                    break
                }
            }
        }
    }

    if (finalStart < finalEnd) {
        return Pair(finalStart, finalEnd)
    }
    return null
}

fun mergeAdjacentStyledRanges(text: String, ranges: List<StyledRange>): List<StyledRange> {
    if (ranges.size <= 1) return ranges

    val sorted = ranges.sortedBy { it.start }
    val result = mutableListOf<StyledRange>()

    for (range in sorted) {
        if (result.isEmpty()) {
            result.add(range)
        } else {
            val last = result.last()
            val sameSpec = last.highlight == range.highlight &&
                           last.underscore == range.underscore &&
                           last.highlightColorHex.lowercase() == range.highlightColorHex.lowercase() &&
                           last.underscoreColorHex.lowercase() == range.underscoreColorHex.lowercase() &&
                           last.colorHex.lowercase() == range.colorHex.lowercase()

            if (sameSpec) {
                val canMerge = if (range.start <= last.end) {
                    true
                } else {
                    val intermediateText = text.substring(last.end, range.start)
                    intermediateText.all { it.isWhitespace() || !it.isLetterOrDigit() }
                }

                if (canMerge) {
                    result[result.size - 1] = last.copy(
                        start = last.start,
                        end = maxOf(last.end, range.end)
                    )
                } else {
                    result.add(range)
                }
            } else {
                result.add(range)
            }
        }
    }
    return result
}
