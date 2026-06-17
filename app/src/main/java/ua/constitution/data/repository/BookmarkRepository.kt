package ua.constitution.data.repository

import kotlinx.coroutines.flow.Flow
import ua.constitution.data.database.BookmarkEntity

/**
 * Abstraction over bookmark persistence so the ViewModel depends on this contract rather than the
 * concrete Room-backed implementation (DIP). Kept narrow (ISP) — only what callers actually need.
 */
interface BookmarkRepository {
    val allBookmarks: Flow<List<BookmarkEntity>>
    fun getBookmarkByArticle(articleId: Int): Flow<BookmarkEntity?>
    suspend fun addBookmark(articleId: Int, notes: String = "")
    suspend fun removeBookmark(articleId: Int)
    suspend fun updateBookmarkNotes(articleId: Int, notes: String)
    suspend fun updateBookmarkEdits(articleId: Int, editsJson: String)
}
