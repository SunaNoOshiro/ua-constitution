package ua.constitution.domain.content

import ua.constitution.data.model.Article

/**
 * Pure article search/filter rule used by the reader, extracted verbatim from the ViewModel.
 *
 * An empty query returns everything, optionally scoped to [chapterId]. A non-empty query performs a
 * GLOBAL search (matching id exactly or as a substring, or the title/body case-insensitively) and
 * ignores the chapter constraint entirely.
 */
fun searchArticles(all: List<Article>, query: String, chapterId: Int?): List<Article> =
    all.filter { matchesQuery(it, query) && matchesChapter(it, query, chapterId) }

/** An empty query matches everything; otherwise the id (exact or substring) or the title/body. */
private fun matchesQuery(article: Article, query: String): Boolean =
    query.isEmpty() ||
        article.id.toString() == query ||
        article.id.toString().contains(query) ||
        article.titleUa.contains(query, ignoreCase = true) ||
        article.textUa.contains(query, ignoreCase = true)

/** A non-empty query searches globally (ignores the chapter); otherwise scope to [chapterId]. */
private fun matchesChapter(article: Article, query: String, chapterId: Int?): Boolean =
    query.isNotEmpty() || chapterId == null || article.chapterId == chapterId
