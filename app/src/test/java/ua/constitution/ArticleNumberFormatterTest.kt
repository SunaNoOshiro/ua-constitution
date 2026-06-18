package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.domain.text.ArticleNumberFormatter

/**
 * Characterizes ArticleNumberFormatter — the pure numeric id formatting extracted from the
 * formatArticleId / ArticleIdText composables. (id == 0 / preamble stays in the UI as it resolves a
 * localized string, so it is not covered here.)
 */
class ArticleNumberFormatterTest {

    @Test
    fun `format renders a plain number for a normal article`() {
        assertEquals("20", ArticleNumberFormatter.format(20, 1))
        assertEquals("1000", ArticleNumberFormatter.format(1000, 0)) // 1000 is NOT > 1000
    }

    @Test
    fun `format raises the trailing digit of ids over 1000 to a superscript`() {
        assertEquals("100¹", ArticleNumberFormatter.format(1001, 0))
        assertEquals("100⁹", ArticleNumberFormatter.format(1009, 0))
    }

    @Test
    fun `CHARACTERIZATION format special-cases article 16-1 in chapter 15`() {
        assertEquals("16¹", ArticleNumberFormatter.format(161, 15))
        // Same id outside chapter 15 is just the plain number.
        assertEquals("161", ArticleNumberFormatter.format(161, 0))
    }

    @Test
    fun `isFractional matches ids over 1000 and the chapter-15 special case`() {
        assertTrue(ArticleNumberFormatter.isFractional(1001, 0))
        assertTrue(ArticleNumberFormatter.isFractional(161, 15))
        assertFalse(ArticleNumberFormatter.isFractional(1000, 0))
        assertFalse(ArticleNumberFormatter.isFractional(161, 0))
        assertFalse(ArticleNumberFormatter.isFractional(20, 1))
    }

    @Test
    fun `fractionalParts splits base and suffix`() {
        assertEquals("100" to "1", ArticleNumberFormatter.fractionalParts(1001, 0))
        assertEquals("100" to "9", ArticleNumberFormatter.fractionalParts(1009, 0))
        assertEquals("16" to "1", ArticleNumberFormatter.fractionalParts(161, 15))
    }

    @Test
    fun `formatWithPreamble returns the preamble label only for id 0`() {
        assertEquals("Преамбула", ArticleNumberFormatter.formatWithPreamble(0, 0, "Преамбула"))
        // id != 0 ignores the label and defers to format()
        assertEquals("20", ArticleNumberFormatter.formatWithPreamble(20, 1, "Преамбула"))
        assertEquals("16¹", ArticleNumberFormatter.formatWithPreamble(161, 15, "Преамбула"))
    }
}
