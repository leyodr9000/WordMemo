package com.ley.wordmemo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import com.ley.wordmemo.ui.components.ProgressRing

/**
 * 列表模式（主 Tab 第 1 页）。
 * 含搜索、状态筛选、单词列表，入口按钮进入卡片学习。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeListScreen(
    onOpenStudy: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = (uiState.filter as? HomeFilter.Query)?.text ?: "",
            onQueryChange = { viewModel.setQuery(it) },
            onSearch = { },
            onActiveChange = { },
            active = uiState.isSearching,
            placeholder = { Text("搜索单词或释义") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {}

        // 学习进度统计卡片
        val total = (counts[WordStatus.NEW] ?: 0) + (counts[WordStatus.MASTERED] ?: 0) + (counts[WordStatus.FORGOTTEN] ?: 0)
        val mastered = counts[WordStatus.MASTERED] ?: 0
        val progress = if (total == 0) 0f else mastered.toFloat() / total
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProgressRing(
                    progress = progress,
                    sizeDp = 96.dp,
                    strokeWidth = 8.dp,
                    label = "掌握率",
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("学习进度", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(6.dp))
                    ProgressStatRow("生词", counts[WordStatus.NEW] ?: 0, total)
                    Spacer(Modifier.size(4.dp))
                    ProgressStatRow("熟练", counts[WordStatus.MASTERED] ?: 0, total)
                    Spacer(Modifier.size(4.dp))
                    ProgressStatRow("忘记", counts[WordStatus.FORGOTTEN] ?: 0, total)
                }
            }
        }

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

        // 开始学习入口
        FilledTonalButton(
            onClick = onOpenStudy,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Default.School, null)
            Spacer(Modifier.width(8.dp))
            Text("开始学习（卡片模式）")
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
                Text(
                    "还没有单词\n点击下方「导入」或「开始学习」",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(words, key = { it.id }) { word ->
                    WordCard(word = word, onDelete = { viewModel.delete(word) })
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
    // 无阴影卡片: 列表滚动时 shadow 是 GPU 开销主因
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
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
                        Text(
                            word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            AssistChip(
                onClick = {},
                label = { Text(statusLabel(WordStatus.from(word.status))) },
            )
        }
    }
}

private fun statusLabel(status: WordStatus): String = when (status) {
    WordStatus.NEW -> "生词"
    WordStatus.MASTERED -> "熟练"
    WordStatus.FORGOTTEN -> "忘记"
}

@Composable
private fun ProgressStatRow(label: String, count: Int, total: Int) {
    val pct = if (total == 0) 0 else (count * 100) / total
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { if (total == 0) 0f else count.toFloat() / total },
            modifier = Modifier.weight(1f).height(8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("$count ($pct%)", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
