package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.constitution.domain.link.findArticleByLink

/**
 * Characterizes findArticleByLink (domain/link) — turning cross-reference link text such as
 * "ст. 20", "п. 5" or "16¹" into an Article.
 *
 * Pure JVM. After the SOLID refactor the resolver takes the article list as a parameter, so the
 * test simply passes a fixture list — no global singleton to seed.
 */
class ArticleLinkResolutionTest {

    // Two articles share id 5 — one in a normal chapter, one in chapter 15 — to characterize the
    // chapter-15 preference rules. id 161 is the parsed form of "16.1".
    private val articles = listOf(
        articleOf(id = 5, chapterId = 1, titleUa = "Стаття 5"),
        articleOf(id = 5, chapterId = 15, titleUa = "Пункт 5"),
        articleOf(id = 12, chapterId = 1, titleUa = "Стаття 12"),
        articleOf(id = 20, chapterId = 2, titleUa = "Стаття 20"),
        articleOf(id = 125, chapterId = 8, titleUa = "Стаття 125"),
        articleOf(id = 161, chapterId = 15, titleUa = "Стаття 16.1")
    )

    @Test
    fun `resolves a plain article reference`() {
        val a = findArticleByLink("ст. 20", articles)
        assertEquals(20, a?.id)
        assertEquals(2, a?.chapterId)
    }

    @Test
    fun `resolves a reference spelled out as стаття`() {
        assertEquals(20, findArticleByLink("стаття 20", articles)?.id)
    }

    @Test
    fun `superscript and dash forms normalize to the same fractional article`() {
        // "16¹" -> "16.1" -> Math.round(16.1*10)=161
        assertEquals(161, findArticleByLink("16¹", articles)?.id)
        assertEquals(161, findArticleByLink("16.1", articles)?.id)
        assertEquals(161, findArticleByLink("16-1", articles)?.id)   // hyphen
        assertEquals(161, findArticleByLink("16–1", articles)?.id)   // en-dash
        assertEquals(161, findArticleByLink("16—1", articles)?.id)   // em-dash
    }

    @Test
    fun `CHARACTERIZATION a punkt reference prefers the chapter 15 article`() {
        val a = findArticleByLink("п. 5", articles)
        assertEquals(5, a?.id)
        assertEquals(15, a?.chapterId)
        assertEquals(15, findArticleByLink("пункт 5", articles)?.chapterId)
    }

    @Test
    fun `CHARACTERIZATION a non-punkt reference prefers the non-chapter-15 article`() {
        val a = findArticleByLink("ст. 5", articles)
        assertEquals(5, a?.id)
        assertEquals(1, a?.chapterId)
    }

    @Test
    fun `bare number resolves against non-chapter-15 articles first`() {
        assertEquals(125, findArticleByLink("125", articles)?.id)
    }

    @Test
    fun `CHARACTERIZATION only the first number in the text is used`() {
        // "1.2" -> 12; the later "3.4" is ignored because regex.find returns the first match only.
        val a = findArticleByLink("Article 1.2 and 3.4", articles)
        assertEquals(12, a?.id)
    }

    @Test
    fun `unknown article number resolves to null`() {
        assertNull(findArticleByLink("999", articles))
    }

    @Test
    fun `text with no number resolves to null`() {
        assertNull(findArticleByLink("abc", articles))
        assertNull(findArticleByLink("", articles))
    }
}
