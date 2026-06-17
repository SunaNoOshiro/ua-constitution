package ua.constitution.data.model

import android.content.Context
import android.util.Log
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages
import ua.constitution.domain.content.ConstitutionContentSource
import ua.constitution.data.source.ConstitutionJsonParser

data class ContentSegment(
    val type: String,
    val value: String = "",
    val text: String = "",
    val url: String = ""
)

data class Link(
    val text: String,
    val url: String
)

data class Note(
    val content: List<ContentSegment>
) {
    val text: String
        get() = content.joinToString("") { segment ->
            if (segment.type == Constants.TYPE_LINK) segment.text else segment.value
        }
    
    val links: List<Link>
        get() = content.filter { it.type == Constants.TYPE_LINK }.map { Link(it.text, it.url) }
}

data class Paragraph(
    val content: List<ContentSegment>,
    val notes: List<Note>
) {
    val text: String
        get() = content.joinToString("") { segment ->
            if (segment.type == Constants.TYPE_LINK) segment.text else segment.value
        }
}

data class Article(
    val id: Int,
    val chapterId: Int,
    val titleUa: String,
    val paragraphs: List<Paragraph>,
    val radaUrl: String = Constants.DEFAULT_RADA_URL
) {
    val textUa: String
        get() = paragraphs.joinToString("\n") { it.text }

    val bookmarkId: Int
        get() = id + chapterId * 10000
}

data class Chapter(
    val id: Int,
    val titleUa: String,
    val info: String = "",
    val excluded: Boolean = false,
    val excludedNote: Note? = null,
    val sourceUrl: String = ""
)

object ConstitutionData : ConstitutionContentSource {

    private var appContext: Context? = null

    val defaultChapters: List<Chapter>
        get() {
            val ctx = appContext
            return if (ctx != null) {
                listOf(
                    Chapter(0, ctx.getString(ua.constitution.R.string.preamble), ctx.getString(ua.constitution.R.string.preamble_info)),
                    Chapter(1, ctx.getString(ua.constitution.R.string.chapter_1_title)),
                    Chapter(2, ctx.getString(ua.constitution.R.string.chapter_2_title)),
                    Chapter(3, ctx.getString(ua.constitution.R.string.chapter_3_title)),
                    Chapter(4, ctx.getString(ua.constitution.R.string.chapter_4_title)),
                    Chapter(5, ctx.getString(ua.constitution.R.string.chapter_5_title)),
                    Chapter(6, ctx.getString(ua.constitution.R.string.chapter_6_title)),
                    Chapter(8, ctx.getString(ua.constitution.R.string.chapter_8_title)),
                    Chapter(9, ctx.getString(ua.constitution.R.string.chapter_9_title)),
                    Chapter(10, ctx.getString(ua.constitution.R.string.chapter_10_title)),
                    Chapter(11, ctx.getString(ua.constitution.R.string.chapter_11_title)),
                    Chapter(12, ctx.getString(ua.constitution.R.string.chapter_12_title)),
                    Chapter(13, ctx.getString(ua.constitution.R.string.chapter_13_title)),
                    Chapter(14, ctx.getString(ua.constitution.R.string.chapter_14_title)),
                    Chapter(15, ctx.getString(ua.constitution.R.string.chapter_15_title))
                )
            } else {
                listOf(
                    Chapter(0, Constants.FALLBACK_PREAMBLE_TITLE, Constants.FALLBACK_PREAMBLE_INFO),
                    Chapter(1, Constants.FALLBACK_CHAPTER_1_TITLE),
                    Chapter(2, Constants.FALLBACK_CHAPTER_2_TITLE),
                    Chapter(3, Constants.FALLBACK_CHAPTER_3_TITLE),
                    Chapter(4, Constants.FALLBACK_CHAPTER_4_TITLE),
                    Chapter(5, Constants.FALLBACK_CHAPTER_5_TITLE),
                    Chapter(6, Constants.FALLBACK_CHAPTER_6_TITLE),
                    Chapter(8, Constants.FALLBACK_CHAPTER_8_TITLE),
                    Chapter(9, Constants.FALLBACK_CHAPTER_9_TITLE),
                    Chapter(10, Constants.FALLBACK_CHAPTER_10_TITLE),
                    Chapter(11, Constants.FALLBACK_CHAPTER_11_TITLE),
                    Chapter(12, Constants.FALLBACK_CHAPTER_12_TITLE),
                    Chapter(13, Constants.FALLBACK_CHAPTER_13_TITLE),
                    Chapter(14, Constants.FALLBACK_CHAPTER_14_TITLE),
                    Chapter(15, Constants.FALLBACK_CHAPTER_15_TITLE)
                )
            }
        }

    const val EXPECTED_JSON_HASH = Constants.EXPECTED_JSON_HASH

    var integrityVerificationPass = false
        private set

    var computedHash = ""
        private set

    var usedFallback = false
        private set

    var initializationError = ""
        private set

    private var isInitialized = false
    private val parsedArticles = mutableListOf<Article>()
    private val parsedChapters = mutableListOf<Chapter>()

    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        val parser = ConstitutionJsonParser(context)
        try {
            val integrity = parser.computeIntegrity()
            computedHash = integrity.computedHash
            integrityVerificationPass = integrity.verificationPass

            val parsed = parser.parse(parser.loadJsonString())
            parsedArticles.clear()
            parsedArticles.addAll(parsed.articles)
            parsedChapters.clear()
            parsedChapters.addAll(parsed.chapters)

            isInitialized = true
            usedFallback = false
            Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.initSuccess(parsedArticles.size))
        } catch (e: Exception) {
            Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.INIT_ERROR, e)
            initializationError = "${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}"
            parsedArticles.clear()
            parsedChapters.clear()
            isInitialized = true
            usedFallback = true
        }
    }

    fun initializeForTests(articles: List<Article>, chapters: List<Chapter> = emptyList()) {
        parsedArticles.clear()
        parsedArticles.addAll(articles)
        parsedChapters.clear()
        parsedChapters.addAll(chapters)
        isInitialized = true
    }

    override val articles: List<Article>
        get() = parsedArticles

    val chapters: List<Chapter>
        get() = if (isInitialized && parsedChapters.isNotEmpty()) parsedChapters else defaultChapters

    fun getArticlesForChapter(chapterId: Int): List<Article> {
        return articles.filter { it.chapterId == chapterId }
    }

    fun getArticleById(id: Int): Article? {
        return articles.find { it.id == id } ?: articles.firstOrNull()
    }

    fun getRandomArticle(): Article {
        return articles.random()
    }
}

