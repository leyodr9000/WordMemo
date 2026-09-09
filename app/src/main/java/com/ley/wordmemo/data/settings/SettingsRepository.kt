package com.ley.wordmemo.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** AI 与学习设置 */
data class AppSettings(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val apiModel: String = "",
    val promptTemplate: String = DEFAULT_PROMPT,
    val chatPersona: String = DEFAULT_CHAT_PERSONA, // AI 助教人设 (system prompt, 参考网页版动态Prompt)
    val temperature: Double = 0.7,                  // 对话温度
    val dailyGoal: Int = 20,
    val autoSpeak: Boolean = true,
    val darkMode: String = "system", // system | light | dark
    val activeBook: String = "",     // 当前词书，空 = 全部
    val primaryColor: Long = 0L,     // 自定义一级强调色 (ARGB), 0=默认
    val secondaryColor: Long = 0L,   // 自定义二级强调色 (ARGB), 0=默认
    val selfTest: Boolean = false,      // 自测模式: 翻转前隐藏释义
    val speechVoice: String = "",       // 首选 TTS 音色 (空的用系统默认)
    val hideMasteredTranslation: Boolean = false,  // 隐藏熟练词翻译(网页版同名)
    val cardAnimation: String = "slide",  // 卡片切换动画: slide/flip/scale/fade
    val uiStyle: String = "monet",   // 界面风格: monet(Material You) | miui(MIUI X)
    val translationSource: String = "offline",  // 翻译源: offline(内置词典) | ai
    val backgroundUri: String = "",  // 自定义背景壁纸 (content:// 或 https://), 空=无 (参考网页版自定义壁纸)
) {
    val isApiConfigured: Boolean
        get() = apiBaseUrl.isNotBlank() && apiKey.isNotBlank() && apiModel.isNotBlank()

    companion object {
        const val DEFAULT_PROMPT = """请将图片中的生词提取为 JSON 数组，每个元素格式：
{"word":"单词","phonetic":"音标","partOfSpeech":"词性","meaning":"中文释义","example":"例句","exampleTranslation":"例句翻译"}
只输出 JSON，不要多余文字。"""
        const val DEFAULT_CHAT_PERSONA = "你是一位耐心的英语助教。请用生动有趣的中文讲解单词与语法：" +
            "包括词根词缀、记忆技巧、易混词对比和实用例句，语气友好，篇幅适中。"
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val apiBaseUrl = stringPreferencesKey("api_base_url")
        val apiKey = stringPreferencesKey("api_key")
        val apiModel = stringPreferencesKey("api_model")
        val prompt = stringPreferencesKey("prompt_template")
        val chatPersona = stringPreferencesKey("chat_persona")
        val temperature = stringPreferencesKey("chat_temperature") // 存字符串, 便于序列化 Double
        val dailyGoal = intPreferencesKey("daily_goal")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val darkMode = stringPreferencesKey("dark_mode")
        val activeBook = stringPreferencesKey("active_book")
        val primaryColor = longPreferencesKey("primary_color")
        val secondaryColor = longPreferencesKey("secondary_color")
        val selfTest = booleanPreferencesKey("self_test")
        val hideMasteredTranslation = booleanPreferencesKey("hide_mastered_translation")
        val cardAnimation = stringPreferencesKey("card_animation")
        val uiStyle = stringPreferencesKey("ui_style")
        val translationSource = stringPreferencesKey("translation_source")
        val speechVoice = stringPreferencesKey("speech_voice")
        val backgroundUri = stringPreferencesKey("background_uri")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            apiBaseUrl = p[Keys.apiBaseUrl] ?: "",
            apiKey = p[Keys.apiKey] ?: "",
            apiModel = p[Keys.apiModel] ?: "",
            promptTemplate = p[Keys.prompt] ?: AppSettings.DEFAULT_PROMPT,
            chatPersona = p[Keys.chatPersona] ?: AppSettings.DEFAULT_CHAT_PERSONA,
            temperature = p[Keys.temperature]?.toDoubleOrNull() ?: 0.7,
            dailyGoal = p[Keys.dailyGoal] ?: 20,
            autoSpeak = p[Keys.autoSpeak] ?: true,
            darkMode = p[Keys.darkMode] ?: "system",
            activeBook = p[Keys.activeBook] ?: "",
            primaryColor = p[Keys.primaryColor] ?: 0L,
            secondaryColor = p[Keys.secondaryColor] ?: 0L,
            selfTest = p[Keys.selfTest] ?: false,
            speechVoice = p[Keys.speechVoice] ?: "",
            hideMasteredTranslation = p[Keys.hideMasteredTranslation] ?: false,
            cardAnimation = p[Keys.cardAnimation] ?: "slide",
            uiStyle = p[Keys.uiStyle] ?: "monet",
            translationSource = p[Keys.translationSource] ?: "offline",
            backgroundUri = p[Keys.backgroundUri] ?: "",
        )
    }

    suspend fun updateApi(baseUrl: String, key: String, model: String) {
        // 清理空白/换行: OkHttp 对请求头值中的 0x0a 会抛
        // "unexpected char 0x0a ... in authorization value"
        val ws = Regex("\\s+")
        context.dataStore.edit { p ->
            p[Keys.apiBaseUrl] = baseUrl.replace(ws, "").trimEnd('/')
            p[Keys.apiKey] = key.replace(ws, "")
            p[Keys.apiModel] = model.replace(ws, "")
        }
    }

    suspend fun updatePrompt(template: String) {
        context.dataStore.edit { p -> p[Keys.prompt] = template }
    }

    /** AI 助教人设 (system prompt) */
    suspend fun updateChatPersona(persona: String) {
        context.dataStore.edit { p -> p[Keys.chatPersona] = persona }
    }

    /** 对话温度 */
    suspend fun updateTemperature(t: Double) {
        context.dataStore.edit { p -> p[Keys.temperature] = t.toString() }
    }

    /** 自定义背景壁纸, 空串清除 */
    suspend fun updateBackgroundUri(uri: String) {
        context.dataStore.edit { p -> p[Keys.backgroundUri] = uri }
    }

    suspend fun updateDailyGoal(goal: Int) {
        context.dataStore.edit { p -> p[Keys.dailyGoal] = goal }
    }

    suspend fun updateAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.autoSpeak] = enabled }
    }

    suspend fun updateDarkMode(mode: String) {
        context.dataStore.edit { p -> p[Keys.darkMode] = mode }
    }

    /** 当前词书：空串表示「全部」 */
    suspend fun setActiveBook(book: String) {
        context.dataStore.edit { p -> p[Keys.activeBook] = book }
    }

    suspend fun setPrimaryColor(argb: Long) {
        context.dataStore.edit { p -> p[Keys.primaryColor] = argb }
    }

    suspend fun setSecondaryColor(argb: Long) {
        context.dataStore.edit { p -> p[Keys.secondaryColor] = argb }
    }

    suspend fun setSelfTest(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.selfTest] = enabled }
    }

    suspend fun setSpeechVoice(voice: String) {
        context.dataStore.edit { p -> p[Keys.speechVoice] = voice }
    }

    suspend fun setHideMasteredTranslation(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.hideMasteredTranslation] = enabled }
    }

    suspend fun setCardAnimation(style: String) {
        context.dataStore.edit { p -> p[Keys.cardAnimation] = style }
    }

    suspend fun setUiStyle(style: String) {
        context.dataStore.edit { p -> p[Keys.uiStyle] = style }
    }

    suspend fun setTranslationSource(source: String) {
        context.dataStore.edit { p -> p[Keys.translationSource] = source }
    }
}