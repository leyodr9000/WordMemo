package com.ley.wordmemo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.settings.AppSettings
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiFormState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val apiForm = MutableStateFlow(ApiFormState())

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
    fun updateAutoSpeak(v: Boolean) = viewModelScope.launch { repository.updateAutoSpeak(v) }
    fun updateDarkMode(mode: String) = viewModelScope.launch { repository.updateDarkMode(mode) }
    fun updatePrompt(p: String) = viewModelScope.launch { repository.updatePrompt(p) }
}