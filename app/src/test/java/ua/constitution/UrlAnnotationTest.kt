package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.constitution.data.model.Article
import ua.constitution.domain.link.handleLinkAnnotationTap
import ua.constitution.domain.link.isInternalArticleLink
import ua.constitution.domain.link.parseUrlAnnotation
import ua.constitution.domain.link.resolveExternalUrl

/**
 * Pure tests for the link-annotation tap logic deduped out of InteractiveText's three identical
 * inline handlers in N5. The composable tap handlers themselves cannot be triggered under
 * Robolectric (readOnly reader field), so these are the primary safety net for the extracted flow.
 */
class UrlAnnotationTest {

    private val base = "https://base"

    @Test
    fun `parseUrlAnnotation splits url and text with empty defaults`() {
        assertEquals("u" to "t", parseUrlAnnotation("u|t"))
        assertEquals("u" to "", parseUrlAnnotation("u"))
        assertEquals("" to "", parseUrlAnnotation(""))
        assertEquals("u" to "t", parseUrlAnnotation("u|t|extra")) // extra parts ignored
    }

    @Test
    fun `isInternalArticleLink is true for anchors and non-http urls`() {
        assertTrue(isInternalArticleLink("#n4164"))
        assertTrue(isInternalArticleLink("ст. 20"))
        assertTrue(isInternalArticleLink(""))
        assertFalse(isInternalArticleLink("http://x"))
        assertFalse(isInternalArticleLink("https://x"))
    }

    @Test
    fun `resolveExternalUrl prefixes anchors with the base url only`() {
        assertEquals("https://base#a", resolveExternalUrl("#a", base))
        assertEquals("http://x", resolveExternalUrl("http://x", base))
    }

    @Test
    fun `internal link that resolves navigates and does not open a url`() {
        val target = articleOf(id = 20, chapterId = 2)
        var clicked: Article? = null
        var opened: String? = null
        handleLinkAnnotationTap("#frag|ст. 20", base, { if (it == "ст. 20") target else null }, { clicked = it }) { opened = it }
        assertEquals(target, clicked)
        assertNull(opened)
    }

    @Test
    fun `internal anchor that fails to resolve opens the base-resolved url`() {
        var clicked: Article? = null
        var opened: String? = null
        handleLinkAnnotationTap("#frag|ст. 99", base, { null }, { clicked = it }) { opened = it }
        assertNull(clicked)
        assertEquals("https://base#frag", opened)
    }

    @Test
    fun `external link skips resolution and opens as-is`() {
        var resolverCalled = false
        var opened: String? = null
        handleLinkAnnotationTap("https://e.com|label", base, { resolverCalled = true; null }, { }) { opened = it }
        assertFalse(resolverCalled)
        assertEquals("https://e.com", opened)
    }

    @Test
    fun `empty url neither navigates nor opens`() {
        var clicked: Article? = null
        var opened: String? = null
        handleLinkAnnotationTap("|", base, { null }, { clicked = it }) { opened = it }
        assertNull(clicked)
        assertNull(opened)
    }
}
