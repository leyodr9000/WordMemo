package com.ley.wordmemo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import com.ley.wordmemo.ui.components.MultiRing
import com.ley.wordmemo.ui.components.RingLayer
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.extra.SuperSwitch

/**
 * 列表模式（主 Tab 第 1 页）。
 * 结构：固定搜索框 + 单一 LazyColumn ——
 *  - 「默认内容」（词书提示/隐藏开关/进度卡/筛选/开始学习）作为列表头部 item：
 *    向上滑列表时自然收起，回到顶部时自动出现（解决默认内容占半屏的问题）
 *  - 「MIUI X」模式下卡片/开关/按钮使用真 Miuix (HyperOS) 组件
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
    val hideMastered by viewModel.hideMastered.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val uiStyle by viewModel.uiStyle.collectAsStateWithLifecycle()
    val isMiuix = uiStyle == "miui"

    val searchActive = (uiState.filter as? HomeFilter.Query)?.text?.isNotBlank() == true
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ===== 固定搜索框 (不随列表收起); MIUI X 模式用 Miuix 输入框 =====
        if (isMiuix) {
            top.yukonga.miuix.kmp.basic.TextField(
                value = (uiState.filter as? HomeFilter.Query)?.text ?: "",
                onValueChange = { viewModel.setQuery(it) },
                label = "搜索单词或释义",
                useLabelAsPlaceholder = true,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            SearchBar(
                query = (uiState.filter as? HomeFilter.Query)?.text ?: "",
                onQueryChange = { viewModel.setQuery(it) },
                onSearch = { },
                onActiveChange = { },
                active = uiState.isSearching,
                placeholder = { Text("搜索单词或释义") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            ) {}
        }

        // 学习进度统计
        val total = (counts[WordStatus.NEW] ?: 0) + (counts[WordStatus.MASTERED] ?: 0) + (counts[WordStatus.FORGOTTEN] ?: 0)
        val mastered = counts[WordStatus.MASTERED] ?: 0
        val newCount = counts[WordStatus.NEW] ?: 0
        val forgotten = counts[WordStatus.FORGOTTEN] ?: 0
        val totalF = if (total == 0) 1f else total.toFloat()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ===== 可收起默认内容 =====
            if (!searchActive && activeBook.isNotBlank()) {
                item(key = "header_book") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "当前词书：$activeBook",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!searchActive) item(key = "header_hide") {
                if (isMiuix) {
                    MiuixCard(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        SuperSwitch(
                            checked = hideMastered,
                            onCheckedChange = { viewModel.toggleHideMastered() },
                            title = "隐藏熟练词翻译",
                            summary = "熟练词释义模糊，点击揭示",
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("隐藏熟练词翻译", style = MaterialTheme.typography.labelMedium)
                                Text("熟练词释义模糊，点击揭示", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hideMastered,
                                onCheckedChange = { viewModel.toggleHideMastered() },
                            )
                        }
                    }
                }
            }

            if (!searchActive) item(key = "header_stats") {
                HomeCard(isMiuix = isMiuix, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 多层进度环 (运动手环样式): 生词/熟练/忘记 各一层不同配色
                        MultiRing(
                            layers = listOf(
                                RingLayer("熟练", mastered / totalF, MaterialTheme.colorScheme.primary),
                                RingLayer("生词", newCount / totalF, MaterialTheme.colorScheme.tertiary),
                                RingLayer("忘记", forgotten / totalF, MaterialTheme.colorScheme.error),
                            ),
                            sizeDp = 116.dp,
                            strokeWidth = 12.dp,
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // 掌握率 (已处理词占比) 移到环外, 与环同行
                            val masteryPct = ((mastered + forgotten) * 100 / totalF).toInt()
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$masteryPct%",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "掌握率",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                            ProgressStatRow("生词", counts[WordStatus.NEW] ?: 0, total)
                            Spacer(Modifier.size(4.dp))
                            ProgressStatRow("熟练", counts[WordStatus.MASTERED] ?: 0, total)
                            Spacer(Modifier.size(4.dp))
                            ProgressStatRow("忘记", counts[WordStatus.FORGOTTEN] ?: 0, total)
                        }
                    }
                    // 今日进度 (参考网页版每日目标): 今日已学 X / 目标 Y
                    val goal = if (dailyGoal <= 0) 1 else dailyGoal
                    Spacer(Modifier.size(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "今日 ${todayStats.reviewedToday}/$goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (todayStats.reviewedToday >= goal)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                (todayStats.reviewedToday.toFloat() / goal).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = if (todayStats.reviewedToday >= goal)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                        )
                        if (todayStats.masteredToday > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "✓ ${todayStats.masteredToday}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (!searchActive) item(key = "header_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
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
            }

            if (!searchActive) item(key = "header_start") {
                if (isMiuix) {
                    MiuixButton(
                        onClick = onOpenStudy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.School, null)
                        Spacer(Modifier.width(8.dp))
                        Text("开始学习（卡片模式）")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onOpenStudy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.School, null)
                        Spacer(Modifier.width(8.dp))
                        Text("开始学习（卡片模式）")
                    }
                }
            }

            // ===== 单词列表 =====
            if (words.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Book, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            if (searchActive) "没有匹配的单词"
                            else "还没有单词\n点击上方「开始学习」或到「词书」导入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(words, key = { it.id }) { word ->
                    WordCard(
                        word = word,
                        isMiuix = isMiuix,
                        hideTranslation = hideMastered,
                        onSetStatus = { st -> viewModel.setStatus(word, st) },
                        onSpeak = { viewModel.speak(word.word) },
                    )
                }
            }
        }
    }
}

/** 双模式卡片容器: MIUI X 模式用 Miuix Card (HyperOS 圆角), 否则 Material3 Card */
@Composable
private fun HomeCard(
    isMiuix: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isMiuix) {
        MiuixCard(
            modifier = modifier,
            insideMargin = androidx.compose.foundation.layout.PaddingValues(16.dp),
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = content,
        )
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
 * 列表行:
 *  - 发音按钮 + 单词/音标 (左)
 *  - 释义 (中, 熟练词可隐藏)
 *  - 三状态快速切换按钮组 (右, 高亮当前)
 */
@Composable
private fun WordCard(
    word: Word,
    isMiuix: Boolean,
    hideTranslation: Boolean,
    onSetStatus: (WordStatus) -> Unit,
    onSpeak: () -> Unit,
) {
    // 隐藏熟练词翻译: 熟练词释义模糊, 点击显示/再次点击隐藏; 切换开关时重置揭示状态
    val status = WordStatus.from(word.status)
    val shouldBlur = hideTranslation && status == WordStatus.MASTERED
    var revealed by remember(word.id, hideTranslation) { mutableStateOf(false) }
    // 行底色随状态 (低饱和区分) — 仅 Material3 模式; Miuix 模式用统一 HyperOS 卡面 + 状态 chips 区分
    val containerColor = when (status) {
        WordStatus.NEW -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        WordStatus.MASTERED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        WordStatus.FORGOTTEN -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f)
    }
    if (isMiuix) {
        MiuixCard(modifier = Modifier.fillMaxWidth(), insideMargin = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            WordCardBody(word, shouldBlur, revealed, { revealed = !revealed }, onSetStatus, onSpeak)
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            WordCardBody(word, shouldBlur, revealed, { revealed = !revealed }, onSetStatus, onSpeak)
        }
    }
}

