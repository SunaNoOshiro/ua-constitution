package ua.constitution.domain.text

/** Pure helpers for rendering article numbers with unicode superscripts and mapping indices
 * between the original ("16.1") and formatted ("16¹") forms. Extracted from MainActivity. */

fun isSuperscriptEquivalent(normal: Char, superChar: Char): Boolean {
    val map = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
    )
    return map[normal] == superChar
}

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
            if (origChar == formChar || isSuperscriptEquivalent(origChar, formChar)) {
                origToForm[origIdx] = formIdx
                formIdx++
            } else if (origChar == '.' || origChar == '-') {
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
        val formChar = formatted[formIdx]
        var assigned = false
        while (origIdx < original.length) {
            val origChar = original[origIdx]
            if (origChar == formChar || isSuperscriptEquivalent(origChar, formChar)) {
                formToOrig[formIdx] = origIdx
                origIdx++
                assigned = true
                break
            } else if (origChar == '.' || origChar == '-') {
                origIdx++
            } else {
                formToOrig[formIdx] = origIdx
                origIdx++
                assigned = true
                break
            }
        }
        if (!assigned) {
            formToOrig[formIdx] = original.length
        }
    }
    return formToOrig
}

fun formatStringToSuperscript(input: String): String {
    var result = input
    val regexDots = """(\d+)\.(\d+)""".toRegex()
    result = regexDots.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { char ->
            when (char) {
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                else -> char
            }
        }.joinToString("")
        "$base$sup"
    }
    val regexHyphens = """(\d+)-(\d+)""".toRegex()
    result = regexHyphens.replace(result) { matchResult ->
        val base = matchResult.groupValues[1]
        val suffix = matchResult.groupValues[2]
        val sup = suffix.map { char ->
            when (char) {
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                else -> char
            }
        }.joinToString("")
        "$base$sup"
    }
    return result
}
