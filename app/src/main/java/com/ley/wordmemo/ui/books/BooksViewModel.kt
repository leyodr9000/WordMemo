package com.ley.wordmemo.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ley.wordmemo.data.model.BookStat
import com.ley.wordmemo.data.repository.WordRepository
import com.ley.wordmemo.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository,
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