package com.ley.wordmemo.data.api

import com.ley.wordmemo.data.settings.AppSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 可自定义的 AI 识图客户端。
 * 兼容 OpenAI Chat Completions API（含图片 content），
 * 用户可在设置中配置任意 baseUrl / key / model。
 */
@Singleton
class AiClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    /**
     * 调用视觉模型，把图片转成单词列表。
     * 请求体参照 OpenAI messages + image_url, 兼容大多数兼容服务。
     */
    suspend fun extractWordsFromImage(
        settings: AppSettings,
        imageFile: File,
        mimeType: String = "image/jpeg",
    ): List<ExtractedWord> {
        require(settings.isApiConfigured) { "请先在设置中配置 AI API" }

        val baseUrl = settings.apiBaseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl
        else "$baseUrl/chat/completions"

        // 图片转 base64 data url
        val base64 = imageFile.readBytes().let {
            android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
        }
        val dataUrl = "data:$mimeType;base64,$base64"

        val body = ChatRequest(
            model = settings.apiModel,
            maxTokens = 4000,
            temperature = 0.2,
            messages = listOf(
                ChatMessage(role = "system", content = settings.promptTemplate),
                ChatMessage(
                    role = "user",
                    content = "" // 图片通过 parts 传递
                )
            )
        )

        // 需要带图片: 构造多模态消息格式 [{"type":"text","text":...},{"type":"image_url",...}]
        val multimodal = """{"model":${json.encodeToString(settings.apiModel)},"max_tokens":4000,"temperature":0.2,"messages":[${"{\"role\":\"system\",\"content\":${json.encodeToString(settings.promptTemplate)}}"},""" +
            """{"role":"user","content":[{"type":"text","text":"请识别图片中的生词"},{"type":"image_url","image_url":{"url":"$dataUrl"}}]}]}"""

        @Suppress("UNUSED_VARIABLE")
        val unused = body // 保留类型引用

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(multimodal.toRequestBody("application/json".toMediaType()))
            .build()

        okHttp.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string().orEmpty()
                throw RuntimeException("AI 请求失败 HTTP ${resp.code}: ${err.take(300)}")
            }
            val raw = resp.body?.string().orEmpty()
            val apiResponse = try {
                json.decodeFromString<ChatResponse>(raw)
            } catch (e: Exception) {
                // 部分服务可能直接返回文本
                return AiParser.parseWords(raw)
            }
            val content = apiResponse.choices.firstOrNull()
                ?.message?.content
                ?: apiResponse.choices.firstOrNull()?.text
                ?: ""
            return AiParser.parseWords(content)
        }
    }

    /**
     * 拉取可用模型列表（GET /models，OpenAI 兼容）。
     */
    suspend fun fetchModels(settings: AppSettings): List<String> {
        require(settings.isApiConfigured) { "请先配置 API" }
        val baseUrl = settings.apiBaseUrl.trimEnd('/')
        val url = "$baseUrl/models"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()
        okHttp.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string().orEmpty()
                throw RuntimeException("拉取模型失败 HTTP ${resp.code}: ${err.take(200)}")
            }
            val raw = resp.body?.string().orEmpty()
            return try {
                json.decodeFromString<ModelsResponse>(raw).data.map { it.id }.filter { it.isNotBlank() }
            } catch (e: Exception) {
                throw RuntimeException("模型列表格式解析失败: ${e.message}")
            }
        }
    }

    private fun String.toRequestBody(type: okhttp3.MediaType?): okhttp3.RequestBody =
        okhttp3.RequestBody.create(type, this)

    /** 预留: 备用 - 直接 multipart 上传图片的方式（部分兼容服务） */
    fun buildMultipartImage(imageFile: File, mimeType: String): MultipartBody.Part {
        val body = imageFile.asRequestBody(mimeType.toMediaType())
        return MultipartBody.Part.createFormData("image", imageFile.name, body)
    }
}