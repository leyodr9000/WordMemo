package com.ley.wordmemo.data.util

import com.ley.wordmemo.data.api.ExtractedWord
import com.ley.wordmemo.data.model.Word
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File

/** 导出/备份：整库导出为 JSON，可再导入 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val words: List<ExportedWord> = emptyList(),
)

@Serializable
data class ExportedWord(
    val word: String,
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val meaning: String,
    val example: String = "",
    val exampleTranslation: String = "",
    val status: Int = 0,
    val reviewCount: Int = 0,
    val forgottenCount: Int = 0,
    val sourceBook: String = "",
)

object BackupHelper {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun export(words: List<Word>): String {
        val data = BackupData(words = words.map {
            ExportedWord(
                word = it.word, phonetic = it.phonetic, partOfSpeech = it.partOfSpeech,
                meaning = it.meaning, example = it.example, exampleTranslation = it.exampleTranslation,
                status = it.status, reviewCount = it.reviewCount, forgottenCount = it.forgottenCount,
                sourceBook = it.sourceBook,
            )
        })
        return json.encodeToString(BackupData.serializer(), data)
    }

    fun writeToFile(words: List<Word>, file: File): Boolean = runCatching {
        file.writeText(export(words))
    }.isSuccess

    fun parseBackup(text: String): List<Word> = runCatching {
        val data = json.decodeFromString(BackupData.serializer(), text)
        data.words.map { ex ->
            Word(
                word = ex.word, phonetic = ex.phonetic, partOfSpeech = ex.partOfSpeech,
                meaning = ex.meaning, example = ex.example, exampleTranslation = ex.exampleTranslation,
                status = ex.status, reviewCount = ex.reviewCount, forgottenCount = ex.forgottenCount,
                sourceBook = ex.sourceBook,
            )
        }
    }.getOrDefault(emptyList())

    fun parseExtracted(raw: String): List<Word> = runCatching {
        json.decodeFromString<List<ExtractedWord>>(raw).map { it.toEntity() }
    }.getOrDefault(emptyList())

    /** 解析"词书JSON"导入文件 */
    fun parseWordBook(raw: String): Pair<String, List<Word>> = runCatching {
        val root: JsonObject = json.parseToJsonElement(raw).jsonObject
        val bookName: String = root["book"]?.jsonPrimitive?.contentOrNull
            ?: root["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val wordsArr: JsonElement? = root["words"] ?: root["list"] ?: root["data"]
        val list: List<ExtractedWord> = when {
            wordsArr != null -> json.decodeFromJsonElement<List<ExtractedWord>>(wordsArr)
            else -> json.decodeFromString<List<ExtractedWord>>(raw)
        }
        bookName to list.map { it.toEntity(bookName) }
    }.getOrDefault("" to emptyList())
}