package ua.constitution

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.data.model.ContentSegment
import ua.constitution.domain.text.formatStringToSuperscript
import ua.constitution.domain.text.mapOriginalToFormatted
import ua.constitution.ui.article.buildSegmentedAnnotatedString
import ua.constitution.ui.theme.SovereignBlue
import ua.constitution.utils.Constants

/**
 * Pins [buildSegmentedAnnotatedString] — the pure builder extracted from SegmentedTextWithEdits.
 * Mirrors production: original text is the segments' displayText joined; the formatted text and the
 * original->formatted index map come from the real superscript helpers.
 */
class SegmentedAnnotatedStringTest {

    private fun build(segments: List<ContentSegment>): AnnotatedString {
        val original = segments.joinToString("") { it.displayText }
        val formatted = formatStringToSuperscript(original)
        return buildSegmentedAnnotatedString(segments, formatted, mapOriginalToFormatted(original, formatted))
    }

    @Test
    fun `text segments are appended verbatim with no annotation or span`() {
        val result = build(listOf(textSegment("Hello world")))
        assertEquals("Hello world", result.text)
        assertTrue(result.getStringAnnotations(Constants.ANNOTATION_TAG_URL, 0, result.length).isEmpty())
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `link segment carries the url annotation and the blue bold underlined style`() {
        val result = build(listOf(textSegment("see "), linkSegment("Law 123", "http://x")))
        assertEquals("see Law 123", result.text)

        val anns = result.getStringAnnotations(Constants.ANNOTATION_TAG_URL, 0, result.length)
        assertEquals(1, anns.size)
        assertEquals("http://x|Law 123", anns[0].item)
        assertEquals("Law 123", result.text.substring(anns[0].start, anns[0].end))

        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0].item
        assertEquals(SovereignBlue, span.color)
        assertEquals(FontWeight.Bold, span.fontWeight)
        assertEquals(TextDecoration.Underline, span.textDecoration)
    }

    @Test
    fun `appended text uses superscript-formatted coordinates and links still map correctly`() {
        val segments = listOf(textSegment("ст. 16.1 "), linkSegment("див", "u"))
        val original = segments.joinToString("") { it.displayText }
        val formatted = formatStringToSuperscript(original)
        // sanity: the superscript path actually changed the text (16.1 -> 16<superscript>)
        assertTrue("expected superscript formatting to alter the text", formatted != original)

        val result = buildSegmentedAnnotatedString(segments, formatted, mapOriginalToFormatted(original, formatted))
        // builder reproduces the formatted text exactly
        assertEquals(formatted, result.text)
        // and the link annotation still spans exactly the link's display text
        val anns = result.getStringAnnotations(Constants.ANNOTATION_TAG_URL, 0, result.length)
        assertEquals(1, anns.size)
        assertEquals("див", result.text.substring(anns[0].start, anns[0].end))
    }
}
