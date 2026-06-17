package ua.constitution

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ua.constitution.data.database.BookmarkEntity
import ua.constitution.data.database.ConstitutionDao

/**
 * In-memory fake of [ConstitutionDao] for fast, deterministic repository / ViewModel tests.
 *
 * It mirrors the contract the real Room DAO enforces (verified independently by [BookmarkDaoTest]):
 *  - insert REPLACEs any existing row with the same articleId (articleId is the primary key),
 *  - the list is ordered by bookmarkedAt DESC,
 *  - updates / deletes targeting a missing articleId are silent no-ops (no insert, no error).
 *
 * The two update methods are fully implemented here (the earlier ad-hoc fake left them empty),
 * so delegation of [ConstitutionRepository.updateBookmarkNotes] / updateBookmarkEdits is observable.
 */
class FakeConstitutionDao : ConstitutionDao {

    val bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())

    private fun publish(list: List<BookmarkEntity>) {
        bookmarks.value = list.sortedByDescending { it.bookmarkedAt }
    }

    override fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarks

    override fun getBookmarkByArticle(articleId: Int): Flow<BookmarkEntity?> =
        bookmarks.map { list -> list.find { it.articleId == articleId } }

    override suspend fun insertBookmark(bookmark: BookmarkEntity) {
        // REPLACE on conflict: drop any existing row for this articleId, then add the new one.
        publish(bookmarks.value.filterNot { it.articleId == bookmark.articleId } + bookmark)
    }

    override suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        publish(bookmarks.value.filterNot { it.articleId == bookmark.articleId })
    }

    override suspend fun deleteBookmarkByArticleId(articleId: Int) {
        publish(bookmarks.value.filterNot { it.articleId == articleId })
    }

    override suspend fun updateBookmarkNotes(articleId: Int, notes: String) {
        // UPDATE ... WHERE articleId = :articleId — affects 0 rows (no-op) if the row is absent.
        publish(bookmarks.value.map { if (it.articleId == articleId) it.copy(notes = notes) else it })
    }

    override suspend fun updateBookmarkEdits(articleId: Int, editsJson: String) {
        publish(bookmarks.value.map { if (it.articleId == articleId) it.copy(editsJson = editsJson) else it })
    }
}
