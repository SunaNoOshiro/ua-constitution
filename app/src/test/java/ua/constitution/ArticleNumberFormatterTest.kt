package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.domain.text.ArticleNumberFormatter

/**
 * Characterizes ArticleNumberFormatter — the single source of truth for the article-number <-> id
 * encoding and its display. Fractional numbers encode as N*1000+M (16.1 -> 16001), whole numbers as
 * themselves, so a fractional id is simply id > 1000 (no chapterId disambiguation needed). id == 0 /
 * preamble stays in the UI as it resolves a localized string.
 */
class ArticleNumberFormatterTest {

    @Test
    fun `encode maps a JSON article number to its id`() {
        assertEquals(16, ArticleNumberFormatter.encode(16.0))
        assertEquals(161, ArticleNumberFormatter.encode(161.0))
        assertEquals(16001, ArticleNumberFormatter.encode(16.1))
        assertEquals(129001, ArticleNumberFormatter.encode(129.1))
        assertEquals(131002, ArticleNumberFormatter.encode(131.2))
    }

    @Test
    fun `encode of a fractional id never collides with a whole article id`() {
        // The whole point of the N*1000+M scheme: 16.1 (16001) no longer equals Article 161 (161).
        assertFalse(ArticleNumberFormatter.encode(16.1) == ArticleNumberFormatter.encode(161.0))
    }

    @Test
    fun `format renders a plain number for a normal article`() {
        assertEquals("20", ArticleNumberFormatter.format(20))
        assertEquals("161", ArticleNumberFormatter.format(161))
        assertEquals("1000", ArticleNumberFormatter.format(1000)) // 1000 is NOT > 1000
    }

    @Test
    fun `format raises the minor digit of fractional ids to a superscript`() {
        assertEquals("16¹", ArticleNumberFormatter.format(16001))
        assertEquals("100¹", ArticleNumberFormatter.format(100001))
        assertEquals("100⁹", ArticleNumberFormatter.format(100009))
        assertEquals("131²", ArticleNumberFormatter.format(131002))
    }

    @Test
    fun `isFractional matches ids over 1000 only`() {
        assertTrue(ArticleNumberFormatter.isFractional(16001))
        assertTrue(ArticleNumberFormatter.isFractional(129001))
        assertFalse(ArticleNumberFormatter.isFractional(1000))
        assertFalse(ArticleNumberFormatter.isFractional(161))
        assertFalse(ArticleNumberFormatter.isFractional(20))
    }

    @Test
    fun `fractionalParts splits base and suffix`() {
        assertEquals("16" to "1", ArticleNumberFormatter.fractionalParts(16001))
        assertEquals("100" to "9", ArticleNumberFormatter.fractionalParts(100009))
        assertEquals("131" to "2", ArticleNumberFormatter.fractionalParts(131002))
    }

    @Test
    fun `formatWithPreamble returns the preamble label only for id 0`() {
        assertEquals("Преамбула", ArticleNumberFormatter.formatWithPreamble(0, "Преамбула"))
        // id != 0 ignores the label and defers to format()
        assertEquals("20", ArticleNumberFormatter.formatWithPreamble(20, "Преамбула"))
        assertEquals("16¹", ArticleNumberFormatter.formatWithPreamble(16001, "Преамбула"))
    }
}
