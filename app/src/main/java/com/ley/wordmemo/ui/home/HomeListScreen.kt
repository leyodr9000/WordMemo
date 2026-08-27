package com.ley.wordmemo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    onOpenImport: (String) -> Unit = {},   // 进入导入页: "camera"|"gallery"|"json"
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // ===== 右上角加号下拉菜单 (微信风格入口) =====
    var menuExpanded by remember { mutableStateOf(false) }
    val words by viewModel.words.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hideMastered by viewModel.hideMastered.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBar(
                query = (uiState.filter as? HomeFilter.Query)?.text ?: "",
                onQueryChange = { viewModel.setQuery(it) },
                onSearch = { },
                onActiveChange = { },
                active = uiState.isSearching,
                placeholder = { Text("搜索单词或释义") },
                modifier = Modifier.weight(1f),
            ) {}
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Add, "导入")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("📷 拍照识别导入") },
                        onClick = { menuExpanded = false; onOpenImport("camera") },
                    )
                    DropdownMenuItem(
                        text = { Text("🖼️ 从相册选择") },
                        onClick = { menuExpanded = false; onOpenImport("gallery") },
                    )
                    DropdownMenuItem(
                        text = { Text("📄 导入 JSON 词书") },
                        onClick = { menuExpanded = false; onOpenImport("json") },
                    )
                }
            }
        }

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
                    WordCard(
                        word = word,
                        hideTranslation = hideMastered,
                        onSetStatus = { st -> viewModel.setStatus(word, st) },
                        onSpeak = { viewModel.speak(word.word) },
                    )
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

/**
 * 列表行 (参考网页版):
 *  - 行底色随状态 (生词亮/熟练淡/忘记偏警示)
 *  - 发音按钮 + 单词/音标 (左)
 *  - 释义 (中)
 *  - 三状态快速切换按钮组 (右, 高亮当前) — 同网页版 mastery-toggle-btn
 */
@Composable
private fun WordCard(
    word: Word,
    hideTranslation: Boolean,
    onSetStatus: (WordStatus) -> Unit,
    onSpeak: () -> Unit,
) {
    // 隐藏熟练词翻译 (网页版 blurred-definition): 熟练词释义模糊, 点击揭示
    val status = WordStatus.from(word.status)
    val shouldBlur = hideTranslation && status == WordStatus.MASTERED
    var revealed by remember { mutableStateOf(false) }
    // 行底色随状态 (低饱和区分)
    val containerColor = when (status) {
        WordStatus.NEW -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        WordStatus.MASTERED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        WordStatus.FORGOTTEN -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // 第一行: 发音 + 单词 + 音标 (左), 状态 (右上角小徽章)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.VolumeUp, "发音", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    word.word,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (word.phonetic.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        word.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 释义 (隐藏熟练词翻译时模糊, 点击揭示 - 网页版 toggleRevealDefinition)
            if (word.partOfSpeech.isNotBlank() || word.meaning.isNotBlank()) {
                val defText = "${word.partOfSpeech} ${word.meaning}".trim()
                Text(
                    text = defText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 36.dp)
                        .then(
                            if (shouldBlur && !revealed)
                                Modifier
                                    .clickable(enabled = true, onClick = { revealed = true })
                                    .graphicsLayer {
                                        alpha = 0.45f
                                        // 文字模糊: 用半透明+遮罩简化 (完整 blur 需要 RenderEffect)
                                    }
                            else if (shouldBlur)
                                Modifier.clickable(enabled = true, onClick = { revealed = false })
                            else Modifier
                        ),
                )
                if (shouldBlur && !revealed) {
                    Text(
                        "••• 点击揭示释义",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 36.dp),
                    )
                }
            }
            if (word.example.isNotBlank()) {
                Text(
                    word.example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 36.dp),
                )
            }

            // 三状态快速切换 (同网页版 mastery-btn-group)
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 32.dp)) {
                StatusToggleBtn("生词", status == WordStatus.NEW, MaterialTheme.colorScheme.secondaryContainer) {
                    onSetStatus(WordStatus.NEW)
                }
                StatusToggleBtn("熟练", status == WordStatus.MASTERED, MaterialTheme.colorScheme.primaryContainer) {
                    onSetStatus(WordStatus.MASTERED)
                }
                StatusToggleBtn("忘记", status == WordStatus.FORGOTTEN, MaterialTheme.colorScheme.errorContainer) {
                    onSetStatus(WordStatus.FORGOTTEN)
                }
            }
        }
    }
}

@Composable
private fun StatusToggleBtn(label: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
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
