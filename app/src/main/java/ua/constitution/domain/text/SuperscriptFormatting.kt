package ua.constitution.domain.text

/** Pure helpers for rendering article numbers with unicode superscripts and mapping indices
 * between the original ("16.1") and formatted ("16¹") forms. Extracted from MainActivity. */

private const val SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"

/** The unicode superscript form of a digit char ('0'..'9'); any other char is returned unchanged.
 *  Single source of truth for the digit→superscript mapping (replaces the duplicated 10-arm `when`
 *  blocks that previously inflated this file's and ArticleNumberFormatter's complexity). */
fun digitToSuperscript(c: Char): Char = if (c in '0'..'9') SUPERSCRIPT_DIGITS[c - '0'] else c

fun isSuperscriptEquivalent(normal: Char, superChar: Char): Boolean =
    normal in '0'..'9' && digitToSuperscript(normal) == superChar

/** True when an original char equals the formatted char or is its superscript equivalent. */
private fun charsMatch(origChar: Char, formChar: Char): Boolean =
    origChar == formChar || isSuperscriptEquivalent(origChar, formChar)

/** The separators ('.' / '-') dropped when forming the superscript ("16.1" -> "16¹"). */
private fun isGapChar(c: Char): Boolean = c == '.' || c == '-'

fun mapOriginalToFormatted(original: String, formatted: String): IntArray {
    val origToForm = IntArray(original.length + 1) { formatted.length }
    var formIdx = 0
    for (origIdx in 0..original.length) {
        if (origIdx == original.length) {
            origToForm[origIdx] = formatted.length
            break
        }
        val origChar = original[origIdx]
        if (formIdx < formatted.length) {
            val formChar = formatted[formIdx]
            if (charsMatch(origChar, formChar)) {
                origToForm[origIdx] = formIdx
                formIdx++
            } else if (isGapChar(origChar)) {
                origToForm[origIdx] = formIdx
            } else {
                origToForm[origIdx] = formIdx
                formIdx++
            }
        } else {
            origToForm[origIdx] = formatted.length
        }
    }
    return origToForm
}

fun mapFormattedToOriginal(original: String, formatted: String): IntArray {
    val formToOrig = IntArray(formatted.length + 1) { original.length }
    var origIdx = 0
    for (formIdx in 0..formatted.length) {
        if (formIdx == formatted.length) {
            formToOrig[formIdx] = original.length
            break
        }
        val (assignedIdx, nextOrigIdx) = resolveOriginalIndex(original, origIdx, formatted[formIdx])
        origIdx = nextOrigIdx
        formToOrig[formIdx] = assignedIdx ?: original.length
    }
    return formToOrig
}

/**
 * Advances past gap characters from [startOrigIdx] to the original-string index that maps to
 * [formChar] (a match, or the first non-gap char), returning that index plus the next scan position.
 * Returns a null index when the original is exhausted without an assignment. Extracted verbatim from
 * the former inline while-loop in [mapFormattedToOriginal] (charsMatch OR not-a-gap both assign).
 */
private fun resolveOriginalIndex(original: String, startOrigIdx: Int, formChar: Char): Pair<Int?, Int> {
    var origIdx = startOrigIdx
    while (origIdx < original.length) {
        val origChar = original[origIdx]
        if (charsMatch(origChar, formChar) || !isGapChar(origChar)) {
            return origIdx to (origIdx + 1)
        }
        origIdx++
    }
    return null to origIdx
}

fun formatStringToSuperscript(input: String): String {
    var result = input
    val regexDots = """(\d+)\.(\d+)""".toRegex()
    result = regexDots.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { digitToSuperscript(it) }.joinToString("")
        "$base$sup"
    }
    val regexHyphens = """(\d+)-(\d+)""".toRegex()
    result = regexHyphens.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { digitToSuperscript(it) }.joinToString("")
        "$base$sup"
    }
    return result
}
