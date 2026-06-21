package ua.constitution

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.model.Article
import ua.constitution.ui.screens.DashboardNavState

/**
 * Pins the cross-article back-stack logic that used to be inlined in MainAppDashboard's closures and
 * was untestable there. Only the history decisions are exercised (pushOrigin/popBack/trimHistoryAt);
 * the scroll-bearing popToArticle/jumpToArticleIndex are deliberately not called — their scroll part
 * needs a laid-out list and is covered by the rendering pin (DashboardArticlesTabTest).
 *
 * Runs under Robolectric only so the injected LazyListState + snapshot state construct off-device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardNavStateTest {

    private val a = articleOf(1, chapterId = 1) // bookmarkId 10001
    private val b = articleOf(2, chapterId = 1) // bookmarkId 10002
    private val c = articleOf(3, chapterId = 1) // bookmarkId 10003

    private var chapterArticles: List<Article> = emptyList()

    private fun newNavState() = DashboardNavState(
        articlesListState = LazyListState(),
        scope = CoroutineScope(Dispatchers.Unconfined),
        articlesForChapter = { chapterArticles },
    )

    @Test
    fun pushOrigin_with_explicit_origin_records_it() {
        val nav = newNavState()
        nav.pushOrigin(origin = a, target = b, isArticlesTab = false)
        assertEquals(listOf(a), nav.navigationHistory.toList())
    }

    @Test
    fun pushOrigin_ignores_navigation_to_the_same_article() {
        val nav = newNavState()
        val sameId = articleOf(1, chapterId = 1) // same bookmarkId as `a`
        nav.pushOrigin(origin = a, target = sameId, isArticlesTab = false)
        assertTrue("origin must not be pushed when it is the target", nav.navigationHistory.isEmpty())
    }

    @Test
    fun pushOrigin_dedupes_a_consecutive_repeat_of_the_top_entry() {
        val nav = newNavState()
        nav.pushOrigin(origin = a, target = b, isArticlesTab = false)
        nav.pushOrigin(origin = a, target = c, isArticlesTab = false)
        assertEquals("the same origin must not be stacked twice in a row", listOf(a), nav.navigationHistory.toList())
    }

    @Test
    fun pushOrigin_with_null_origin_outside_the_articles_tab_records_nothing() {
        val nav = newNavState()
        nav.pushOrigin(origin = null, target = b, isArticlesTab = false)
        assertTrue(nav.navigationHistory.isEmpty())
    }

    @Test
    fun pushOrigin_with_null_origin_in_the_articles_tab_uses_the_top_visible_article() {
        chapterArticles = listOf(a, b)
        val nav = newNavState() // fresh list state -> firstVisibleItemIndex 0 -> derives `a`
        nav.pushOrigin(origin = null, target = c, isArticlesTab = true)
        assertEquals(listOf(a), nav.navigationHistory.toList())
    }

    @Test
    fun popBack_returns_and_removes_the_most_recent_entry() {
        val nav = newNavState()
        nav.navigationHistory.addAll(listOf(a, b))
        assertSame(b, nav.popBack())
        assertEquals(listOf(a), nav.navigationHistory.toList())
        assertSame(a, nav.popBack())
        assertTrue(nav.navigationHistory.isEmpty())
        assertNull("empty stack pops null", nav.popBack())
    }

    @Test
    fun trimHistoryAt_drops_every_entry_above_the_matched_one() {
        val nav = newNavState()
        nav.navigationHistory.addAll(listOf(a, b, c))
        nav.trimHistoryAt(b) // b is at index 1 -> keep [a]
        assertEquals(listOf(a), nav.navigationHistory.toList())
    }

    @Test
    fun trimHistoryAt_the_root_entry_clears_the_whole_stack() {
        val nav = newNavState()
        nav.navigationHistory.addAll(listOf(a, b))
        nav.trimHistoryAt(a) // a is at index 0 -> remove everything
        assertTrue(nav.navigationHistory.isEmpty())
    }

    @Test
    fun trimHistoryAt_an_article_not_on_the_stack_is_a_no_op() {
        val nav = newNavState()
        nav.navigationHistory.addAll(listOf(a, b))
        nav.trimHistoryAt(c)
        assertEquals(listOf(a, b), nav.navigationHistory.toList())
    }

    @Test
    fun trimHistoryAt_null_is_a_no_op() {
        val nav = newNavState()
        nav.navigationHistory.addAll(listOf(a, b))
        nav.trimHistoryAt(null)
        assertEquals(listOf(a, b), nav.navigationHistory.toList())
    }
}
