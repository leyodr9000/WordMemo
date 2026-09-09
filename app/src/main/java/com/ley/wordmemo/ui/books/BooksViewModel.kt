package com.ley.wordmemo.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.model.BookStat
import com.ley.wordmemo.data.reader.OfflineDict
import com.ley.wordmemo.data.util.BackupHelper
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private val _activeBook = MutableStateFlow("")
    val activeBook: StateFlow<String> = _activeBook

    private val _loaded = MutableStateFlow(false)
    /** false = 尚未加载完(显示转圈)；true = 已完成首次查询(空则显示"暂无词书") */
    val loaded: StateFlow<Boolean> = _loaded

    val books: StateFlow<List<BookStat>> = combine(
        repository.books,
        _activeBook,
    ) { list, active ->
        // 把当前激活词书排到最前，并标记
        _loaded.value = true
        list.sortedWith(compareByDescending<BookStat> { it.book == active }.thenByDescending { it.lastUpdated })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _activeBook.value = settingsRepository.settings.first().activeBook
        }
    }

    /** 界面风格 (monet | miui) — 词书页双模式渲染用 */
    val uiStyle: StateFlow<String> = settingsRepository.settings
        .map { it.uiStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "monet")

    fun selectBook(book: String) {
        _activeBook.value = book
        viewModelScope.launch { settingsRepository.setActiveBook(book) }
    }

    /** 返回当前激活词书（空=全部） */
    fun currentActive(): String = _activeBook.value

    fun createBook(name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.createEmptyBook(name.trim())
            onDone(ok)
        }
    }

    fun renameBook(oldName: String, newName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.renameBook(oldName, newName.trim())
            if (_activeBook.value == oldName) {
                _activeBook.value = newName.trim()
                settingsRepository.setActiveBook(newName.trim())
            }
            onDone(true)
        }
    }

    /** 从 JSON 字符串导入词书, 返回导入词数 */
    fun importBookJson(jsonText: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val (bookName, words) = BackupHelper.parseWordBook(jsonText)
            if (words.isEmpty()) { onDone(0); return@launch }
            val enriched = enrichMeanings(words)
            repository.insertAll(enriched)
            // 自动激活新词书
            val name = bookName.ifBlank { "导入词书" }
            _activeBook.value = name
            settingsRepository.setActiveBook(name)
            onDone(enriched.size)
        }
    }

    /** 从 CSV / TSV / 纯文本导入词书 (word,phonetic,meaning[,unit]) */
    fun importDelimited(text: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val words = BackupHelper.parseDelimited(text)
            if (words.isEmpty()) { onDone(0); return@launch }
            val enriched = enrichMeanings(words)
            repository.insertAll(enriched)
            onDone(enriched.size)
        }
    }

    /** 释义缺失时用内置离线词典自动补全 (无需 AI) */
    private fun enrichMeanings(words: List<com.ley.wordmemo.data.model.Word>): List<com.ley.wordmemo.data.model.Word> {
        val needFill = words.any { it.meaning.isBlank() }
        if (!needFill) return words
        OfflineDict.ensureLoaded(context)
        return words.map { w ->
            if (w.meaning.isBlank()) {
                OfflineDict.lookup(w.word)?.let { w.copy(meaning = it) } ?: w
            } else w
        }
    }

    fun deleteBook(book: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.deleteBook(book)
            if (_activeBook.value == book) {
                _activeBook.value = ""
                settingsRepository.setActiveBook("")
            }
            onDone(true)
        }
    }
}