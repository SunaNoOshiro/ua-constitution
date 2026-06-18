package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.domain.text.ParagraphRangeMapping
import ua.constitution.domain.text.StyledRange

/**
 * Pins the per-paragraph <-> combined-text coordinate math extracted from ArticleCard in G1.
 * The +2 "\n\n" joiner and the half-open [start, end) intersection are subtle, so these guard
 * the arithmetic independently of Compose.
 */
class ParagraphRangeMappingTest {

    private fun para(text: String) = paragraphOf(textSegment(text))
    private fun range(start: Int, end: Int) =
        StyledRange(start = start, end = end, colorHex = "#FFF59D", highlight = true, underscore = false)

    @Test
    fun `paragraphOffsets advances by each paragraph length plus the two-char joiner`() {
        val paras = listOf(para("ab"), para("cde"), para("f")) // lengths 2, 3, 1
        val offsets = ParagraphRangeMapping.paragraphOffsets(paras)
        assertEquals(listOf(0, 4, 9), offsets.toList()) // 0 ; 0+2+2=4 ; 4+3+2=9
    }

    @Test
    fun `flattenToSegments joins paragraphs with a double-newline segment between them`() {
        val segs = ParagraphRangeMapping.flattenToSegments(listOf(para("ab"), para("cd")))
        assertEquals(3, segs.size)
        assertEquals("ab", segs[0].value)
        assertEquals("\n\n", segs[1].value)
        assertEquals("cd", segs[2].value)
    }

    @Test
    fun `flattenToSegments adds no trailing joiner for a single paragraph`() {
        assertEquals(1, ParagraphRangeMapping.flattenToSegments(listOf(para("only"))).size)
    }

    @Test
    fun `toCombined shifts each paragraph's ranges by its offset`() {
        val paras = listOf(para("abcd"), para("efghi")) // offsets [0, 6]
        val offsets = ParagraphRangeMapping.paragraphOffsets(paras)
        val combined = ParagraphRangeMapping.toCombined(
            mapOf(0 to listOf(range(1, 3)), 1 to listOf(range(0, 2))),
            offsets
        )
        assertEquals(listOf(1 to 3, 6 to 8), combined.map { it.start to it.end })
    }

    @Test
    fun `toPerParagraph round-trips ranges that lie within a single paragraph`() {
        val paras = listOf(para("abcd"), para("efghi")) // offsets [0, 6]
        val offsets = ParagraphRangeMapping.paragraphOffsets(paras)
        val perPara = mapOf(0 to listOf(range(1, 3)), 1 to listOf(range(0, 2)))
        val combined = ParagraphRangeMapping.toCombined(perPara, offsets)
        val back = ParagraphRangeMapping.toPerParagraph(combined, paras, offsets)
        assertEquals(
            perPara.mapValues { e -> e.value.map { it.start to it.end } },
            back.mapValues { e -> e.value.map { it.start to it.end } }
        )
    }

    @Test
    fun `toPerParagraph clamps a range that overruns the paragraph end`() {
        val paras = listOf(para("abcd")) // len 4, span [0, 4)
        val offsets = ParagraphRangeMapping.paragraphOffsets(paras)
        val back = ParagraphRangeMapping.toPerParagraph(listOf(range(2, 10)), paras, offsets)
        assertEquals(listOf(2 to 4), back[0]!!.map { it.start to it.end })
    }

    @Test
    fun `toPerParagraph drops a range landing entirely in the joiner gap`() {
        val paras = listOf(para("ab"), para("cd")) // offsets [0, 4]; gap "\n\n" occupies [2, 4)
        val offsets = ParagraphRangeMapping.paragraphOffsets(paras)
        val back = ParagraphRangeMapping.toPerParagraph(listOf(range(2, 4)), paras, offsets)
        assertTrue(back.isEmpty())
    }
}
