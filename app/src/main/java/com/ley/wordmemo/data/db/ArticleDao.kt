package com.ley.wordmemo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ley.wordmemo.data.model.Article
import com.ley.wordmemo.data.model.WordTranslation
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    fun observeById(id: Long): Flow<Article?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: Article): Long

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun delete(id: Long)

    // ---- 单词翻译缓存 ----
    @Query("SELECT * FROM word_translations WHERE word = :word")
    suspend fun findTranslation(word: String): WordTranslation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheTranslation(t: WordTranslation)

    /** 批量查询缓存（一次查 50 个词） */
    @Query("SELECT * FROM word_translations WHERE word IN (:words)")
    suspend fun findTranslations(words: List<String>): List<WordTranslation>
}