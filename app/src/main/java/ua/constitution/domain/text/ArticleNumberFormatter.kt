package ua.constitution.domain.text

/**
 * Pure numeric formatting for article ids — the non-localized logic previously inlined in the
 * formatArticleId / ArticleIdText composables. The id == 0 (preamble) case stays in the UI because
 * it resolves a localized string resource.
 */
object ArticleNumberFormatter {

    /** True when the id renders with a raised superscript suffix (a "fractional" article). */
    fun isFractional(id: Int, chapterId: Int): Boolean =
        id > 1000 || (chapterId == 15 && id == 161)

    /**
     * Base + superscript-suffix split for a fractional id, e.g. (1001, 0) -> ("100", "1"),
     * (161, 15) -> ("16", "1"). Only meaningful when [isFractional] is true.
     */
    fun fractionalParts(id: Int, chapterId: Int): Pair<String, String> {
        val base = if (chapterId == 15 && id == 161) "16" else (id / 10).toString()
        val suffix = if (chapterId == 15 && id == 161) "1" else (id % 10).toString()
        return base to suffix
    }

    /**
     * The value an article number sorts by. A fractional article sorts at base.suffix
     * (16¹ -> 16.1, 129¹ -> 129.1); everything else at its integer id. The fractional split is taken
     * from [isFractional]/[fractionalParts], so the "chapterId 15 + id 161 = 16¹" disambiguation
     * lives in ONE place (shared with [format]). This corrects the former inline `if (id > 1000)`
     * sort, which left 16¹ (encoded 161, <= 1000) at 161.0 — after article 160 — instead of 16.1.
     */
    fun sortKey(id: Int, chapterId: Int): Double {
        if (!isFractional(id, chapterId)) return id.toDouble()
        val (base, suffix) = fractionalParts(id, chapterId)
        return base.toDouble() + suffix.toDouble() / 10.0
    }

    /**
     * [format], but returns [preambleLabel] for the preamble (id == 0). Lets the UI keep resolving
     * the localized preamble string while the id == 0 branch becomes pure and unit-testable.
     */
    fun formatWithPreamble(id: Int, chapterId: Int, preambleLabel: String): String =
        if (id == 0) preambleLabel else format(id, chapterId)

    /**
     * Renders the article number with a unicode superscript suffix where applicable:
     * (161, 15) -> "16¹", (1001, 0) -> "100¹", (20, 1) -> "20". Does not handle id == 0.
     */
    fun format(id: Int, chapterId: Int): String {
        if (chapterId == 15 && id == 161) {
            return "16¹"
        }
        if (id > 1000) {
            val base = id / 10
            val superscript = digitToSuperscript('0' + (id % 10))
            return "$base$superscript"
        }
        return id.toString()
    }
}
