package com.luntikius.wallet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luntikius.wallet.data.model.Pass

/**
 * Room database for storing passes.
 * Singleton pattern ensures single database instance.
 */
@Database(
    entities = [Pass::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(PassTypeConverters::class)
abstract class PassDatabase : RoomDatabase() {
    abstract fun passDao(): PassDao

    companion object {
        @Volatile
        private var INSTANCE: PassDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new image path columns
                database.execSQL("ALTER TABLE passes ADD COLUMN logoPath TEXT")
                database.execSQL("ALTER TABLE passes ADD COLUMN stripPath TEXT")
                database.execSQL("ALTER TABLE passes ADD COLUMN backgroundPath TEXT")
            }
        }

        fun getInstance(context: Context): PassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PassDatabase::class.java,
                    "pass_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
