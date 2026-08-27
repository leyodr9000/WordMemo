package com.ley.wordmemo.data.api

import com.ley.wordmemo.data.model.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 识别/导入结果 */
@Serializable
data class ExtractedWord(
    @SerialName("word") val word: String = "",
    @SerialName("phonetic") val phonetic: String = "",
    @SerialName("partOfSpeech") val partOfSpeech: String = "",
    @SerialName("meaning") val meaning: String = "",
    @SerialName("example") val example: String = "",
    @SerialName("exampleTranslation") val exampleTranslation: String = "",
) {
    fun toEntity(sourceBook: String = ""): Word = Word(
        word = word.trim(),
        phonetic = phonetic.trim(),
        partOfSpeech = partOfSpeech.trim(),
        meaning = meaning.trim(),
        example = example.trim(),
        exampleTranslation = exampleTranslation.trim(),
        sourceBook = sourceBook,
    )
}

/** 兼容 OpenAI Chat Completions 的请求体 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 4000,
    val temperature: Double = 0.2,
)

@Serializable
data class ChatMessage(
    val role: String, // system | user
    val content: String,
)

/** OpenAI 风格响应（兼容多数聚合/自建服务） */
@Serializable
data class ChatResponse(
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val message: Message? = null,
        val text: String? = null, // 部分服务用 text 而非 message
    ) {
        @Serializable
        data class Message(val content: String = "")
    }
}

/** GET /models 响应（OpenAI 兼容） */
@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList(),
) {
    @Serializable
    data class ModelInfo(
        val id: String = "",
        val owned_by: String? = null,
    )
}

/** 解析 AI 返回文本中的 JSON 数组（容忍 markdown 代码块包裹） */
object AiParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseWords(raw: String): List<ExtractedWord> {
        val cleaned = extractJson(raw)
        return try {
            json.decodeFromString<List<ExtractedWord>>(cleaned)
                .filter { it.word.isNotBlank() && it.meaning.isNotBlank() }
        } catch (e: Exception) {
            // 尝试单个对象
            try {
                listOf(json.decodeFromString<ExtractedWord>(cleaned))
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private fun extractJson(raw: String): String {
        var s = raw.trim()
        // 去掉 ```json ... ``` 包裹
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```")
                .substringBefore("```").trim()
        }
        // 截取第一个 [ 到最后一个 ]
        val start = s.indexOf('[')
        val end = s.lastIndexOf(']')
        if (start >= 0 && end > start) return s.substring(start, end + 1)
        // 单对象：{ 到 }
        val oStart = s.indexOf('{')
        val oEnd = s.lastIndexOf('}')
        if (oStart >= 0 && oEnd > oStart) return s.substring(oStart, oEnd + 1)
        return s
    }
}