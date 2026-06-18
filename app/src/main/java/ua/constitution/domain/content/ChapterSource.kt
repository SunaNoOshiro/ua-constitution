package ua.constitution.domain.content

import ua.constitution.data.model.Chapter

/**
 * Read-only source of the constitution's chapters.
 *
 * Segregated from [ConstitutionContentSource] (ISP) so a consumer that only needs the chapter
 * list does not depend on the full article list, and vice-versa. The concrete source decides
 * whether to serve parsed chapters or a default fallback.
 */
interface ChapterSource {
    val chapters: List<Chapter>
}
