package ua.constitution.domain.link

/**
 * Lowercase markers that identify a "punkt" (point) cross-reference, e.g. "п. 5" or "пункт 5".
 * Co-located with [findArticleByLink], their only consumer — moved out of the global Constants
 * grab-bag for cohesion. String values are unchanged.
 */
internal object LinkPatterns {
    const val PUNKT_FULL = "пункт"
    const val P_DOT = "п."
    const val P_SPACE_START = "п "
    const val P_SPACE_MID = " п "
}
