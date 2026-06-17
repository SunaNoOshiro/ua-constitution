package ua.constitution.domain.link

import ua.constitution.data.model.Article
import ua.constitution.utils.Constants

/** Resolves cross-reference link text (e.g. "ст. 20", "п. 5", "16¹") to an Article.
 * Takes the article list as a parameter rather than reaching into a global singleton. */

fun findArticleByLink(text: String, articles: List<Article>): Article? {
    // Normalise text
    val normalized = text
        .replace("¹", ".1")
        .replace("²", ".2")
        .replace("³", ".3")
        .replace("⁴", ".4")
        .replace("⁵", ".5")
        .replace("⁶", ".6")
        .replace("⁷", ".7")
        .replace("⁸", ".8")
        .replace("⁹", ".9")
        .replace("⁰", ".0")
        .replace("-", ".") // "16-1" -> "16.1"
        .replace("–", ".") // en-dash
        .replace("—", ".") // em-dash

    // Check if the link specifically refers to a "punkt" / "п"
    val lower = text.lowercase()
    val isPunkt = lower.contains(Constants.LINK_PUNKT_FULL) ||
                  lower.contains(Constants.LINK_P_DOT) ||
                  lower.startsWith(Constants.LINK_P_SPACE_START) ||
                  lower.contains(Constants.LINK_P_SPACE_MID)

    // Find decimal or integer number (e.g. "125" or "16.1")
    val regex = """\d+(?:\.\d+)?""".toRegex()
    val match = regex.find(normalized)
    if (match != null) {
        val numberStr = match.value
        val dVal = numberStr.toDoubleOrNull()
        if (dVal != null) {
            val targetId = if (dVal % 1.0 != 0.0) {
                Math.round(dVal * 10).toInt()
            } else {
                dVal.toInt()
            }
            // If it's labeled as a "punkt" (point) or chapter 15 reference, look in chapter 15 first
            val article = if (isPunkt) {
                articles.find { it.id == targetId && it.chapterId == 15 }
                    ?: articles.find { it.id == targetId }
            } else {
                articles.find { it.id == targetId && it.chapterId != 15 }
                    ?: articles.find { it.id == targetId }
            }
            if (article != null) return article
        }
    }
    return null
}
