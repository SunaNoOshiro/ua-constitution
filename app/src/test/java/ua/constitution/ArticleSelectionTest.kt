package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.constitution.domain.content.articleOfDayIndex
import ua.constitution.domain.content.selectArticlesByBookmarkIds

/**
 * Pure (no Robolectric) tests for the article-selection helpers extracted from the ViewModel in N1.
 * The ViewModel delegation itself stays pinned by ConstitutionViewModelAccessorsTest; these pin the
 * extracted math/filter directly, including the edge cases that were awkward to reach via the VM.
 */
class ArticleSelectionTest {

    @Test
    fun `articleOfDayIndex wraps modulo the article count`() {
        assertEquals(0, articleOfDayIndex(0, 4))
        assertEquals(3, articleOfDayIndex(3, 4))
        assertEquals(0, articleOfDayIndex(4, 4)) // wraps
        assertEquals(3, articleOfDayIndex(7, 4))
        assertEquals(0, articleOfDayIndex(100, 4))
        assertEquals(1, articleOfDayIndex(366, 5)) // Calendar.DAY_OF_YEAR is 1..366
    }

    @Test
    fun `articleOfDayIndex is null when there are no articles`() {
        assertNull(articleOfDayIndex(42, 0))
        assertNull(articleOfDayIndex(0, 0))
    }

    @Test
    fun `selectArticlesByBookmarkIds preserves article order regardless of id order`() {
        // bookmarkId = id + chapterId * 10000
        val articles = listOf(
            articleOf(id = 1, chapterId = 1),   // bookmarkId 10001
            articleOf(id = 2, chapterId = 1),   // bookmarkId 10002
            articleOf(id = 20, chapterId = 2)   // bookmarkId 20020
        )
        // ids intentionally reversed; result must follow article order, not the id list order.
        val result = selectArticlesByBookmarkIds(articles, listOf(20020, 10001))
        assertEquals(listOf(1, 20), result.map { it.id })
    }

    @Test
    fun `selectArticlesByBookmarkIds is empty when nothing matches`() {
        val articles = listOf(articleOf(id = 1, chapterId = 1))
        assertEquals(emptyList<Int>(), selectArticlesByBookmarkIds(articles, listOf(99999)).map { it.id })
        assertEquals(emptyList<Int>(), selectArticlesByBookmarkIds(articles, emptyList()).map { it.id })
    }
}
