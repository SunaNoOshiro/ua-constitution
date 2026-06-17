package ua.constitution.domain.title

import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.utils.Constants

/** A parsed article title split into its display part (label + number) and the descriptive name. */
data class ArticleTitle(val display: String, val name: String)

/**
 * Splits a title like "Стаття 20. Державні символи України" into a display part ("Стаття 20", with
 * the number superscript-formatted) and the name ("Державні символи України"). Titles that don't
 * match the "Стаття|Пункт N" pattern fall back to superscript-formatting the whole title with an
 * empty name. Extracted verbatim from ArticleCard; now uses Constants.SPLIT_TITLE_REGEX_PATTERN
 * (previously an unused constant duplicated inline).
 */
fun parseArticleTitle(titleUa: String): ArticleTitle {
    val regex = Constants.SPLIT_TITLE_REGEX_PATTERN.toRegex()
    val match = regex.find(titleUa)
    return if (match != null) {
        val label = match.groupValues[1]
        val rawNumber = match.groupValues[2]
        val formattedNumber = formatStringToSuperscript(rawNumber)
        val namePart = match.groupValues[3]
        ArticleTitle("$label $formattedNumber", namePart)
    } else {
        ArticleTitle(formatStringToSuperscript(titleUa), "")
    }
}
