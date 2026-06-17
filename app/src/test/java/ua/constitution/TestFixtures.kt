package ua.constitution

import ua.constitution.data.model.Article
import ua.constitution.data.model.ContentSegment
import ua.constitution.data.model.Note
import ua.constitution.data.model.Paragraph
import ua.constitution.utils.Constants

/**
 * Concise builders for test fixtures.
 *
 * These exist purely to keep the characterization tests readable — each test states only the
 * fields it actually cares about. They construct the real production model types, so they capture
 * current behavior without introducing any test-only seam in production code.
 */

fun textSegment(value: String): ContentSegment =
    ContentSegment(type = Constants.TYPE_TEXT, value = value)

fun linkSegment(text: String, url: String): ContentSegment =
    ContentSegment(type = Constants.TYPE_LINK, text = text, url = url)

fun paragraphOf(vararg segments: ContentSegment, notes: List<Note> = emptyList()): Paragraph =
    Paragraph(content = segments.toList(), notes = notes)

fun noteOf(vararg segments: ContentSegment): Note =
    Note(content = segments.toList())

/**
 * Builds an [Article]. Defaults to chapter 1 and a "Стаття N" title so call sites only specify
 * what matters. radaUrl is left empty for determinism (the production default is a fixed URL that
 * none of these tests assert on).
 */
fun articleOf(
    id: Int,
    chapterId: Int = 1,
    titleUa: String = "Стаття $id",
    paragraphs: List<Paragraph> = emptyList(),
    radaUrl: String = ""
): Article = Article(
    id = id,
    chapterId = chapterId,
    titleUa = titleUa,
    paragraphs = paragraphs,
    radaUrl = radaUrl
)
