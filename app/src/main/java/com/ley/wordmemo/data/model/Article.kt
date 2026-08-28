package com.ley.wordmemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 阅读文章（考研英语阅读短文等） */
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",     // 原文（英文段落，按句分割可解析）
    val source: String = "内置",   // 内置 | AI翻译 | 拍照导入 | 手动粘贴
    val createdAt: Long = System.currentTimeMillis(),
)

/** 单词翻译缓存（点词翻译气泡用，避免重复请求 AI） */
@Entity(tableName = "word_translations")
data class WordTranslation(
    @PrimaryKey
    val word: String = "",
    val meaning: String = "",     // 中文释义
    val createdAt: Long = System.currentTimeMillis(),
)