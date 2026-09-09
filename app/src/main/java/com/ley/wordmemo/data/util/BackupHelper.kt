package com.ley.wordmemo.data.util

import com.ley.wordmemo.data.api.ExtractedWord
import com.ley.wordmemo.data.model.Word
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
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

    /** 取对象中第一个非空字符串字段 (兼容网页版多种字段命名) */
    private fun str(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = (obj[k] as? JsonPrimitive)?.contentOrNull
            if (!v.isNullOrBlank()) return v.trim()
        }
        return ""
    }

    /**
     * 解析"词书JSON"导入文件 (兼容多种格式, 参考网页版导入):
     *  1. {"book"/"name": "...", "words"/"list"/"data": [...]}
     *  2. 顶层数组 [{word, phonetic, partOfSpeech/pos, meaning/definition, example, ...}]
     *     兼容网页版 definition 字段名与中文键名 (单词/音标/词性/释义)
     *  3. 顶层数组 ["apple", "banana", ...] (纯单词列表)
     */
    fun parseWordBook(raw: String): Pair<String, List<Word>> = runCatching {
        val root: JsonElement = json.parseToJsonElement(raw)
        var bookName = ""
        var arr: JsonArray? = null
        if (root is JsonObject) {
            bookName = str(root, "book", "name", "title")
            val wordsEl: JsonElement? = root["words"] ?: root["list"] ?: root["data"]
            if (wordsEl != null) arr = wordsEl.jsonArray
        } else {
            arr = root.jsonArray
        }
        val list: List<ExtractedWord> = arr?.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> ExtractedWord(word = el.content.trim(), meaning = "")
                is JsonObject -> ExtractedWord(
                    word = str(el, "word", "单词"),
                    phonetic = str(el, "phonetic", "音标"),
                    partOfSpeech = str(el, "partOfSpeech", "pos", "词性"),
                    meaning = str(el, "meaning", "definition", "释义", "中文", "中文释义", "翻译"),
                    example = str(el, "example", "例句"),
                    exampleTranslation = str(el, "exampleTranslation", "例句翻译"),
                )
                else -> null
            }
        } ?: emptyList()
        bookName to list.filter { it.word.isNotBlank() }.map { it.toEntity(bookName) }
    }.getOrDefault("" to emptyList())

    /**
     * 解析 CSV / TSV / 纯文本词书 (参考网页版文本导入 word,phonetic,definition[,unit]):
     *  - 支持 Tab 分隔 (Excel 直接粘贴) 与逗号分隔 (含引号转义)
     *  - 2 列: word + 释义(含中文) 或 音标
     *  - 3 列: word, phonetic, meaning
     *  - ≥4 列: word, phonetic, 词性(可选), meaning, 例句(可选)
     *  - 单列: 纯单词列表 (释义可由内置词典自动补全)
     */
    fun parseDelimited(raw: String): List<Word> {
        val out = mutableListOf<ExtractedWord>()
        val lines = raw.lines().map { it.trimStart('\ufeff') }.filter { it.isNotBlank() }
        for ((idx, line) in lines.withIndex()) {
            val cols = splitDelimited(line)
            if (cols.isEmpty()) continue
            // 跳过表头行
            if (idx == 0 && cols.size > 1) {
                val head = cols.joinToString(",").lowercase()
                if (listOf("word", "单词", "音标", "phonetic", "释义", "meaning", "definition", "翻译").any { head.contains(it) }) {
                    continue
                }
            }
            val w = cols[0].trim()
            if (w.isEmpty()) continue
            val c2 = cols.getOrNull(1)?.trim().orEmpty()
            val c3 = cols.getOrNull(2)?.trim().orEmpty()
            val c4 = cols.getOrNull(3)?.trim().orEmpty()
            val c5 = cols.getOrNull(4)?.trim().orEmpty()
            val entry = when {
                cols.size == 1 -> ExtractedWord(word = w)
                cols.size == 2 ->
                    // 含中文视为释义, 否则视为音标
                    if (c2.any { it.code > 0x4E00 && it.code < 0x9FFF }) ExtractedWord(word = w, meaning = c2)
                    else ExtractedWord(word = w, phonetic = c2)
                cols.size == 3 -> ExtractedWord(word = w, phonetic = c2, meaning = c3)
                else -> {
                    // 第3列疑似词性 (如 n. / vt. / adj.) 则识别
                    val pos = if (c3.matches(Regex("[a-zA-Z\\.\\s/]+")) && c3.length <= 8) c3 else ""
                    // 第4列若为 "Unit 1" 这类单元标记则不作为释义
                    val meaning = if (c4.matches(Regex("(?i)unit\\s*\\d+.*"))) "" else c4
                    ExtractedWord(
                        word = w, phonetic = c2, partOfSpeech = pos,
                        meaning = meaning, example = c5,
                    )
                }
            }
            out += entry
        }
        return out.map { it.toEntity() }
    }

    /** Tab 优先, 其次逗号 (支持引号包裹的 CSV 字段) */
    private fun splitDelimited(line: String): List<String> {
        val t = line.trim()
        if (t.isEmpty()) return emptyList()
        if (t.contains('\t')) return t.split('\t').map { it.trim() }
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in t) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { out += sb.toString().trim(); sb.clear() }
                else -> sb.append(ch)
            }
        }
        out += sb.toString().trim()
        return out.filter { it.isNotEmpty() || out.size > 1 }
    }
}