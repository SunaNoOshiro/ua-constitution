package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.domain.content.searchArticles

/**
 * Characterizes the pure search/filter rule extracted from the ViewModel. Testing it directly (no
 * coroutines / StateFlow) is exactly the testability win the extraction unlocks; the StateFlow
 * integration is still covered by ConstitutionViewModelTest.
 */
class ArticleSearchTest {

    private val all = listOf(
        articleOf(id = 0, chapterId = 0, titleUa = "Преамбула"),
        articleOf(id = 1, chapterId = 1, titleUa = "Стаття 1",
            paragraphs = listOf(paragraphOf(textSegment("Україна є суверенна і незалежна держава")))),
        articleOf(id = 2, chapterId = 1, titleUa = "Стаття 2"),
        articleOf(id = 20, chapterId = 2, titleUa = "Стаття 20",
            paragraphs = listOf(paragraphOf(textSegment("Державні символи України"))))
    )

    @Test
    fun `empty query returns everything`() {
        assertEquals(4, searchArticles(all, "", null).size)
    }

    @Test
    fun `empty query scoped to a chapter returns only that chapter`() {
        assertEquals(listOf(1, 2), searchArticles(all, "", 1).map { it.id })
    }

    @Test
    fun `CHARACTERIZATION a non-empty query ignores the chapter scope (global search)`() {
        // chapter 2 selected, but query "1" still surfaces article 1 from chapter 1.
        assertTrue(searchArticles(all, "1", 2).map { it.id }.contains(1))
    }

    @Test
    fun `CHARACTERIZATION id match is exact OR substring`() {
        assertTrue(searchArticles(all, "2", null).map { it.id }.containsAll(listOf(2, 20)))
        assertEquals(listOf(20), searchArticles(all, "20", null).map { it.id })
    }

    @Test
    fun `title and body searches are case-insensitive`() {
        assertEquals(listOf(1, 2, 20), searchArticles(all, "СТАТТЯ", null).map { it.id })
        assertEquals(listOf(20), searchArticles(all, "державні", null).map { it.id })
    }

    @Test
    fun `query matching nothing returns empty`() {
        assertTrue(searchArticles(all, "zzzzz", null).isEmpty())
    }
}
