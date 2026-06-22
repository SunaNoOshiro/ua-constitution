package ua.constitution.data.database

import android.content.Context
import androidx.room.Room
import ua.constitution.utils.Constants

/**
 * Owns construction of the process-wide [ConstitutionDatabase] (the Room builder + the volatile
 * singleton). Extracted out of the `@Database` schema class so that class is responsible only for
 * declaring the schema (SRP) — building/caching the instance is a separate concern that lives here.
 * Construction (flags, singleton semantics) is preserved verbatim.
 */
object DatabaseProvider {

    @Volatile
    private var INSTANCE: ConstitutionDatabase? = null

    fun getDatabase(context: Context): ConstitutionDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ConstitutionDatabase::class.java,
                Constants.DATABASE_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
