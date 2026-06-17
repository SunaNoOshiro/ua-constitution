package ua.constitution.data.source

import android.content.Context
import android.util.Log
import ua.constitution.R
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.data.model.ContentSegment
import ua.constitution.data.model.Link
import ua.constitution.data.model.Note
import ua.constitution.data.model.Paragraph
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages

/** Result of the startup SHA-256 integrity check. */
data class IntegrityResult(val computedHash: String, val verificationPass: Boolean)

/** The parsed constitution: ordered articles and chapters. */
data class ParsedConstitution(val articles: List<Article>, val chapters: List<Chapter>)

/**
 * Loads and parses the constitution JSON (from raw resources or assets) and computes the asset
 * integrity hash. Extracted verbatim from ConstitutionData so loading/parsing is a single, testable
 * responsibility separate from the in-memory holder.
 */
class ConstitutionJsonParser(private val context: Context) {

    fun computeIntegrity(): IntegrityResult {
        return try {
            val assetStream = context.assets.open(Constants.CONSTITUTION_JSON_FILE)
            val digest = java.security.MessageDigest.getInstance(Constants.ALGORITHM_SHA_256)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (assetStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            assetStream.close()
            val hashBytes = digest.digest()
            val hash = hashBytes.joinToString("") { Constants.HEX_FORMAT_BYTE.format(it) }
            if (hash == Constants.EXPECTED_JSON_HASH) {
                Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityVerified(hash))
            } else {
                Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityMismatch(hash, Constants.EXPECTED_JSON_HASH))
            }
            IntegrityResult(hash, true)
        } catch (hashEx: Exception) {
            Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.INTEGRITY_COMPUTE_FAILED, hashEx)
            IntegrityResult(Constants.ERROR_HASH_VALUE, true)
        }
    }

    fun loadJsonString(): String {
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
        return jsonString
    }

    fun parse(jsonString: String): ParsedConstitution {
        val rootObj = org.json.JSONObject(jsonString)
        val articles = mutableListOf<Article>()
        val chapters = mutableListOf<Chapter>()

        // Dynamic Preamble parsing
        if (rootObj.has(Constants.KEY_PREAMBLE)) {
            val preObj = rootObj.getJSONObject(Constants.KEY_PREAMBLE)
            val titleUa = preObj.optString(Constants.KEY_TITLE_UA, context.getString(R.string.preamble))
            val paragraphsArr = preObj.optJSONArray(Constants.KEY_PARAGRAPHS)
            val paragraphs = parseParagraphs(paragraphsArr)
            val preambleSourceUrl = preObj.optString(Constants.KEY_SOURCE_URL, Constants.PREAMBLE_SOURCE_URL)

            chapters.add(
                Chapter(
                    id = 0,
                    titleUa = titleUa,
                    info = context.getString(R.string.preamble_info),
                    sourceUrl = preambleSourceUrl
                )
            )
            articles.add(
                Article(
                    id = 0,
                    chapterId = 0,
                    titleUa = titleUa,
                    paragraphs = paragraphs,
                    radaUrl = preambleSourceUrl
                )
            )
        } else {
            chapters.add(
                Chapter(
                    id = 0,
                    titleUa = context.getString(R.string.preamble),
                    info = context.getString(R.string.preamble_info),
                    sourceUrl = Constants.PREAMBLE_SOURCE_URL
                )
            )
            articles.add(
                Article(
                    id = 0,
                    chapterId = 0,
                    titleUa = context.getString(R.string.preamble),
                    paragraphs = listOf(
                        Paragraph(
                            listOf(ContentSegment(Constants.TYPE_TEXT, value = context.getString(R.string.preamble_fallback_text))),
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

                    articles.add(
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

            chapters.add(
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

        articles.sortBy {
            if (it.id > 1000) it.id.toDouble() / 10.0 else it.id.toDouble()
        }

        return ParsedConstitution(articles, chapters)
    }

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
}
