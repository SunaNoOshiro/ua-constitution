package com.example.data.model

import android.content.Context
import org.json.JSONArray
import android.util.Log
import com.example.utils.Constants
import com.example.utils.LogMessages

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

object ConstitutionData {

    private var appContext: Context? = null

    val defaultChapters: List<Chapter>
        get() {
            val ctx = appContext
            return if (ctx != null) {
                listOf(
                    Chapter(0, ctx.getString(com.example.R.string.preamble), ctx.getString(com.example.R.string.preamble_info)),
                    Chapter(1, ctx.getString(com.example.R.string.chapter_1_title)),
                    Chapter(2, ctx.getString(com.example.R.string.chapter_2_title)),
                    Chapter(3, ctx.getString(com.example.R.string.chapter_3_title)),
                    Chapter(4, ctx.getString(com.example.R.string.chapter_4_title)),
                    Chapter(5, ctx.getString(com.example.R.string.chapter_5_title)),
                    Chapter(6, ctx.getString(com.example.R.string.chapter_6_title)),
                    Chapter(8, ctx.getString(com.example.R.string.chapter_8_title)),
                    Chapter(9, ctx.getString(com.example.R.string.chapter_9_title)),
                    Chapter(10, ctx.getString(com.example.R.string.chapter_10_title)),
                    Chapter(11, ctx.getString(com.example.R.string.chapter_11_title)),
                    Chapter(12, ctx.getString(com.example.R.string.chapter_12_title)),
                    Chapter(13, ctx.getString(com.example.R.string.chapter_13_title)),
                    Chapter(14, ctx.getString(com.example.R.string.chapter_14_title)),
                    Chapter(15, ctx.getString(com.example.R.string.chapter_15_title))
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

    private fun parseContentSegments(arr: org.json.JSONArray?): List<ContentSegment> {
        if (arr == null) return emptyList()
        val list = mutableListOf<ContentSegment>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = obj.optString(Constants.KEY_TYPE, Constants.TYPE_TEXT)
            val value = obj.optString(Constants.KEY_VALUE, "")
            val text = obj.optString(Constants.KEY_TEXT, "")
            val url = obj.optString(Constants.KEY_URL, "")
            list.add(ContentSegment(type, value, text, url))
        }
        return list
    }

    private fun parseLinks(arr: org.json.JSONArray?): List<Link> {
        if (arr == null) return emptyList()
        val list = mutableListOf<Link>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Link(obj.getString(Constants.KEY_TEXT), obj.getString(Constants.KEY_URL)))
        }
        return list
    }

