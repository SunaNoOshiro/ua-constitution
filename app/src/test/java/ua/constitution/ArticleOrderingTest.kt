package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.domain.text.ArticleNumberFormatter.sortKey

/**
 * Pins the article sort-key rule (ArticleNumberFormatter.sortKey). With the N*1000+M encoding a
 * fractional id sorts at its true base.suffix value and a whole id at its integer value — so a
 * fractional article sorts next to its base article, with no chapterId disambiguation. Pure JVM.
 */
class ArticleOrderingTest {

    @Test
    fun `whole article ids sort by their value`() {
        assertEquals(0.0, sortKey(0), 0.0)
        assertEquals(16.0, sortKey(16), 0.0)
        assertEquals(161.0, sortKey(161), 0.0)
    }

    @Test
    fun `fractional ids sort at base point suffix, right after their base article`() {
        assertEquals(16.1, sortKey(16001), 1e-9)   // 16¹ sorts between 16 and 17
        assertEquals(129.1, sortKey(129001), 1e-9)
        assertEquals(131.2, sortKey(131002), 1e-9)
    }
}
