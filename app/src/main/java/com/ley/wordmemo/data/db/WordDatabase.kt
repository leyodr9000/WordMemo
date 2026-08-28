package com.ley.wordmemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ley.wordmemo.data.model.Article
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordTranslation

@Database(
    entities = [Word::class, Article::class, WordTranslation::class],
    version = 2,
    exportSchema = false,
)
abstract class WordDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: WordDatabase? = null

        /** v1 -> v2: 新增 articles / word_translations 两张表 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `articles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `word_translations` (
                        `word` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`word`)
                    )"""
                )
            }
        }

        fun getInstance(context: Context): WordDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "wordmemo.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}