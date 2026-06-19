package ua.constitution.domain.link

import ua.constitution.data.model.Article

/** Resolves cross-reference link text (e.g. "ст. 20", "п. 5", "16¹") to an Article.
 * Takes the article list as a parameter rather than reaching into a global singleton.
 * Decomposed into small helpers (normalize / punkt-detect / id-extract / lookup) so each stays
 * within the cyclomatic-complexity budget; behavior is verbatim (pinned by ArticleLinkResolutionTest). */

fun findArticleByLink(text: String, articles: List<Article>): Article? {
    val targetId = extractTargetId(normalizeLinkText(text)) ?: return null
    return findArticleById(articles, targetId, isPunktReference(text))
}

/** Normalizes superscripts and dashes to the plain "N.M" form so a number can be extracted. */
private fun normalizeLinkText(text: String): String = text
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

/** True when the link refers to a "punkt"/point (so chapter 15 should be searched first). */
private fun isPunktReference(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains(LinkPatterns.PUNKT_FULL) ||
        lower.contains(LinkPatterns.P_DOT) ||
        lower.startsWith(LinkPatterns.P_SPACE_START) ||
        lower.contains(LinkPatterns.P_SPACE_MID)
}

/** The first decimal/integer in [normalized] encoded as an article id ("16.1" -> 161), or null. */
private fun extractTargetId(normalized: String): Int? {
    val match = """\d+(?:\.\d+)?""".toRegex().find(normalized) ?: return null
    val dVal = match.value.toDoubleOrNull() ?: return null
    return if (dVal % 1.0 != 0.0) Math.round(dVal * 10).toInt() else dVal.toInt()
}

/** Looks up [targetId], preferring chapter 15 for punkt references and non-15 otherwise, then any. */
private fun findArticleById(articles: List<Article>, targetId: Int, isPunkt: Boolean): Article? =
    if (isPunkt) {
        articles.find { it.id == targetId && it.chapterId == 15 } ?: articles.find { it.id == targetId }
    } else {
        articles.find { it.id == targetId && it.chapterId != 15 } ?: articles.find { it.id == targetId }
    }
