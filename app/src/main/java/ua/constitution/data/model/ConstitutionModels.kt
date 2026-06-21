package ua.constitution.data.model

import ua.constitution.utils.Constants

/**
 * The parsed constitution domain model types. These used to live in ConstitutionData.kt alongside
 * the mutable `object ConstitutionData`; that global was replaced by the immutable
 * [ConstitutionContent] store plus [ua.constitution.data.source.ConstitutionLoader].
 */

data class ContentSegment(
    val type: String,
    val value: String = "",
    val text: String = "",
    val url: String = ""
) {
    /** The text this segment contributes to the rendered/plain string: a link contributes its [text],
     *  everything else its [value]. The single home for the rule that was duplicated across the
     *  models and the reader composables. */
    val displayText: String
        get() = if (type == Constants.TYPE_LINK) text else value
}

data class Link(
    val text: String,
    val url: String
)

data class Note(
    val content: List<ContentSegment>
) {
    val text: String
        get() = content.joinToString("") { it.displayText }

    val links: List<Link>
        get() = content.filter { it.type == Constants.TYPE_LINK }.map { Link(it.text, it.url) }
}

data class Paragraph(
    val content: List<ContentSegment>,
    val notes: List<Note>
) {
    val text: String
        get() = content.joinToString("") { it.displayText }
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
