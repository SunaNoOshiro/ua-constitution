package com.example.data.repository

import com.example.data.database.BookmarkEntity
import com.example.data.database.ConstitutionDao
import kotlinx.coroutines.flow.Flow

class ConstitutionRepository(private val dao: ConstitutionDao) {

    val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    fun getBookmarkByArticle(articleId: Int): Flow<BookmarkEntity?> =
        dao.getBookmarkByArticle(articleId)

    suspend fun addBookmark(articleId: Int, notes: String = "") {
        dao.insertBookmark(BookmarkEntity(articleId = articleId, notes = notes))
    }

    suspend fun removeBookmark(articleId: Int) {
        dao.deleteBookmarkByArticleId(articleId)
    }

    suspend fun updateBookmarkNotes(articleId: Int, notes: String) {
        dao.updateBookmarkNotes(articleId, notes)
    }

    suspend fun updateBookmarkEdits(articleId: Int, editsJson: String) {
        dao.updateBookmarkEdits(articleId, editsJson)
    }
}
