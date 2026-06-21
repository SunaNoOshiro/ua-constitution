package ua.constitution.domain.text

/**
 * The single source of truth for the article-number <-> id encoding and its presentation.
 *
 * A fractional article number "N.M" (e.g. 16¹ = 16.1) is encoded as `N * 1000 + M`, so 16.1 -> 16001,
 * 129.1 -> 129001, 131.2 -> 131002. Whole article numbers keep their value (16 -> 16, 161 -> 161).
 * Because real article numbers are < 1000, an encoded id is fractional iff `id > 1000` — an
 * unambiguous, collision-free test (the previous `N*10 + M` scheme made 16.1 -> 161, which collided
 * with real Article 161 and forced a `chapterId == 15` special case throughout the app).
 *
 * The id == 0 (preamble) case is handled by the UI, which resolves a localized string.
 */
object ArticleNumberFormatter {

    /** Encodes a JSON article number (16 or 16.1) to its id. Fractional -> N*1000+M, whole -> N. */
    fun encode(number: Double): Int {
        if (number % 1.0 == 0.0) return number.toInt()
        val major = number.toInt()
        val minor = Math.round(number * 10).toInt() - major * 10
        return major * 1000 + minor
    }

    /** True when the id renders with a raised superscript suffix (a "fractional" article). */
    fun isFractional(id: Int): Boolean = id > 1000

    /**
     * Base + superscript-suffix split for a fractional id: 16001 -> ("16", "1"), 131002 -> ("131", "2").
     * Only meaningful when [isFractional] is true.
     */
    fun fractionalParts(id: Int): Pair<String, String> =
        (id / 1000).toString() to (id % 1000).toString()

    /** The value an article number sorts by: a fractional article sorts at base.suffix
     *  (16001 -> 16.1, 129001 -> 129.1); everything else at its integer id. */
    fun sortKey(id: Int): Double =
        if (isFractional(id)) (id / 1000) + (id % 1000) / 10.0 else id.toDouble()

    /**
     * [format], but returns [preambleLabel] for the preamble (id == 0). Lets the UI keep resolving
     * the localized preamble string while the id == 0 branch becomes pure and unit-testable.
     */
    fun formatWithPreamble(id: Int, preambleLabel: String): String =
        if (id == 0) preambleLabel else format(id)

    /**
     * Renders the article number with a unicode superscript suffix where applicable:
     * 16001 -> "16¹", 100009 -> "100⁹", 20 -> "20". Does not handle id == 0.
     */
    fun format(id: Int): String {
        if (!isFractional(id)) return id.toString()
        val (base, suffix) = fractionalParts(id)
        return "$base${digitToSuperscript('0' + suffix.toInt())}"
    }
}
