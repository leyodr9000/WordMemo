package com.ley.wordmemo.ui.importwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.api.AiClient
import com.ley.wordmemo.data.api.AiParser
import com.ley.wordmemo.data.api.ExtractedWord
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ImportState {
    data object Idle : ImportState
    data object NoImage : ImportState
    data object NoApi : ImportState
    data object Recognizing : ImportState
    data class Preview(val words: List<ExtractedWord>) : ImportState
    data class Done(val count: Int) : ImportState
    data class Error(val message: String) : ImportState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val aiClient: AiClient,
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private var currentImage: File? = null
    val pendingWords: MutableStateFlow<List<ExtractedWord>> = MutableStateFlow(emptyList())
    val selectedBook: MutableStateFlow<String> = MutableStateFlow("")

    fun onImagePicked(file: File?) {
        currentImage = file
        _state.value = if (file == null) ImportState.NoImage else ImportState.Idle
    }

    fun recognize() {
        val img = currentImage ?: run { _state.value = ImportState.NoImage; return }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.isApiConfigured) {
                _state.value = ImportState.NoApi
                return@launch
            }
            _state.value = ImportState.Recognizing
            try {
                val words = withContext(Dispatchers.IO) {
                    aiClient.extractWordsFromImage(settings, img)
                }
                if (words.isEmpty()) {
                    _state.value = ImportState.Error("未能从图片中识别出单词，请检查 AI 配置或图片")
                } else {
                    pendingWords.value = words
                    _state.value = ImportState.Preview(words)
                }
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "识别失败")
            }
        }
    }

    /** 预览中支持手动修改/剔除 */
    fun updatePending(index: Int, word: ExtractedWord) {
        val list = pendingWords.value.toMutableList()
        if (index in list.indices) list[index] = word
        pendingWords.value = list
    }

    fun togglePending(index: Int) {
        val list = pendingWords.value.toMutableList()
        if (index in list.indices) {
            // 空 word 视为剔除标记
            list[index] = list[index].copy(word = "__REMOVE__")
            pendingWords.value = list
        }
    }

    fun importAll() {
        val words = pendingWords.value
            .filter { it.word.isNotBlank() && it.word != "__REMOVE__" }
            .map { it.toEntity(selectedBook.value.trim()) }
        viewModelScope.launch {
            repository.insertAll(words)
            _state.value = ImportState.Done(words.size)
        }
    }

    fun reset() {
        currentImage = null
        pendingWords.value = emptyList()
        _state.value = ImportState.Idle
    }
}