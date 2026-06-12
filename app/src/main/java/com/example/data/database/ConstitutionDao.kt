package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.utils.Constants

@Dao
interface ConstitutionDao {

    // --- Bookmarks queries ---
    @Query("SELECT * FROM " + Constants.TABLE_BOOKMARKS + " ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM " + Constants.TABLE_BOOKMARKS + " WHERE articleId = :articleId LIMIT 1")
    fun getBookmarkByArticle(articleId: Int): Flow<BookmarkEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM " + Constants.TABLE_BOOKMARKS + " WHERE articleId = :articleId")
    suspend fun deleteBookmarkByArticleId(articleId: Int)

    @Query("UPDATE " + Constants.TABLE_BOOKMARKS + " SET notes = :notes WHERE articleId = :articleId")
    suspend fun updateBookmarkNotes(articleId: Int, notes: String)

    @Query("UPDATE " + Constants.TABLE_BOOKMARKS + " SET editsJson = :editsJson WHERE articleId = :articleId")
    suspend fun updateBookmarkEdits(articleId: Int, editsJson: String)
}
