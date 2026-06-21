package ua.constitution.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ua.constitution.data.model.Article

/**
 * Hoisted navigation/scroll state for the dashboard ARTICLES experience. It owns the cross-article
 * back-stack and the article-list scroll coordination that used to live as five loose `var`s/lists
 * captured by closures across the BackHandler, the bottom nav, the chapter picker and the inline
 * ARTICLES branch of [MainAppDashboard].
 *
 * Behaviour is preserved verbatim: the list state, coroutine scope and the article-lookup are
 * injected (created with the same `rememberLazyListState()`/`rememberCoroutineScope()` as before) and
 * the history mutations are the exact decisions that were inlined. Pulling them onto a plain class
 * makes the previously-untestable back-stack logic ([pushOrigin]/[popBack]/[trimHistoryAt]) directly
 * unit-testable, independent of the scroll machinery.
 */
class DashboardNavState(
    val articlesListState: LazyListState,
    private val scope: CoroutineScope,
    private val articlesForChapter: (Int) -> List<Article>,
) {
    /** The chapter whose article list is shown (defaults to chapter 1, as before). */
    var currentSelectedChapterId by mutableStateOf(1)

    /** The article index the user explicitly jumped to, overriding the scroll-derived active index. */
    var clickedArticleIndex by mutableStateOf<Int?>(null)

    /** While true, an in-flight programmatic scroll must not reset [clickedArticleIndex]. */
    var ignoreScrollActiveIndexSetting by mutableStateOf(false)

    /** The cross-article back-stack: origins pushed when the reader follows a link to another article. */
    val navigationHistory = mutableStateListOf<Article>()

    /**
     * Records the origin article (if any) on the back-stack before navigating to [target]. Mirrors the
     * former inline decision in `navigateToArticleWithOrigin`: when no explicit origin is given and the
     * ARTICLES tab is active, the origin is the article currently at the top of the list. The origin is
     * pushed only when it differs from the target and is not already the most recent entry.
     */
    fun pushOrigin(origin: Article?, target: Article, isArticlesTab: Boolean) {
        val current = origin ?: if (isArticlesTab) {
            articlesForChapter(currentSelectedChapterId).getOrNull(articlesListState.firstVisibleItemIndex)
        } else {
            null
        }
        if (current != null && current.bookmarkId != target.bookmarkId) {
            if (navigationHistory.isEmpty() || navigationHistory.last().bookmarkId != current.bookmarkId) {
                navigationHistory.add(current)
            }
        }
    }

    /** Selects [target]'s chapter and scrolls the list to it. The shared body of the three former
     *  verbatim "pop and scroll" blocks (BackHandler, navigate-tail, back button). */
    fun popToArticle(target: Article) {
        currentSelectedChapterId = target.chapterId
        scope.launch {
            val chapterArticles = articlesForChapter(target.chapterId)
            val index = chapterArticles.indexOfFirst { it.bookmarkId == target.bookmarkId }
            if (index >= 0) {
                ignoreScrollActiveIndexSetting = true
                clickedArticleIndex = index
                articlesListState.animateScrollToItem(index)
            }
        }
    }

    /** Pops and returns the most recent back-stack entry, or null when the stack is empty. */
    fun popBack(): Article? = navigationHistory.removeLastOrNull()

    /** Jumps the list to [index] (the quick-links chip tap), suppressing the scroll-derived active index. */
    fun jumpToArticleIndex(index: Int) {
        ignoreScrollActiveIndexSetting = true
        clickedArticleIndex = index
        scope.launch {
            articlesListState.animateScrollToItem(index)
        }
    }

    /** When the visible article is already an ancestor on the back-stack, drop everything above it
     *  (the user scrolled/navigated back to it without using the back button). */
    fun trimHistoryAt(currentArticle: Article?) {
        if (currentArticle == null || navigationHistory.isEmpty()) return
        val indexInHistory = navigationHistory.indexOfFirst { it.bookmarkId == currentArticle.bookmarkId }
        if (indexInHistory >= 0) {
            while (navigationHistory.size > indexInHistory) {
                navigationHistory.removeLastOrNull()
            }
        }
    }
}
