package com.ley.wordmemo.ui.importwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.api.AiClient
import com.ley.wordmemo.data.api.ExtractedWord
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    /** 队列识别中: stage=0上传 1分析 2解析; index/total = 当前第几张 */
    data class Recognizing(val stage: Int = 0, val index: Int = 0, val total: Int = 1) : ImportState
    data class Preview(val words: List<ExtractedWord>, val notice: String? = null) : ImportState
    data class Done(val count: Int) : ImportState
    data class Error(val message: String) : ImportState
}

/**
 * 拍照导入 ViewModel — 支持多张照片排队识别:
 * 逐张调用 AI, 汇总去重后进入统一预览, 一次导入。
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val aiClient: AiClient,
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private val _currentImage = MutableStateFlow<File?>(null)
    val currentImage: StateFlow<File?> = _currentImage
    val pendingWords: MutableStateFlow<List<ExtractedWord>> = MutableStateFlow(emptyList())
    val selectedBook: MutableStateFlow<String> = MutableStateFlow("")

    /** 单图预览 (相机拍照后立即展示) */
    fun onImagePicked(file: File?) {
        _currentImage.value = file
    }

    /** 入口: 一批图片 (1..n) 排队识别 */
    fun processImages(files: List<File>) {
        if (files.isEmpty()) {
            _state.value = ImportState.NoImage
            return
        }
        _currentImage.value = files.lastOrNull()
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.isApiConfigured) {
                _state.value = ImportState.NoApi
                return@launch
            }
            val all = mutableListOf<ExtractedWord>()
            val errors = mutableListOf<String>()
            for ((i, f) in files.withIndex()) {
                // 阶段可视化: 上传 → 分析 → 解析
                _state.value = ImportState.Recognizing(0, i, files.size)
                delay(300)
                _state.value = ImportState.Recognizing(1, i, files.size)
                val words = runCatching {
                    withContext(Dispatchers.IO) {
                        aiClient.extractWordsFromImage(settings, f)
                    }
                }.onFailure { e ->
                    errors.add("第${i + 1}张: ${e.message ?: "识别失败"}")
                }.getOrDefault(emptyList())
                _state.value = ImportState.Recognizing(2, i, files.size)
                delay(150)
                all += words
            }
            // 按单词去重 (同词保留先出现的)
            val deduped = all.distinctBy { it.word.trim().lowercase() }
            if (deduped.isEmpty()) {
                _state.value = ImportState.Error(
                    errors.firstOrNull() ?: "未能从图片中识别出单词，请检查 AI 配置或图片",
                )
            } else {
                pendingWords.value = deduped
                _state.value = ImportState.Preview(
                    deduped,
                    notice = if (errors.isNotEmpty()) "部分图片识别失败：${errors.joinToString("；")}" else null,
                )
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
        _currentImage.value = null
        pendingWords.value = emptyList()
        _state.value = ImportState.Idle
    }
}
