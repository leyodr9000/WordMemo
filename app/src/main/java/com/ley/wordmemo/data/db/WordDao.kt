package com.ley.wordmemo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ley.wordmemo.data.model.BookStat
import com.ley.wordmemo.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: Int): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): Word?

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE status = :status")
    fun observeStatusCount(status: Int): Flow<Int>

    // ---- 单词书（sourceBook）管理 ----
    @Query("""
        SELECT COALESCE(NULLIF(sourceBook,''),'默认词库') AS book,
               COUNT(*) AS total,
               SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS newCount,
               SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS masteredCount,
               SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS forgottenCount,
               MAX(updatedAt) AS lastUpdated
        FROM words GROUP BY book ORDER BY lastUpdated DESC
    """)
    fun observeBooks(): Flow<List<BookStat>>

    @Query("SELECT * FROM words WHERE COALESCE(NULLIF(sourceBook,''),'默认词库') = :book ORDER BY createdAt DESC")
    fun observeByBook(book: String): Flow<List<Word>>

    @Query("SELECT DISTINCT COALESCE(NULLIF(sourceBook,''),'默认词库') FROM words ORDER BY sourceBook")
    suspend fun getBookNames(): List<String>

    @Query("SELECT COUNT(*) FROM words WHERE COALESCE(NULLIF(sourceBook,''),'默认词库') = :book")
    fun observeBookCount(book: String): Flow<Int>

    @Query("UPDATE words SET sourceBook = :newBook WHERE COALESCE(NULLIF(sourceBook,''),'默认词库') = :oldBook")
    suspend fun renameBook(oldBook: String, newBook: String)


    @Query("UPDATE words SET status = :status, updatedAt = :ts WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, ts: Long)

    @Query("DELETE FROM words WHERE COALESCE(NULLIF(sourceBook,''),'默认词库') = :book")
    suspend fun deleteBook(book: String)

    @Query("SELECT * FROM words WHERE status = :status ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomByStatus(status: Int, limit: Int = 20): List<Word>

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandom(limit: Int): List<Word>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    @Query("DELETE FROM words")
    suspend fun deleteAll()
}