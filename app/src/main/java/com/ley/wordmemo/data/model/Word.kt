package com.ley.wordmemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单词状态：生词 / 熟练 / 忘记
 */
enum class WordStatus(val dbValue: Int) {
    NEW(0),        // 生词
    MASTERED(1),   // 熟练
    FORGOTTEN(2);  // 忘记

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.dbValue == value } ?: NEW
    }
}

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,                // 单词（英文）
    val phonetic: String = "",       // 音标
    val partOfSpeech: String = "",   // 词性 vt./n. 等
    val meaning: String,             // 释义（中文）
    val example: String = "",        // 例句
    val exampleTranslation: String = "", // 例句翻译
    val status: Int = WordStatus.NEW.dbValue, // 生词/熟练/忘记
    val reviewCount: Int = 0,        // 复习次数
    val forgottenCount: Int = 0,     // 忘记次数
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceBook: String = ""      // 来源（书本名，可空）
)