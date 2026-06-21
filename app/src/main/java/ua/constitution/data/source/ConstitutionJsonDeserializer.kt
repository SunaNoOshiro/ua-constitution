package ua.constitution.data.source

import android.content.Context
import ua.constitution.R
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.data.model.ContentSegment
import ua.constitution.data.model.Link
import ua.constitution.data.model.Note
import ua.constitution.data.model.Paragraph
import ua.constitution.domain.text.ArticleNumberFormatter
import ua.constitution.utils.Constants

/** The parsed constitution: ordered articles and chapters. */
data class ParsedConstitution(val articles: List<Article>, val chapters: List<Chapter>)

/**
 * Maps the constitution JSON string to the domain model (ordered articles + chapters). Single
 * responsibility: JSON -> domain deserialization, with NO file IO and NO integrity/crypto. Extracted
 * verbatim from `ConstitutionJsonParser.parse()` (and its private helpers); still takes a [Context]
 * only to resolve the localized preamble strings — a future StringProvider seam. The work is split
 * into small per-section helpers to keep each function within the cyclomatic-complexity budget.
 */
class ConstitutionJsonDeserializer(private val context: Context) {

    fun deserialize(jsonString: String): ParsedConstitution {
        val rootObj = org.json.JSONObject(jsonString)
        val articles = mutableListOf<Article>()
        val chapters = mutableListOf<Chapter>()

        addPreamble(rootObj, chapters, articles)

        val chaptersArray = rootObj.getJSONArray(Constants.KEY_CHAPTERS)
        for (i in 0 until chaptersArray.length()) {
            val chObj = chaptersArray.getJSONObject(i)
            articles.addAll(parseChapterArticles(chObj))
            chapters.add(parseChapter(chObj))
        }

        articles.sortBy { ArticleNumberFormatter.sortKey(it.id) }

        return ParsedConstitution(articles, chapters)
    }

    /** Adds the preamble chapter + article (id 0): from the JSON `preamble` object, or a localized
     *  fallback when it is absent. */
    private fun addPreamble(rootObj: org.json.JSONObject, chapters: MutableList<Chapter>, articles: MutableList<Article>) {
        if (rootObj.has(Constants.KEY_PREAMBLE)) {
            val preObj = rootObj.getJSONObject(Constants.KEY_PREAMBLE)
            val titleUa = preObj.optString(Constants.KEY_TITLE_UA, context.getString(R.string.preamble))
            val paragraphs = parseParagraphs(preObj.optJSONArray(Constants.KEY_PARAGRAPHS))
            val preambleSourceUrl = preObj.optString(Constants.KEY_SOURCE_URL, Constants.PREAMBLE_SOURCE_URL)
            chapters.add(Chapter(id = 0, titleUa = titleUa, info = context.getString(R.string.preamble_info), sourceUrl = preambleSourceUrl))
            articles.add(Article(id = 0, chapterId = 0, titleUa = titleUa, paragraphs = paragraphs, radaUrl = preambleSourceUrl))
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
    }

    /** Parses a chapter object (without its nested articles, which [parseChapterArticles] handles). */
    private fun parseChapter(chObj: org.json.JSONObject): Chapter {
        val excludedNoteObj = chObj.optJSONObject(Constants.KEY_EXCLUDED_NOTE)
        val excludedNote = if (excludedNoteObj != null) {
            Note(parseContentSegments(excludedNoteObj.optJSONArray(Constants.KEY_CONTENT)))
        } else null
        return Chapter(
            id = chObj.getInt(Constants.KEY_ID),
            titleUa = chObj.getString(Constants.KEY_TITLE_UA),
            info = chObj.optString(Constants.KEY_INFO, ""),
            excluded = chObj.optBoolean(Constants.KEY_EXCLUDED, false),
            excludedNote = excludedNote,
            sourceUrl = chObj.optString(Constants.KEY_SOURCE_URL, Constants.DEFAULT_RADA_URL)
        )
    }

    /** The articles nested directly under a chapter object, in JSON order (empty if none). */
    private fun parseChapterArticles(chObj: org.json.JSONObject): List<Article> {
        if (!chObj.has(Constants.KEY_ARTICLES)) return emptyList()
        val articlesArr = chObj.getJSONArray(Constants.KEY_ARTICLES)
        val list = mutableListOf<Article>()
        for (j in 0 until articlesArr.length()) {
            val artObj = articlesArr.getJSONObject(j)
            list.add(
                Article(
                    id = toArticleId(artObj.get(Constants.KEY_ID)),
                    chapterId = artObj.getInt(Constants.KEY_CHAPTER_ID),
                    titleUa = artObj.getString(Constants.KEY_TITLE_UA),
                    paragraphs = parseParagraphs(artObj.optJSONArray(Constants.KEY_PARAGRAPHS)),
                    radaUrl = artObj.optString(Constants.KEY_SOURCE_URL, Constants.DEFAULT_RADA_URL)
                )
            )
        }
        return list
    }

    /** Encodes a JSON article id via [ArticleNumberFormatter.encode] (fractional 16.1 -> 16001, whole
     *  -> its int); a non-numeric value falls back to 0. */
    private fun toArticleId(idObj: Any): Int = when (idObj) {
        is Number -> ArticleNumberFormatter.encode(idObj.toDouble())
        else -> idObj.toString().toIntOrNull() ?: 0
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
