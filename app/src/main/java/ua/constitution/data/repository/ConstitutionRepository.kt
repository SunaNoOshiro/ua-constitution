package ua.constitution.data.repository

import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.database.ConstitutionDao
import kotlinx.coroutines.flow.Flow

class ConstitutionRepository(private val dao: ConstitutionDao) : BookmarkRepository {

    override val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    override suspend fun addBookmark(articleId: Int, notes: String) {
        dao.insertBookmark(BookmarkEntity(articleId = articleId, notes = notes))
    }

    override suspend fun removeBookmark(articleId: Int) {
        dao.deleteBookmarkByArticleId(articleId)
    }

    override suspend fun updateBookmarkNotes(articleId: Int, notes: String) {
        dao.updateBookmarkNotes(articleId, notes)
    }

    override suspend fun updateBookmarkEdits(articleId: Int, editsJson: String) {
        dao.updateBookmarkEdits(articleId, editsJson)
    }
}
