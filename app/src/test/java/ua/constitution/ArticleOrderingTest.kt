package ua.constitution

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.constitution.data.source.articleSortKey

/**
 * Pins the article sort-key rule extracted from the deserializer — including the known quirk that a
 * low fractional id (16.1 -> encoded 161) is NOT divided and therefore mis-sorts. Pure JVM.
 */
class ArticleOrderingTest {

    @Test
    fun whole_article_ids_sort_by_their_value() {
        assertEquals(0.0, articleSortKey(0), 0.0)
        assertEquals(16.0, articleSortKey(16), 0.0)
        assertEquals(161.0, articleSortKey(161), 0.0)
    }

    @Test
    fun high_fractional_ids_above_1000_are_divided_into_place() {
        // 129.1 encodes to 1291 (> 1000) -> 129.1, sorts between 129 and 130. Correct.
        assertEquals(129.1, articleSortKey(1291), 1e-9)
        assertEquals(151.2, articleSortKey(1512), 1e-9)
    }

    @Test
    fun CHARACTERIZATION_low_fractional_16_1_is_not_divided_and_mis_sorts() {
        // 16.1 encodes to 161 (<= 1000) so it is NOT divided: it sorts as 161.0 (after article 160)
        // and collides with the real Article 161. Pinned current behavior; fixing needs an encoding
        // change (observable ordering), deferred for an explicit decision.
        assertEquals(161.0, articleSortKey(161), 0.0)
    }
}
