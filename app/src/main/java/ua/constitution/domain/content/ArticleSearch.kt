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
    all.filter { article ->
        val matchesQuery = query.isEmpty() ||
                article.id.toString() == query ||
                article.id.toString().contains(query) ||
                article.titleUa.contains(query, ignoreCase = true) ||
                article.textUa.contains(query, ignoreCase = true)

        // If there is an active search query, perform global search (ignore chapter constraint)
        val matchesChapter = query.isNotEmpty() || chapterId == null || article.chapterId == chapterId
        matchesQuery && matchesChapter
    }
