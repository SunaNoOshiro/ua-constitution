package ua.constitution.domain.content

import ua.constitution.data.model.Article

/**
 * Lookup of the parsed articles for a chapter.
 *
 * Segregated (ISP) from the raw [ConstitutionContentSource.articles] list so consumers that only
 * resolve a chapter's articles do not couple to search/iteration concerns.
 */
interface ArticleLookup {
    fun getArticlesForChapter(chapterId: Int): List<Article>
}
