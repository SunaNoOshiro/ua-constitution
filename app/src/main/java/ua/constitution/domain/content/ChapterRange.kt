package ua.constitution.domain.content

/**
 * Which article-range label a chapter card shows, decided purely from the chapter id and whether the
 * chapter's first and last article ids render the same. Extracted from the inline branching in the
 * dashboard CHAPTERS card so the decision is unit-testable; the UI maps each kind to its localized
 * string resource (with the formatted first/last ids). Branch order is verbatim:
 * preamble (chapter 0) → chapter-15 points → ordinary articles, single vs. multi by first==last.
 */
enum class ChapterRangeKind { PREAMBLE, POINT_SINGLE, POINT_MULTI, ARTICLE_SINGLE, ARTICLE_MULTI }

fun chapterRangeKind(chapterId: Int, firstEqualsLast: Boolean): ChapterRangeKind = when {
    chapterId == 0 -> ChapterRangeKind.PREAMBLE
    chapterId == 15 && firstEqualsLast -> ChapterRangeKind.POINT_SINGLE
    chapterId == 15 -> ChapterRangeKind.POINT_MULTI
    firstEqualsLast -> ChapterRangeKind.ARTICLE_SINGLE
    else -> ChapterRangeKind.ARTICLE_MULTI
}
