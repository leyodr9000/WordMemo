package com.ley.wordmemo.data.reader

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 离线英汉词典：从 assets/dict_base.json 加载, 内存 Map 查询 */
object OfflineDict {

    @Serializable
    private data class DictEntry(val w: String = "", val m: String = "")

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var loaded: Map<String, String>? = null

    fun ensureLoaded(context: Context) {
        if (loaded != null) return
        synchronized(this) {
            if (loaded != null) return
            val map = HashMap<String, String>()
            try {
                val raw = context.assets.open("dict_base.json").bufferedReader().use { it.readText() }
                val arr = json.parseToJsonElement(raw).jsonArray
                for (el in arr) {
                    val obj = el.jsonObject
                    val w = obj["w"]?.jsonPrimitive?.contentOrNull ?: continue
                    val m = obj["m"]?.jsonPrimitive?.contentOrNull ?: continue
                    map[w.lowercase()] = m
                }
            } catch (e: Exception) {
                // 词典加载失败不阻塞
            }
            loaded = map
        }
    }

    /** 查单词释义（归一化小写/去词形变化） */
    fun lookup(word: String): String? {
        val d = loaded ?: return null
        val low = word.trim().lowercase().trim(',', '.', ';', ':', '!', '?', '"', '(', ')', 's')
        d[word.trim().lowercase()]?.let { return it }
        d[low]?.let { return it }
        // 简单词形: 去 s/es/ed/ing
        val base = stem(word)
        d[base]?.let { return it }
        return null
    }

    private fun stem(word: String): String {
        var w = word.lowercase()
        if (w.endsWith("ies") && w.length > 4) return w.dropLast(3) + "y"
        if (w.endsWith("es")) return w.dropLast(2)
        if (w.endsWith("ing") && w.length > 5) return w.dropLast(3)
        if (w.endsWith("ed") && w.length > 4) return w.dropLast(2)
        if (w.endsWith("s") && w.length > 3) return w.dropLast(1)
        return w
    }

    /** 离线整句翻译：逐词查词典拼接（朴素模式, 大量词时保留原文） */
    fun translateSentenceOffline(context: Context, sentence: String): String {
        ensureLoaded(context)
        val d = loaded ?: return ""
        val words = sentence.split(Regex("\\s+"))
        val parts = words.map { w ->
            val clean = w.trim().trim(',', '.', ';', ':', '!', '?', '"', '(', ')')
            val m = lookup(clean)
            if (m != null && m.isNotBlank()) m else clean
        }
        return parts.joinToString(" ")
    }
}