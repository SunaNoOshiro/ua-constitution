package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.content.mergeAdjacentLinkSegments

/** Pins the adjacent-link-segment merge extracted from the reader composables in H2. */
class SegmentMergingTest {

    @Test
    fun `consecutive links with the same url merge into one, concatenating text`() {
        val out = mergeAdjacentLinkSegments(
            listOf(linkSegment("149", "#149"), linkSegment("-1", "#149"))
        )
        assertEquals(1, out.size)
        assertEquals("149-1", out[0].text)
        assertEquals("#149", out[0].url)
    }

    @Test
    fun `consecutive links with different urls are kept separate`() {
        val out = mergeAdjacentLinkSegments(
            listOf(linkSegment("a", "#1"), linkSegment("b", "#2"))
        )
        assertEquals(listOf("#1", "#2"), out.map { it.url })
    }

    @Test
    fun `a text segment between two same-url links breaks the run`() {
        val out = mergeAdjacentLinkSegments(
            listOf(linkSegment("a", "#1"), textSegment(" "), linkSegment("b", "#1"))
        )
        assertEquals(3, out.size)
        assertEquals(listOf("link", "text", "link"), out.map { it.type })
    }

    @Test
    fun `non-link segments pass through unchanged and a trailing link is flushed`() {
        val out = mergeAdjacentLinkSegments(
            listOf(textSegment("intro "), linkSegment("x", "#1"), linkSegment("y", "#1"))
        )
        assertEquals(2, out.size)
        assertEquals("intro ", out[0].value)
        assertEquals("xy", out[1].text)
    }
}
