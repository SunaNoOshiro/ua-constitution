package ua.constitution.domain.content

import ua.constitution.data.model.Article

/**
 * Lookups over the parsed articles: by chapter, by id, and a random pick.
 *
 * Segregated (ISP) from the raw [ConstitutionContentSource.articles] list so consumers that only
 * resolve individual articles do not couple to search/iteration concerns. Lookup quirks (e.g.
 * an unknown id falling back to the first article) are implementation details of the concrete
 * source and are pinned by its tests.
 */
interface ArticleLookup {
    fun getArticlesForChapter(chapterId: Int): List<Article>
    fun getArticleById(id: Int): Article?
    fun getRandomArticle(): Article
}
