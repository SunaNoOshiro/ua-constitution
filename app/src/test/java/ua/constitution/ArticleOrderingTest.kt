package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.text.ArticleNumberFormatter.sortKey

/**
 * Pins the article sort-key rule (ArticleNumberFormatter.sortKey). Fractional articles sort at their
 * true base.suffix value via the same chapter-aware disambiguation the display uses; this is the
 * fix for the former `if (id > 1000)` sort that left 16¹ (encoded 161) at 161.0. Pure JVM.
 */
class ArticleOrderingTest {

    @Test
    fun whole_article_ids_sort_by_their_value() {
        assertEquals(0.0, sortKey(0, chapterId = 0), 0.0)
        assertEquals(16.0, sortKey(16, chapterId = 15), 0.0)
    }

    @Test
    fun high_fractional_ids_sort_at_base_suffix() {
        // 129.1 encodes to 1291, 151.2 to 1512 -> sort between their neighbours.
        assertEquals(129.1, sortKey(1291, chapterId = 8), 1e-9)
        assertEquals(151.2, sortKey(1512, chapterId = 12), 1e-9)
    }

    @Test
    fun low_fractional_16_1_now_sorts_at_16_1_not_161() {
        // The fix: 16¹ is id 161 in chapter 15; it must sort at 16.1 (right after article 16),
        // not at 161.0 (after article 160) as the old >1000-only rule did.
        assertEquals(16.1, sortKey(161, chapterId = 15), 1e-9)
    }

    @Test
    fun real_article_161_outside_chapter_15_still_sorts_at_161() {
        // The genuine Article 161 (chapter 14) is NOT fractional -> sorts at 161.0.
        assertEquals(161.0, sortKey(161, chapterId = 14), 0.0)
    }
}
