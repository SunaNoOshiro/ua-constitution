package ua.constitution.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.constitution.utils.Constants

@Entity(tableName = Constants.TABLE_BOOKMARKS)
data class BookmarkEntity(
    @PrimaryKey val articleId: Int,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val editsJson: String = ""
)
