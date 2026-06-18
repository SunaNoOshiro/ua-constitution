package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.content.ChapterRangeKind
import ua.constitution.domain.content.chapterRangeKind

/**
 * Pure tests for the chapter-card range-label decision extracted from the dashboard CHAPTERS branch
 * (N8). Rendered strings stay pinned by the dashboard; this pins which label kind applies, including
 * the chapter-0 (preamble) and chapter-15 (points) special cases and the single-vs-multi split.
 */
class ChapterRangeTest {

    @Test
    fun `chapter 0 is the preamble label`() {
        assertEquals(ChapterRangeKind.PREAMBLE, chapterRangeKind(0, firstEqualsLast = true))
        assertEquals(ChapterRangeKind.PREAMBLE, chapterRangeKind(0, firstEqualsLast = false))
    }

    @Test
    fun `chapter 15 uses the point range labels`() {
        assertEquals(ChapterRangeKind.POINT_SINGLE, chapterRangeKind(15, firstEqualsLast = true))
        assertEquals(ChapterRangeKind.POINT_MULTI, chapterRangeKind(15, firstEqualsLast = false))
    }

    @Test
    fun `ordinary chapters use the article range labels`() {
        assertEquals(ChapterRangeKind.ARTICLE_SINGLE, chapterRangeKind(1, firstEqualsLast = true))
        assertEquals(ChapterRangeKind.ARTICLE_MULTI, chapterRangeKind(1, firstEqualsLast = false))
        assertEquals(ChapterRangeKind.ARTICLE_MULTI, chapterRangeKind(14, firstEqualsLast = false))
    }
}
