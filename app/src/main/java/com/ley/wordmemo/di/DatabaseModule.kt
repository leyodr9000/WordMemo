package com.ley.wordmemo.di

import android.content.Context
import com.ley.wordmemo.data.db.ArticleDao
import com.ley.wordmemo.data.db.WordDao
import com.ley.wordmemo.data.db.WordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WordDatabase =
        WordDatabase.getInstance(context)

    @Provides
    fun provideWordDao(db: WordDatabase): WordDao = db.wordDao()

    @Provides
    fun provideArticleDao(db: WordDatabase): ArticleDao = db.articleDao()
}