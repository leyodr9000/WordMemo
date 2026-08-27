package com.ley.wordmemo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStudyClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SearchBar(
                query = (uiState.filter as? HomeFilter.Query)?.text ?: "",
                onQueryChange = { viewModel.setQuery(it) },
                onSearch = { },
                onActiveChange = { },
                active = uiState.isSearching,
                placeholder = { Text("搜索单词或释义") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            ) {}
        },
        bottomBar = {
            HomeBottomBar(
                onStudy = onStudyClick,
                onImport = onImportClick,
                onBooks = onBooksClick,
                onSettings = onSettingsClick,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 状态筛选 chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChip("全部", uiState.filter == HomeFilter.All) { viewModel.setFilter(HomeFilter.All) }
                StatusChip("生词 ${counts[WordStatus.NEW] ?: 0}", uiState.filter == HomeFilter.ByStatus(WordStatus.NEW)) {
                    viewModel.setFilter(HomeFilter.ByStatus(WordStatus.NEW))
                }
                StatusChip("熟练 ${counts[WordStatus.MASTERED] ?: 0}", uiState.filter == HomeFilter.ByStatus(WordStatus.MASTERED)) {
                    viewModel.setFilter(HomeFilter.ByStatus(WordStatus.MASTERED))
                }
                StatusChip("忘记 ${counts[WordStatus.FORGOTTEN] ?: 0}", uiState.filter == HomeFilter.ByStatus(WordStatus.FORGOTTEN)) {
                    viewModel.setFilter(HomeFilter.ByStatus(WordStatus.FORGOTTEN))
                }
            }
            Spacer(Modifier.size(4.dp))
            if (words.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Book, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.size(12.dp))
                    Text("还没有单词\n点击下方「拍照导入」或「开始学习」", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(words, key = { it.id }) { word ->
                        WordCard(word = word, onDelete = { viewModel.delete(word) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun WordCard(word: Word, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        word.word,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (word.phonetic.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(word.phonetic, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (word.partOfSpeech.isNotBlank() || word.meaning.isNotBlank()) {
                    Text(
                        "${word.partOfSpeech} ${word.meaning}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (word.example.isNotBlank()) {
                    Text(
                        word.example,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatusBadge(status = WordStatus.from(word.status))
        }
    }
}

@Composable
private fun StatusBadge(status: WordStatus) {
    val (label, color) = when (status) {
        WordStatus.NEW -> "生词" to MaterialTheme.colorScheme.primary
        WordStatus.MASTERED -> "熟练" to MaterialTheme.colorScheme.tertiary
        WordStatus.FORGOTTEN -> "忘记" to MaterialTheme.colorScheme.error
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        // colors 可用, 简化处理
    )
}

@Composable
private fun HomeBottomBar(
    onStudy: () -> Unit,
    onImport: () -> Unit,
    onBooks: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
        NavigationBarItem(
            selected = false,
            onClick = onStudy,
            icon = { Icon(Icons.Default.School, null) },
            label = { Text("学习") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onImport,
            icon = { Icon(Icons.Default.Add, null) },
            label = { Text("导入") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onBooks,
            icon = { Icon(Icons.Default.MenuBook, null) },
            label = { Text("词书") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("设置") },
        )
    }
}