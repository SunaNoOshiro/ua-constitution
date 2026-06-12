package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.utils.Constants

@Database(
    entities = [
        BookmarkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ConstitutionDatabase : RoomDatabase() {

    abstract fun constitutionDao(): ConstitutionDao

    companion object {
        @Volatile
        private var INSTANCE: ConstitutionDatabase? = null

        fun getDatabase(context: Context): ConstitutionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConstitutionDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
