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
    val dailyGoal: Int = 20,
    val autoSpeak: Boolean = true,
    val darkMode: String = "system", // system | light | dark
    val activeBook: String = "",     // 当前词书，空 = 全部
    val primaryColor: Long = 0L,     // 自定义一级强调色 (ARGB), 0=默认
    val secondaryColor: Long = 0L,   // 自定义二级强调色 (ARGB), 0=默认
) {
    val isApiConfigured: Boolean
        get() = apiBaseUrl.isNotBlank() && apiKey.isNotBlank() && apiModel.isNotBlank()

    companion object {
        const val DEFAULT_PROMPT = """请将图片中的生词提取为 JSON 数组，每个元素格式：
{"word":"单词","phonetic":"音标","partOfSpeech":"词性","meaning":"中文释义","example":"例句","exampleTranslation":"例句翻译"}
只输出 JSON，不要多余文字。"""
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
        val dailyGoal = intPreferencesKey("daily_goal")
        val autoSpeak = booleanPreferencesKey("auto_speak")
        val darkMode = stringPreferencesKey("dark_mode")
        val activeBook = stringPreferencesKey("active_book")
        val primaryColor = longPreferencesKey("primary_color")
        val secondaryColor = longPreferencesKey("secondary_color")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            apiBaseUrl = p[Keys.apiBaseUrl] ?: "",
            apiKey = p[Keys.apiKey] ?: "",
            apiModel = p[Keys.apiModel] ?: "",
            promptTemplate = p[Keys.prompt] ?: AppSettings.DEFAULT_PROMPT,
            dailyGoal = p[Keys.dailyGoal] ?: 20,
            autoSpeak = p[Keys.autoSpeak] ?: true,
            darkMode = p[Keys.darkMode] ?: "system",
            activeBook = p[Keys.activeBook] ?: "",
            primaryColor = p[Keys.primaryColor] ?: 0L,
            secondaryColor = p[Keys.secondaryColor] ?: 0L,
        )
    }

    suspend fun updateApi(baseUrl: String, key: String, model: String) {
        context.dataStore.edit { p ->
            p[Keys.apiBaseUrl] = baseUrl.trim()
            p[Keys.apiKey] = key.trim()
            p[Keys.apiModel] = model.trim()
        }
    }

    suspend fun updatePrompt(template: String) {
        context.dataStore.edit { p -> p[Keys.prompt] = template }
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
}