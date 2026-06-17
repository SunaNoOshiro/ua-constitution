package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ua.constitution.data.model.ConstitutionData

/**
 * Characterizes findArticleByLink (MainActivity.kt) — turning cross-reference link text such as
 * "ст. 20", "п. 5" or "16¹" into an Article from ConstitutionData.
 *
 * Pure JVM: the function only touches ConstitutionData.articles (seeded via initializeForTests),
 * a regex and Math.round — no Android types.
 */
class ArticleLinkResolutionTest {

    @Before
    fun seedArticles() {
        // Note: two articles share id 5 — one in a normal chapter, one in chapter 15 — so we can
        // characterize the chapter-15 preference rules. id 161 is the parsed form of "16.1".
        ConstitutionData.initializeForTests(
            listOf(
                articleOf(id = 5, chapterId = 1, titleUa = "Стаття 5"),
                articleOf(id = 5, chapterId = 15, titleUa = "Пункт 5"),
                articleOf(id = 12, chapterId = 1, titleUa = "Стаття 12"),
                articleOf(id = 20, chapterId = 2, titleUa = "Стаття 20"),
                articleOf(id = 125, chapterId = 8, titleUa = "Стаття 125"),
                articleOf(id = 161, chapterId = 15, titleUa = "Стаття 16.1")
            )
        )
    }

    @Test
    fun `resolves a plain article reference`() {
        val a = findArticleByLink("ст. 20")
        assertEquals(20, a?.id)
        assertEquals(2, a?.chapterId)
    }

    @Test
    fun `resolves a reference spelled out as стаття`() {
        assertEquals(20, findArticleByLink("стаття 20")?.id)
    }

    @Test
    fun `superscript and dash forms normalize to the same fractional article`() {
        // "16¹" -> "16.1" -> Math.round(16.1*10)=161
        assertEquals(161, findArticleByLink("16¹")?.id)
        assertEquals(161, findArticleByLink("16.1")?.id)
        assertEquals(161, findArticleByLink("16-1")?.id)   // hyphen
        assertEquals(161, findArticleByLink("16–1")?.id)   // en-dash
        assertEquals(161, findArticleByLink("16—1")?.id)   // em-dash
    }

    @Test
    fun `CHARACTERIZATION a punkt reference prefers the chapter 15 article`() {
        val a = findArticleByLink("п. 5")
        assertEquals(5, a?.id)
        assertEquals(15, a?.chapterId)
        assertEquals(15, findArticleByLink("пункт 5")?.chapterId)
    }

    @Test
    fun `CHARACTERIZATION a non-punkt reference prefers the non-chapter-15 article`() {
        val a = findArticleByLink("ст. 5")
        assertEquals(5, a?.id)
        assertEquals(1, a?.chapterId)
    }

    @Test
    fun `bare number resolves against non-chapter-15 articles first`() {
        assertEquals(125, findArticleByLink("125")?.id)
    }

    @Test
    fun `CHARACTERIZATION only the first number in the text is used`() {
        // "1.2" -> 12; the later "3.4" is ignored because regex.find returns the first match only.
        val a = findArticleByLink("Article 1.2 and 3.4")
        assertEquals(12, a?.id)
    }

    @Test
    fun `unknown article number resolves to null`() {
        assertNull(findArticleByLink("999"))
    }

    @Test
    fun `text with no number resolves to null`() {
        assertNull(findArticleByLink("abc"))
        assertNull(findArticleByLink(""))
    }
}
