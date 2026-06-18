package ua.constitution.domain.content

import ua.constitution.data.model.Article

/**
 * Pure article-selection helpers extracted from the ViewModel so the index/filter logic can be
 * unit-tested directly, without constructing a ViewModel. Behavior is identical to the former
 * inline ViewModel expressions.
 */

/** Index into an article list of size [articleCount] for the "article of the day" seeded by
 *  [dayOfYear], or null when there are no articles. Mirrors the former
 *  `if (articles.isNotEmpty()) dayOfYear % articles.size else null`. */
fun articleOfDayIndex(dayOfYear: Int, articleCount: Int): Int? =
    if (articleCount > 0) dayOfYear % articleCount else null

/** The articles whose bookmarkId is among [bookmarkArticleIds], preserving article order. Mirrors
 *  the former `articles.filter { a -> bookmarks.any { it.articleId == a.bookmarkId } }`. */
fun selectArticlesByBookmarkIds(articles: List<Article>, bookmarkArticleIds: Collection<Int>): List<Article> =
    articles.filter { it.bookmarkId in bookmarkArticleIds }
