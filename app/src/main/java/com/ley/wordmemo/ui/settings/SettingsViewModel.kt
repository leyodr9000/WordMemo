package com.ley.wordmemo.ui.settings

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.api.AiClient
import com.ley.wordmemo.data.settings.AppSettings
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ApiFormState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

data class ModelsState(
    val loading: Boolean = false,
    val models: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val aiClient: AiClient,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val apiForm = MutableStateFlow(ApiFormState())
    val modelsState = MutableStateFlow(ModelsState())

    init {
        viewModelScope.launch {
            val s = repository.settings.first()
            apiForm.value = ApiFormState(
                baseUrl = s.apiBaseUrl,
                apiKey = s.apiKey,
                model = s.apiModel,
            )
        }
    }

    val isConfigured: StateFlow<Boolean> = repository.settings
        .map { it.isApiConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onBaseUrlChange(v: String) { apiForm.value = apiForm.value.copy(baseUrl = v) }
    fun onKeyChange(v: String) { apiForm.value = apiForm.value.copy(apiKey = v) }
    fun onModelChange(v: String) { apiForm.value = apiForm.value.copy(model = v) }

    fun saveApi() {
        val f = apiForm.value
        if (f.baseUrl.isBlank() || f.apiKey.isBlank() || f.model.isBlank()) {
            apiForm.value = f.copy(error = "Base URL / API Key / 模型名都不能为空")
            return
        }
        viewModelScope.launch {
            apiForm.value = f.copy(saving = true, error = null)
            repository.updateApi(f.baseUrl.trimEnd('/'), f.apiKey.trim(), f.model.trim())
            apiForm.value = f.copy(saving = false, saved = true)
        }
    }

    fun updateDailyGoal(goal: Int) = viewModelScope.launch { repository.updateDailyGoal(goal) }
    fun updateSelfTest(v: Boolean) = viewModelScope.launch { repository.setSelfTest(v) }
    fun updateSpeechVoice(v: String) = viewModelScope.launch { repository.setSpeechVoice(v) }
    fun updateHideMasteredTranslation(v: Boolean) = viewModelScope.launch { repository.setHideMasteredTranslation(v) }
    fun updateAutoSpeak(v: Boolean) = viewModelScope.launch { repository.updateAutoSpeak(v) }
    fun updateDarkMode(mode: String) = viewModelScope.launch { repository.updateDarkMode(mode) }
    fun updatePrompt(p: String) = viewModelScope.launch { repository.updatePrompt(p) }

    /** 应用主题色方案（一级+二级强调色一起存） */
    fun updateTheme(option: com.ley.wordmemo.ui.theme.ThemeColorOption) {
        viewModelScope.launch {
            repository.setPrimaryColor(option.primaryArgb)
            repository.setSecondaryColor(option.secondaryArgb)
        }
    }

    /** 单独设置一级强调色（Monet 派生整套） */
    fun updatePrimary(color: androidx.compose.ui.graphics.Color) {
        viewModelScope.launch {
            repository.setPrimaryColor(color.toArgb().toLong() and 0xFFFFFFFFL)
        }
    }

    /** 单独设置二级强调色 */
    fun updateSecondary(color: androidx.compose.ui.graphics.Color) {
        viewModelScope.launch {
            repository.setSecondaryColor(color.toArgb().toLong() and 0xFFFFFFFFL)
        }
    }

    /** 使用当前表单里的 Base URL + Key 拉取可用模型列表 */
    fun fetchModels() {
        val f = apiForm.value
        if (f.baseUrl.isBlank() || f.apiKey.isBlank()) {
            modelsState.value = ModelsState(error = "请先填写 Base URL 和 API Key")
            return
        }
        viewModelScope.launch {
            modelsState.value = ModelsState(loading = true)
            try {
                val testSettings = com.ley.wordmemo.data.settings.AppSettings(
                    apiBaseUrl = f.baseUrl.trimEnd('/'),
                    apiKey = f.apiKey.trim(),
                    apiModel = f.model,
                )
                val models = withContext(Dispatchers.IO) { aiClient.fetchModels(testSettings) }
                modelsState.value = ModelsState(models = models)
            } catch (e: Exception) {
                modelsState.value = ModelsState(error = e.message ?: "拉取失败")
            }
        }
    }

    /** 勾选模型并填入表单 */
    fun selectModel(model: String) {
        apiForm.value = apiForm.value.copy(model = model)
    }
}