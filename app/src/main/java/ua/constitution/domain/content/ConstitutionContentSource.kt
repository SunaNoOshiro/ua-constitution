package ua.constitution.domain.content

import ua.constitution.data.model.Article

/**
 * Read-only source of the parsed constitution articles.
 *
 * Consumers (e.g. the ViewModel) depend on this abstraction instead of the ConstitutionData
 * singleton (DIP). Kept to just what those consumers use (ISP) — loading, parsing and integrity
 * remain implementation details of the concrete source.
 */
interface ConstitutionContentSource {
    val articles: List<Article>
}
