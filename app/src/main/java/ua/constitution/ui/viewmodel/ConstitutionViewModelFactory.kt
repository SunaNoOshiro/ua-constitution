package ua.constitution.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ua.constitution.data.repository.BookmarkRepository
import ua.constitution.domain.content.ArticleLookup
import ua.constitution.domain.content.ChapterSource
import ua.constitution.domain.content.ConstitutionContentSource
import ua.constitution.domain.content.IntegrityStatus

/**
 * Builds a [ConstitutionViewModel] from its injected, already-segregated dependencies. Extracted
 * from MainActivity's inline anonymous factory to separate composition (creating dependencies) from
 * factory wiring. The construction order and the unknown-ViewModel guard are preserved verbatim.
 */
class ConstitutionViewModelFactory(
    private val repository: BookmarkRepository,
    private val contentSource: ConstitutionContentSource,
    private val chapterSource: ChapterSource,
    private val articleLookup: ArticleLookup,
    private val integrity: IntegrityStatus
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConstitutionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConstitutionViewModel(
                repository,
                contentSource,
                chapterSource,
                articleLookup,
                integrity
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
