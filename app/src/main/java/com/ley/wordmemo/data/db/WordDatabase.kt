package com.ley.wordmemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

        fun getInstance(context: Context): WordDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "wordmemo.db"
                ).build().also { INSTANCE = it }
            }
    }
}