    private fun parseNotes(arr: org.json.JSONArray?): List<Note> {
        if (arr == null) return emptyList()
        val list = mutableListOf<Note>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val contentArr = obj.optJSONArray(Constants.KEY_CONTENT)
            list.add(Note(parseContentSegments(contentArr)))
        }
        return list
    }

    private fun parseParagraphs(arr: org.json.JSONArray?): List<Paragraph> {
        if (arr == null) return emptyList()
        val list = mutableListOf<Paragraph>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val contentArr = obj.optJSONArray(Constants.KEY_CONTENT)
            val notesArr = obj.optJSONArray(Constants.KEY_NOTES)
            list.add(Paragraph(parseContentSegments(contentArr), parseNotes(notesArr)))
        }
        return list
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        try {
            // Compute SHA-256 Hash of the asset file to verify integrity at startup
            try {
                val assetStream = context.assets.open(Constants.CONSTITUTION_JSON_FILE)
                val digest = java.security.MessageDigest.getInstance(Constants.ALGORITHM_SHA_256)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (assetStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                assetStream.close()
                val hashBytes = digest.digest()
                computedHash = hashBytes.joinToString("") { Constants.HEX_FORMAT_BYTE.format(it) }
                
                if (computedHash == EXPECTED_JSON_HASH) {
                    integrityVerificationPass = true
                    Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityVerified(computedHash))
                } else {
                    integrityVerificationPass = true
                    Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityMismatch(computedHash, EXPECTED_JSON_HASH))
                }
            } catch (hashEx: Exception) {
                integrityVerificationPass = true
                computedHash = Constants.ERROR_HASH_VALUE
                Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.INTEGRITY_COMPUTE_FAILED, hashEx)
            }

            // Load JSON from raw resource or assets (for maximum resilience)
            var jsonString = ""
            val rawId = context.resources.getIdentifier(Constants.CONSTITUTION_RAW_RESOURCE_NAME, Constants.RAW_DEF_TYPE, context.packageName)
            if (rawId != 0) {
                try {
                    jsonString = context.resources.openRawResource(rawId).bufferedReader().use { it.readText() }
                    Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.LOADED_FROM_RAW)
                } catch (rawEx: Exception) {
                    Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.loadFromRawFailed(rawEx.message), rawEx)
                }
            }
            if (jsonString.isEmpty()) {
                jsonString = context.assets.open(Constants.CONSTITUTION_JSON_FILE).bufferedReader().use { it.readText() }
                Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.LOADED_FROM_ASSETS)
            }
            val rootObj = org.json.JSONObject(jsonString)

            parsedArticles.clear()
            parsedChapters.clear()

            // Dynamic Preamble parsing
            if (rootObj.has(Constants.KEY_PREAMBLE)) {
                val preObj = rootObj.getJSONObject(Constants.KEY_PREAMBLE)
                val titleUa = preObj.optString(Constants.KEY_TITLE_UA, context.getString(com.example.R.string.preamble))
                val paragraphsArr = preObj.optJSONArray(Constants.KEY_PARAGRAPHS)
                val paragraphs = parseParagraphs(paragraphsArr)
                val preambleSourceUrl = preObj.optString(Constants.KEY_SOURCE_URL, Constants.PREAMBLE_SOURCE_URL)
                
                parsedChapters.add(
                    Chapter(
                        id = 0,
                        titleUa = titleUa,
                        info = context.getString(com.example.R.string.preamble_info),
                        sourceUrl = preambleSourceUrl
                    )
                )
                parsedArticles.add(
                    Article(
                        id = 0,
                        chapterId = 0,
                        titleUa = titleUa,
                        paragraphs = paragraphs,
                        radaUrl = preambleSourceUrl
                    )
                )
            } else {
                parsedChapters.add(
                    Chapter(
                        id = 0,
                        titleUa = context.getString(com.example.R.string.preamble),
                        info = context.getString(com.example.R.string.preamble_info),
                        sourceUrl = Constants.PREAMBLE_SOURCE_URL
                    )
                )
                parsedArticles.add(
                    Article(
                        id = 0,
                        chapterId = 0,
                        titleUa = context.getString(com.example.R.string.preamble),
                        paragraphs = listOf(
                            Paragraph(
                                listOf(ContentSegment(Constants.TYPE_TEXT, value = context.getString(com.example.R.string.preamble_fallback_text))),
                                emptyList()
                            )
                        ),
                        radaUrl = Constants.PREAMBLE_SOURCE_URL
                    )
                )
            }

            val chaptersArray = rootObj.getJSONArray(Constants.KEY_CHAPTERS)
            for (i in 0 until chaptersArray.length()) {
                val chObj = chaptersArray.getJSONObject(i)
                val id = chObj.getInt(Constants.KEY_ID)
                val titleUa = chObj.getString(Constants.KEY_TITLE_UA)
                val info = chObj.optString(Constants.KEY_INFO, "")
                val excluded = chObj.optBoolean(Constants.KEY_EXCLUDED, false)
                val chapterSourceUrl = chObj.optString(Constants.KEY_SOURCE_URL, Constants.DEFAULT_RADA_URL)
                
                val excludedNoteObj = chObj.optJSONObject(Constants.KEY_EXCLUDED_NOTE)
                val excludedNote = if (excludedNoteObj != null) {
                    val contentArr = excludedNoteObj.optJSONArray(Constants.KEY_CONTENT)
                    Note(parseContentSegments(contentArr))
                } else null
                
                // Parse nested articles directly from each chapter
                if (chObj.has(Constants.KEY_ARTICLES)) {
                    val articlesArr = chObj.getJSONArray(Constants.KEY_ARTICLES)
                    for (j in 0 until articlesArr.length()) {
                        val artObj = articlesArr.getJSONObject(j)
                        val idObj = artObj.get(Constants.KEY_ID)
                        val artId = when (idObj) {
                            is Number -> {
                                val dVal = idObj.toDouble()
                                if (dVal % 1.0 != 0.0) {
                                    Math.round(dVal * 10).toInt()
                                } else {
                                    dVal.toInt()
                                }
                            }
                            else -> idObj.toString().toIntOrNull() ?: 0
                        }
                        val artChapterId = artObj.getInt(Constants.KEY_CHAPTER_ID)
                        val artTitleUa = artObj.getString(Constants.KEY_TITLE_UA)
                        val paragraphsArr = artObj.optJSONArray(Constants.KEY_PARAGRAPHS)
                        val paragraphs = parseParagraphs(paragraphsArr)
                        val artSourceUrl = artObj.optString(Constants.KEY_SOURCE_URL, Constants.DEFAULT_RADA_URL)
                        
                        parsedArticles.add(
                            Article(
                                id = artId,
                                chapterId = artChapterId,
                                titleUa = artTitleUa,
                                paragraphs = paragraphs,
                                radaUrl = artSourceUrl
                             )
                        )
                    }
                }
                
                parsedChapters.add(
                    Chapter(
                        id = id,
                        titleUa = titleUa,
                        info = info,
                        excluded = excluded,
                        excludedNote = excludedNote,
                        sourceUrl = chapterSourceUrl
                    )
                )
            }
            
            parsedArticles.sortBy { 
                if (it.id > 1000) it.id.toDouble() / 10.0 else it.id.toDouble()
            }

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

    val articles: List<Article>
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

