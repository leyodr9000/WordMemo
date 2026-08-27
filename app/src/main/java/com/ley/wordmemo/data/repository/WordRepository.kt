package com.ley.wordmemo.data.repository

import com.ley.wordmemo.data.db.WordDao
import com.ley.wordmemo.data.model.BookStat
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    val allWords: Flow<List<Word>> = wordDao.observeAll().distinctUntilChanged()
    val totalCount: Flow<Int> = wordDao.observeTotalCount().distinctUntilChanged()

    fun wordsByStatus(status: WordStatus): Flow<List<Word>> =
        wordDao.observeByStatus(status.dbValue).distinctUntilChanged()

    fun countByStatus(status: WordStatus): Flow<Int> =
        wordDao.observeStatusCount(status.dbValue).distinctUntilChanged()

    fun search(query: String): Flow<List<Word>> = wordDao.search(query).distinctUntilChanged()

    suspend fun getById(id: Long): Word? = wordDao.getById(id)

    suspend fun insert(word: Word): Long = wordDao.insert(word)

    suspend fun insertAll(words: List<Word>) = wordDao.insertAll(words)

    suspend fun update(word: Word) = wordDao.update(word)

    /** 快速变更词状态(列表/卡片用), 同时刷新 updatedAt */
    suspend fun setStatus(id: Long, status: com.ley.wordmemo.data.model.WordStatus) =
        wordDao.updateStatus(id, status.dbValue, System.currentTimeMillis())

    suspend fun delete(word: Word) = wordDao.delete(word)

    suspend fun deleteAll() = wordDao.deleteAll()

    /** 标记状态并更新复习计数（简化间隔重复：熟练+1，忘记+1） */
    suspend fun markStatus(word: Word, status: WordStatus): Word {
        val now = System.currentTimeMillis()
        val updated = when (status) {
            WordStatus.MASTERED -> word.copy(
                status = WordStatus.MASTERED.dbValue,
                reviewCount = word.reviewCount + 1,
                updatedAt = now
            )
            WordStatus.FORGOTTEN -> word.copy(
                status = WordStatus.FORGOTTEN.dbValue,
                forgottenCount = word.forgottenCount + 1,
                updatedAt = now
            )
            WordStatus.NEW -> word.copy(
                status = WordStatus.NEW.dbValue,
                updatedAt = now
            )
        }
        wordDao.update(updated)
        return updated
    }

    /** 复习队列：生词+忘记优先，随机取 */
    suspend fun getStudyQueue(limit: Int = 30): List<Word> {
        val forgotten = wordDao.getRandomByStatus(WordStatus.FORGOTTEN.dbValue, limit / 2)
        val fresh = wordDao.getRandomByStatus(WordStatus.NEW.dbValue, limit / 2)
        val combined = (forgotten + fresh).distinctBy { it.id }
        if (combined.size < limit) {
            val more = wordDao.getRandom(limit).filter { w -> combined.none { it.id == w.id } }
            return (combined + more).take(limit)
        }
        return combined.take(limit)
    }

    // ============ 单词书管理 ============

    val books: Flow<List<BookStat>> = wordDao.observeBooks().distinctUntilChanged()

    fun wordsByBook(book: String): Flow<List<Word>> = wordDao.observeByBook(book).distinctUntilChanged()

    fun bookCount(book: String): Flow<Int> = wordDao.observeBookCount(book).distinctUntilChanged()

    suspend fun getBookNames(): List<String> = wordDao.getBookNames()

    suspend fun renameBook(oldBook: String, newBook: String) {
        if (oldBook != newBook && newBook.isNotBlank()) wordDao.renameBook(oldBook, newBook)
    }

    suspend fun deleteBook(book: String) = wordDao.deleteBook(book)

    suspend fun createEmptyBook(name: String): Boolean {
        if (name.isBlank()) return false
        val names = wordDao.getBookNames()
        if (names.contains(name)) return false
        // 用一条占位词创建词书目录（避免空书无法出现在列表）
        wordDao.insert(
            Word(
                word = "__PLACEHOLDER__",
                meaning = "",
                sourceBook = name,
            )
        )
        return true
    }
}