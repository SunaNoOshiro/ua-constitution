package ua.constitution.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Article
import ua.constitution.data.model.Chapter
import ua.constitution.data.repository.BookmarkRepository
import ua.constitution.domain.content.ArticleLookup
import ua.constitution.domain.content.ChapterSource
import ua.constitution.domain.content.ConstitutionContentSource
import ua.constitution.domain.content.IntegrityStatus
import ua.constitution.domain.content.articleOfDayIndex
import ua.constitution.domain.content.searchArticles
import ua.constitution.domain.content.selectArticlesByBookmarkIds
import ua.constitution.domain.link.findArticleByLink
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConstitutionViewModel(
    private val repository: BookmarkRepository,
    private val contentSource: ConstitutionContentSource,
    private val chapterSource: ChapterSource,
    private val articleLookup: ArticleLookup,
    private val integrity: IntegrityStatus
) : ViewModel() {

    // --- Search & Exploration State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChapterId = MutableStateFlow<Int?>(null)
    val selectedChapterId: StateFlow<Int?> = _selectedChapterId.asStateFlow()

    // --- Bookmarks ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search functionality ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChapter(chapterId: Int?) {
        _selectedChapterId.value = if (_selectedChapterId.value == chapterId) null else chapterId
    }

    // Reactive list of Articles matching search query
    val filteredArticles: StateFlow<List<Article>> = _searchQuery
        .combine(_selectedChapterId) { query, chapterId ->
            searchArticles(contentSource.articles, query, chapterId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), contentSource.articles)

    // --- Content read accessors -----------------------------------------------------------------
    // Plain getters/functions (deliberately NOT StateFlows/derivedStateOf) so composables read
    // content through the ViewModel with the SAME recomposition timing as the previous direct
    // content-global reads. Each one moves former inline UI logic here verbatim (DIP).

    val chapters: List<Chapter>
        get() = chapterSource.chapters

    fun articlesForChapter(chapterId: Int): List<Article> =
        articleLookup.getArticlesForChapter(chapterId)

    /** Articles that are currently bookmarked, in article order. Delegates to the pure
     *  [selectArticlesByBookmarkIds]. */
    fun bookmarkedArticles(bookmarks: List<BookmarkEntity>): List<Article> =
        selectArticlesByBookmarkIds(contentSource.articles, bookmarks.map { it.articleId })

    /** The "article of the day" for a day-of-year seed, or null when there is no content (the
     *  caller supplies the localized UI fallback). Delegates to the pure [articleOfDayIndex]. */
    fun articleOfDay(dayOfYear: Int): Article? =
        articleOfDayIndex(dayOfYear, contentSource.articles.size)?.let { contentSource.articles[it] }

    /** Resolves cross-reference link text (e.g. "ст. 20") to an article via the content list. */
    fun resolveLink(text: String): Article? =
        findArticleByLink(text, contentSource.articles)

    // --- Initialization / integrity status (for the load-error banner) --------------------------
    val initializationError: String
        get() = integrity.initializationError
    val computedHash: String
        get() = integrity.computedHash
    val articlesEmpty: Boolean
        get() = contentSource.articles.isEmpty()
    val articlesCount: Int
        get() = contentSource.articles.size

    // --- Bookmarking & Study Notes ---
    fun toggleBookmark(articleId: Int) {
        viewModelScope.launch {
            val isBookmarked = bookmarks.value.any { it.articleId == articleId }
            if (isBookmarked) {
                repository.removeBookmark(articleId)
            } else {
                repository.addBookmark(articleId)
            }
        }
    }

    fun updateNotes(articleId: Int, notes: String) {
        viewModelScope.launch {
            repository.updateBookmarkNotes(articleId, notes)
        }
    }

    fun updateBookmarkEdits(articleId: Int, editsJson: String) {
        viewModelScope.launch {
            repository.updateBookmarkEdits(articleId, editsJson)
        }
    }
}

