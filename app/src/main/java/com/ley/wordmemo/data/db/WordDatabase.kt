package com.ley.wordmemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ley.wordmemo.data.model.Word

@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class WordDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

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