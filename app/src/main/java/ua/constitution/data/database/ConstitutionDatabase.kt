package ua.constitution.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room schema only. Building and caching the singleton instance lives in [DatabaseProvider].
 */
@Database(
    entities = [
        BookmarkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ConstitutionDatabase : RoomDatabase() {

    abstract fun constitutionDao(): ConstitutionDao
}
