package com.ley.wordmemo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.api.AiClient
import com.ley.wordmemo.data.api.ChatMessage
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 对话条目 */
data class ChatEntry(
    val role: String,        // "user" | "assistant" | "system"
    val content: String,
    val streaming: Boolean = false,
)

data class ChatUiState(
    val entries: List<ChatEntry> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
    val configured: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiClient: AiClient,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(configured = settingsRepository.settings.first().isApiConfigured)
        }
    }

    fun onInputChange(v: String) { _state.value = _state.value.copy(input = v) }

    /** 设置当前单词上下文（从卡片/列表页进入时调用） */
    fun setWordContext(word: String, meaning: String = "") {
        if (word.isBlank()) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val sys = "你是单词记忆 App 的英语助教。当前用户在学单词「$word」${if (meaning.isNotBlank()) "（释义：$meaning）" else ""}。" +
                "请用中文生动解释这个词：词根词缀、记忆技巧、易混词、例句。语气友好，篇幅适中。"
            _state.value = _state.value.copy(
                entries = listOf(ChatEntry("system", sys)),
                input = "帮我讲解「$word」",
            )
        }
    }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.isApiConfigured) {
                _state.value = _state.value.copy(error = "请先到「设置」配置 AI API")
                return@launch
            }
            val userMsg = ChatEntry("user", text)
            val cur = _state.value
            _state.value = cur.copy(
                entries = cur.entries + userMsg + ChatEntry("assistant", "", streaming = true),
                input = "",
                sending = true,
                error = null,
            )
            // 组装历史（去 system 级别，转为 OpenAI messages，保留 system 与最近 N 条）
            val history = _state.value.entries.dropLast(1) // 去掉流式占位
                .takeLast(16)
                .map { ChatMessage(role = it.role, content = it.content) }
            if (history.none { it.role == "system" }) {
                // 若无 system（如未设上下文），补默认
                history + ChatMessage("system", "你是单词记忆 App 的英语助教，用中文讲解英语单词：词根、记忆技巧、例句。")
            }
            try {
                val result = withContext(Dispatchers.IO) {
                    aiClient.chat(
                        settings = settings,
                        history = history,
                        onDelta = { delta ->
                            // 流式追加到当前 assistant 占位
                            val st = _state.value
                            val list = st.entries.toMutableList()
                            val idx = list.indexOfLast { it.role == "assistant" && it.streaming }
                            if (idx >= 0) {
                                list[idx] = list[idx].copy(content = list[idx].content + delta)
                                _state.value = st.copy(entries = list)
                            }
                        }
                    )
                }
                val st = _state.value
                val list = st.entries.toMutableList()
                val idx = list.indexOfLast { it.role == "assistant" && it.streaming }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(content = if (result.isBlank()) list[idx].content else result, streaming = false)
                }
                _state.value = st.copy(entries = list, sending = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(sending = false, error = e.message ?: "对话失败")
            }
        }
    }

    fun clear() {
        _state.value = _state.value.copy(entries = emptyList(), error = null)
    }
}