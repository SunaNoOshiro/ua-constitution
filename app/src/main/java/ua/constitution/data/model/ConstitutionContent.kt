package ua.constitution.data.model

import ua.constitution.domain.content.ArticleLookup
import ua.constitution.domain.content.ChapterSource
import ua.constitution.domain.content.ConstitutionContentSource
import ua.constitution.domain.content.IntegrityStatus

/**
 * Immutable, loaded constitution content. Replaces the former mutable `object ConstitutionData`:
 * built once by [ua.constitution.data.source.ConstitutionLoader] and constructor-injected into the
 * ViewModel, so there is no process-wide mutable state (and tests need no reflection/seam).
 *
 * Implements the same four segregated read interfaces. Lookup behavior is moved verbatim from the
 * former object, including the pinned quirks (an unknown id falls back to the first article; chapters
 * fall back to the defaults when none were parsed).
 */
class ConstitutionContent(
    override val articles: List<Article>,
    private val parsedChapters: List<Chapter>,
    private val defaultChapters: List<Chapter>,
    override val integrityVerificationPass: Boolean,
    override val computedHash: String,
    override val usedFallback: Boolean,
    override val initializationError: String,
) : ConstitutionContentSource, ChapterSource, ArticleLookup, IntegrityStatus {

    override val chapters: List<Chapter>
        get() = if (parsedChapters.isNotEmpty()) parsedChapters else defaultChapters

    override fun getArticlesForChapter(chapterId: Int): List<Article> =
        articles.filter { it.chapterId == chapterId }

    override fun getArticleById(id: Int): Article? =
        articles.find { it.id == id } ?: articles.firstOrNull()

    override fun getRandomArticle(): Article = articles.random()
}
