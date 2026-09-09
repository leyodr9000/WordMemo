package com.ley.wordmemo.ui.home

import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import com.ley.wordmemo.data.stats.StudyStatsRepository
import com.ley.wordmemo.data.stats.TodayStats
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
    private val statsRepository: StudyStatsRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    /** 隐藏熟练词翻译设置 (参考网页版 hideMasteredTranslation) */
    val hideMastered: StateFlow<Boolean> = settingsRepository.settings
        .map { it.hideMasteredTranslation }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 当前词书 (空 = 全部)。参考网页版多词书切换 */
    val activeBook: StateFlow<String> = settingsRepository.settings
        .map { it.activeBook }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** 今日学习统计 (参考网页版进度追踪) */
    val todayStats: StateFlow<TodayStats> = statsRepository.todayStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())

    /** 每日目标 */
    val dailyGoal: StateFlow<Int> = settingsRepository.settings
        .map { it.dailyGoal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    private val bookFlow = settingsRepository.settings
        .map { it.activeBook }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** 按 activeBook 过滤后的全量词列表 (BookNames.normalize: 空=默认词库) */
    private val bookWords: StateFlow<List<Word>> = combine(
        repository.allWords, bookFlow,
    ) { all, book ->
        if (book.isBlank()) all
        else all.filter {
            com.ley.wordmemo.data.model.BookNames.normalize(it.sourceBook) == book
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val words: StateFlow<List<Word>> = combine(
        _uiState, bookWords,
    ) { state, all ->
        when (val f = state.filter) {
            HomeFilter.All -> all
            is HomeFilter.ByStatus -> all.filter { it.status == f.status.dbValue }
            is HomeFilter.Query -> filterByQuery(all, f.text)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 统计跟随当前词书 (与列表一致) */
    val counts: StateFlow<Map<WordStatus, Int>> = bookWords.map { list ->
        mapOf(
            WordStatus.NEW to list.count { it.status == WordStatus.NEW.dbValue },
            WordStatus.MASTERED to list.count { it.status == WordStatus.MASTERED.dbValue },
            WordStatus.FORGOTTEN to list.count { it.status == WordStatus.FORGOTTEN.dbValue },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    /** 列表页快捷开关: 隐藏熟练词翻译 */
    fun toggleHideMastered() = viewModelScope.launch {
        val cur = hideMastered.value
        settingsRepository.setHideMasteredTranslation(!cur)
    }

    private fun filterByQuery(all: List<Word>, q: String): List<Word> {
        val query = q.trim()
        if (query.isEmpty()) return all
        return all.filter {
            it.word.contains(query, ignoreCase = true) ||
                it.meaning.contains(query, ignoreCase = true)
        }
    }
}
