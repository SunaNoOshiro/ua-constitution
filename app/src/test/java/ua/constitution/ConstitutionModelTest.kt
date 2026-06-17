package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.data.model.Link

/**
 * Characterizes the pure computed properties on the data model (ConstitutionData.kt):
 * Note.text / Note.links, Paragraph.text, Article.textUa, Article.bookmarkId.
 *
 * No Android types are touched, so this runs on the plain JVM.
 */
class ConstitutionModelTest {

    @Test
    fun `Note text concatenates value for text segments and text for link segments`() {
        val note = noteOf(
            textSegment("See "),
            linkSegment(text = "Law 123", url = "https://example.org"),
            textSegment(" now")
        )
        assertEquals("See Law 123 now", note.text)
    }

    @Test
    fun `Note links returns only the link segments mapped to Link`() {
        val note = noteOf(
            textSegment("a"),
            linkSegment(text = "L1", url = "u1"),
            textSegment("b"),
            linkSegment(text = "L2", url = "u2")
        )
        assertEquals(listOf(Link("L1", "u1"), Link("L2", "u2")), note.links)
    }

    @Test
    fun `Note with no link segments has empty links and empty content yields empty text`() {
        assertEquals(emptyList<Link>(), noteOf(textSegment("plain")).links)
        assertEquals("", noteOf().text)
    }

    @Test
    fun `Paragraph text uses the same text-vs-link rule as Note`() {
        val p = paragraphOf(
            textSegment("Україна "),
            linkSegment(text = "посилання", url = "u"),
            textSegment(" кінець")
        )
        assertEquals("Україна посилання кінець", p.text)
    }

    @Test
    fun `Article textUa joins paragraph texts with newlines`() {
        val article = articleOf(
            id = 1,
            paragraphs = listOf(
                paragraphOf(textSegment("Перший абзац")),
                paragraphOf(textSegment("Другий абзац"))
            )
        )
        assertEquals("Перший абзац\nДругий абзац", article.textUa)
    }

    @Test
    fun `Article textUa is empty when there are no paragraphs`() {
        assertEquals("", articleOf(id = 1, paragraphs = emptyList()).textUa)
    }

    @Test
    fun `Article bookmarkId combines id with chapterId times ten thousand`() {
        assertEquals(20020, articleOf(id = 20, chapterId = 2).bookmarkId)
        assertEquals(0, articleOf(id = 0, chapterId = 0).bookmarkId)
        assertEquals(10161, articleOf(id = 161, chapterId = 1).bookmarkId)
    }
}
