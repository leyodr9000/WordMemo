package com.ley.wordmemo.data.repository

import com.ley.wordmemo.data.db.ArticleDao
import com.ley.wordmemo.data.model.Article
import com.ley.wordmemo.data.model.WordTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
) {
    val articles: Flow<List<Article>> = articleDao.observeAll()
        .distinctUntilChanged()

    fun article(id: Long): Flow<Article?> = articleDao.observeById(id)

    suspend fun insert(article: Article): Long = articleDao.insert(article)

    suspend fun delete(id: Long) = articleDao.delete(id)

    suspend fun findTranslation(word: String): WordTranslation? = articleDao.findTranslation(word)

    suspend fun cacheTranslation(t: WordTranslation) = articleDao.cacheTranslation(t)

    suspend fun findTranslations(words: List<String>): List<WordTranslation> =
        if (words.isEmpty()) emptyList() else articleDao.findTranslations(words)
}
