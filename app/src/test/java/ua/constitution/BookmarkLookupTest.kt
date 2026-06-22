package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.ui.editsJsonFor
import ua.constitution.ui.isBookmarked

/**
 * Pins the pure bookmark-lookup extensions (used by 5 UI call sites). Matching is by the article's
 * derived bookmarkId (= id + chapterId * 10000), so articleOf(id=5, chapterId=2).bookmarkId == 20005.
 */
class BookmarkLookupTest {

    private val article = articleOf(id = 5, chapterId = 2) // bookmarkId = 20005

    @Test
    fun `isBookmarked is true when a bookmark matches the article bookmarkId`() {
        assertTrue(listOf(BookmarkEntity(articleId = article.bookmarkId)).isBookmarked(article))
    }

    @Test
    fun `isBookmarked is false for a non-matching id or an empty list`() {
        assertFalse(listOf(BookmarkEntity(articleId = 999)).isBookmarked(article))
        assertFalse(emptyList<BookmarkEntity>().isBookmarked(article))
    }

    @Test
    fun `editsJsonFor returns the matching bookmark's editsJson`() {
        val list = listOf(BookmarkEntity(articleId = article.bookmarkId, editsJson = "{edits}"))
        assertEquals("{edits}", list.editsJsonFor(article))
    }

    @Test
    fun `editsJsonFor returns empty string when the article is not bookmarked`() {
        assertEquals("", emptyList<BookmarkEntity>().editsJsonFor(article))
        assertEquals("", listOf(BookmarkEntity(articleId = 999, editsJson = "y")).editsJsonFor(article))
    }
}
