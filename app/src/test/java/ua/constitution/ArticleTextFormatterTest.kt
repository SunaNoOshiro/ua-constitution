package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.text.formatArticleForCopy

/**
 * Pure tests for the article copy-text builder extracted (DRY) from ArticleCard's copy button and
 * fullArticleTextToCopy memo in N3. Pins the assembly (number/name/blank-line separators) and that
 * each paragraph is superscript-formatted.
 */
class ArticleTextFormatterTest {

    @Test
    fun `number only, single paragraph, no trailing separator`() {
        assertEquals("20\n\nтекст", formatArticleForCopy("20", "", listOf("текст")))
    }

    @Test
    fun `number plus name and multiple paragraphs separated by blank lines`() {
        assertEquals(
            "20. Назва\n\nперший\n\nдругий",
            formatArticleForCopy("20", "Назва", listOf("перший", "другий"))
        )
    }

    @Test
    fun `empty name omits the dot-space separator`() {
        assertEquals("Преамбула\n\nтекст", formatArticleForCopy("Преамбула", "", listOf("текст")))
    }

    @Test
    fun `paragraph text is superscript-formatted`() {
        // formatStringToSuperscript("16.1") -> "16¹"
        assertEquals("20\n\n16¹", formatArticleForCopy("20", "", listOf("16.1")))
    }

    @Test
    fun `no paragraphs still emits the header and blank line`() {
        assertEquals("20\n\n", formatArticleForCopy("20", "", emptyList()))
    }
}
