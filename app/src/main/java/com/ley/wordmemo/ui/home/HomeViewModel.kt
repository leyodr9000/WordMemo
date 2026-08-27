package com.ley.wordmemo.ui.home

import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import com.ley.wordmemo.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeFilter {
    data object All : HomeFilter
    data class ByStatus(val status: WordStatus) : HomeFilter
    data class Query(val text: String) : HomeFilter
}

data class HomeUiState(
    val filter: HomeFilter = HomeFilter.All,
    val isSearching: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WordRepository,
    private val settingsRepository: com.ley.wordmemo.data.settings.SettingsRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    /** 隐藏熟练词翻译设置 (参考网页版 hideMasteredTranslation) */
    val hideMastered: kotlinx.coroutines.flow.StateFlow<Boolean> = settingsRepository.settings
        .map { it.hideMasteredTranslation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val words: StateFlow<List<Word>> = combine(
        _uiState,
        repository.allWords,
    ) { state, all ->
        when (val f = state.filter) {
            HomeFilter.All -> all
            is HomeFilter.ByStatus -> all.filter { it.status == f.status.dbValue }
            is HomeFilter.Query -> filterByQuery(all, f.text)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val counts: StateFlow<Map<WordStatus, Int>> = combine(
        repository.countByStatus(WordStatus.NEW),
        repository.countByStatus(WordStatus.MASTERED),
        repository.countByStatus(WordStatus.FORGOTTEN),
    ) { n, m, f -> mapOf(WordStatus.NEW to n, WordStatus.MASTERED to m, WordStatus.FORGOTTEN to f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(filter: HomeFilter) {
        _uiState.value = _uiState.value.copy(
            filter = filter,
            isSearching = filter is HomeFilter.Query
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(filter = HomeFilter.All, isSearching = false)
    }

    fun setQuery(q: String) {
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(filter = HomeFilter.All, isSearching = false)
        } else {
            _uiState.value = _uiState.value.copy(filter = HomeFilter.Query(q), isSearching = true)
        }
    }

    fun delete(word: Word) = viewModelScope.launch { repository.delete(word) }

    private var tts: android.speech.tts.TextToSpeech? = null

    init {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }
    }

    /** 播放单词发音 (TTS) */
    fun speak(word: String) {
        tts?.speak(word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "word")
    }


    fun setStatus(word: Word, status: WordStatus) =
        viewModelScope.launch { repository.setStatus(word.id, status) }

    private fun filterByQuery(all: List<Word>, q: String): List<Word> {
        val query = q.trim()
        if (query.isEmpty()) return all
        return all.filter {
            it.word.contains(query, ignoreCase = true) ||
                it.meaning.contains(query, ignoreCase = true)
        }
    }
}