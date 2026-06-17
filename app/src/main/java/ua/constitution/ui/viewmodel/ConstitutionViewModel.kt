package ua.constitution.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.model.Article
import ua.constitution.data.model.ConstitutionData
import ua.constitution.data.repository.ConstitutionRepository
import ua.constitution.domain.content.searchArticles
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConstitutionViewModel(private val repository: ConstitutionRepository) : ViewModel() {

    // --- Search & Exploration State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChapterId = MutableStateFlow<Int?>(null)
    val selectedChapterId: StateFlow<Int?> = _selectedChapterId.asStateFlow()

    private val _selectedArticle = MutableStateFlow<Article?>(null)
    val selectedArticle: StateFlow<Article?> = _selectedArticle.asStateFlow()

    // --- Bookmarks ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selection and Utility ---
    fun selectArticle(article: Article) {
        _selectedArticle.value = article
    }

    fun clearSelectedArticle() {
        _selectedArticle.value = null
    }

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
            searchArticles(ConstitutionData.articles, query, chapterId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConstitutionData.articles)

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

