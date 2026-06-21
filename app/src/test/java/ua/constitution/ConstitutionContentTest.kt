package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.data.model.ConstitutionContent

/**
 * Characterizes the immutable ConstitutionContent store (formerly the ConstitutionData object's
 * accessors). Pure JVM — no Robolectric, no reflection, no test-only initialize seam; the store is
 * just constructed directly. The real-asset load is covered by [ConstitutionLoaderTest].
 */
class ConstitutionContentTest {

    private fun content(
        articles: List<Article>,
        parsedChapters: List<Chapter> = emptyList(),
        defaultChapters: List<Chapter> = emptyList()
    ) = ConstitutionContent(
        articles = articles,
        parsedChapters = parsedChapters,
        defaultChapters = defaultChapters,
        integrityVerificationPass = true,
        computedHash = "",
        usedFallback = false,
        initializationError = ""
    )

    @Test
    fun `articles are exposed as given`() {
        val seeded = listOf(articleOf(id = 1, chapterId = 1), articleOf(id = 2, chapterId = 1))
        assertEquals(seeded, content(seeded).articles)
    }

    @Test
    fun `getArticlesForChapter returns only that chapter's articles`() {
        val c = content(
            listOf(
                articleOf(id = 1, chapterId = 1),
                articleOf(id = 2, chapterId = 1),
                articleOf(id = 20, chapterId = 2)
            )
        )
        assertEquals(listOf(1, 2), c.getArticlesForChapter(1).map { it.id })
        assertEquals(listOf(20), c.getArticlesForChapter(2).map { it.id })
        assertTrue(c.getArticlesForChapter(99).isEmpty())
    }

    @Test
    fun `chapters returns the parsed chapters when present`() {
        val parsed = listOf(Chapter(1, "Розділ I"), Chapter(2, "Розділ II"))
        val defaults = listOf(Chapter(0, "default"))
        assertEquals(parsed, content(listOf(articleOf(id = 1, chapterId = 1)), parsed, defaults).chapters)
    }

    @Test
    fun `chapters falls back to the default list when none were parsed`() {
        val defaults = listOf(Chapter(0, "preamble"), Chapter(15, "transitional"))
        assertEquals(defaults, content(listOf(articleOf(id = 1, chapterId = 1)), emptyList(), defaults).chapters)
    }
}