@Composable
private fun WordCardBody(
    word: Word,
    shouldBlur: Boolean,
    revealed: Boolean,
    onReveal: () -> Unit,
    onSetStatus: (WordStatus) -> Unit,
    onSpeak: () -> Unit,
) {
    val status = WordStatus.from(word.status)
    Column(modifier = Modifier.fillMaxWidth()) {
        // 第一行: 发音 + 单词 (左, 弹性截断) + 音标 (右, 限宽截断防溢出)
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
                modifier = Modifier.weight(1.6f),
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    word.phonetic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 释义: 隐藏熟练词翻译时占位提示, 点击显示 / 再次点击隐藏
        if (word.partOfSpeech.isNotBlank() || word.meaning.isNotBlank()) {
            val defText = "${word.partOfSpeech} ${word.meaning}".trim()
            if (shouldBlur && !revealed) {
                Text(
                    "👁 点击显示释义",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 36.dp)
                        .clickable { onReveal() },
                )
            } else if (shouldBlur && revealed) {
                Text(
                    text = defText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 36.dp)
                        .clickable { onReveal() },
                )
            } else {
                Text(
                    text = defText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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

        // 三状态快速切换
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
    // 进度条带增减动画
    val anim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (total == 0) 0f else count.toFloat() / total,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
        label = "stat-${label}",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
        LinearProgressIndicator(
            progress = { anim },
            modifier = Modifier.weight(1f).height(8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count ($pct%)",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",  // 等宽数字: 个位/十位不跳动
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
