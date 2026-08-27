package com.ley.wordmemo.ui.study

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val queue: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val showAnswer: Boolean = false,
    val selfTest: Boolean = false,   // 自测模式
    val speechVoice: String = "",
    val stats: StudyStats = StudyStats(),
) {
    val currentWord: Word? get() = queue.getOrNull(currentIndex)
    val total: Int get() = queue.size
    val progress: Int get() = if (queue.isEmpty()) 0 else currentIndex + 1

    data class StudyStats(
        val known: Int = 0,
        val forgotten: Int = 0,
    )
}

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState

    var tts: TextToSpeech? = null

    fun loadQueue() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.value = StudyUiState(
                queue = repository.getStudyQueue(30),
                selfTest = settings.selfTest,
                speechVoice = settings.speechVoice,
            )
        }
    }

    fun showAnswer() {
        val s = _uiState.value
        if (s.currentWord != null) _uiState.value = s.copy(showAnswer = true)
    }

    fun onSpeak() {
        val word = _uiState.value.currentWord ?: return
        val voice = _uiState.value.speechVoice
        if (voice.isNotEmpty()) {
            tts?.setVoice(android.speech.tts.Voice("$voice", java.util.Locale.US, 2, 1, false, emptySet()))
        }
        tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, "word")
    }

    fun onKnown() {
        val s = _uiState.value
        val word = s.currentWord ?: return
        viewModelScope.launch {
            repository.markStatus(word, WordStatus.MASTERED)
            if (settingsRepository.settings.first().autoSpeak) {
                // 自动发音下一个
            }
            advance(s.stats.copy(known = s.stats.known + 1))
        }
    }

    fun onForgotten() {
        val s = _uiState.value
        val word = s.currentWord ?: return
        viewModelScope.launch {
            repository.markStatus(word, WordStatus.FORGOTTEN)
            advance(s.stats.copy(forgotten = s.stats.forgotten + 1))
        }
    }

    /** 下一词（循环） */
    fun next() {
        val s = _uiState.value
        if (s.queue.isEmpty()) return
        val nextIndex = (s.currentIndex + 1) % s.queue.size
        _uiState.value = s.copy(currentIndex = nextIndex, showAnswer = false)
    }

    /** 上一词（循环） */
    fun previous() {
        val s = _uiState.value
        if (s.queue.isEmpty()) return
        val prevIndex = (s.currentIndex - 1 + s.queue.size) % s.queue.size
        _uiState.value = s.copy(currentIndex = prevIndex, showAnswer = false)
    }

    private suspend fun advance(stats: StudyUiState.StudyStats) {
        val s = _uiState.value
        val nextIndex = (s.currentIndex + 1) % s.queue.size
        _uiState.value = s.copy(currentIndex = nextIndex, showAnswer = false, stats = stats)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}