package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.title.ArticleTitle
import ua.constitution.domain.title.parseArticleTitle

/**
 * Characterizes parseArticleTitle (domain/title) — the title-splitting the ArticleCard relies on.
 * Pure JVM. This pins the full pipeline (regex split + superscript formatting of the number).
 */
class TitleParsingTest {

    @Test
    fun `splits a Стаття title into display and name`() {
        assertEquals(
            ArticleTitle("Стаття 20", "Державні символи України"),
            parseArticleTitle("Стаття 20. Державні символи України")
        )
    }

    @Test
    fun `splits a Пункт title`() {
        assertEquals(ArticleTitle("Пункт 5", "Назва"), parseArticleTitle("Пункт 5. Назва"))
    }

    @Test
    fun `superscript-formats fractional and hyphenated article numbers in the display part`() {
        assertEquals(ArticleTitle("Стаття 16¹", "Текст"), parseArticleTitle("Стаття 16.1. Текст"))
        assertEquals(ArticleTitle("Стаття 129¹", "Судове рішення"), parseArticleTitle("Стаття 129-1. Судове рішення"))
    }

    @Test
    fun `a number with no name yields an empty name`() {
        assertEquals(ArticleTitle("Стаття 1", ""), parseArticleTitle("Стаття 1."))
    }

    @Test
    fun `the trailing dot after the number is optional`() {
        assertEquals(ArticleTitle("Стаття 2", ""), parseArticleTitle("Стаття 2"))
    }

    @Test
    fun `CHARACTERIZATION a non-matching title falls back to the whole title with an empty name`() {
        assertEquals(ArticleTitle("Преамбула", ""), parseArticleTitle("Преамбула"))
        assertEquals(ArticleTitle("Загальні засади", ""), parseArticleTitle("Загальні засади"))
    }
}
