package ua.constitution

import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.domain.content.ArticleLookup
import ua.constitution.domain.content.ChapterSource
import ua.constitution.domain.content.ConstitutionContentSource
import ua.constitution.domain.content.IntegrityStatus

/**
 * Reusable in-memory content source for tests.
 *
 * Implements the same segregated read interfaces as the production [ua.constitution.data.model.ConstitutionData]
 * object, and mirrors its lookup quirks in ONE place (notably [getArticleById] falling back to the
 * first article for an unknown id). Lets ViewModel/screen tests inject content without booting the
 * process-wide ConstitutionData global or resetting it via reflection.
 */
class FakeContentSource(
    override val articles: List<Article> = emptyList(),
    override val chapters: List<Chapter> = emptyList(),
    override val integrityVerificationPass: Boolean = true,
    override val computedHash: String = "",
    override val usedFallback: Boolean = false,
    override val initializationError: String = ""
) : ConstitutionContentSource, ChapterSource, ArticleLookup, IntegrityStatus {

    override fun getArticlesForChapter(chapterId: Int): List<Article> =
        articles.filter { it.chapterId == chapterId }

    // Mirrors ConstitutionData.getArticleById: an unknown id falls back to the first article.
    override fun getArticleById(id: Int): Article? =
        articles.find { it.id == id } ?: articles.firstOrNull()

    override fun getRandomArticle(): Article = articles.random()
}
