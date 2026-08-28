package com.ley.wordmemo.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.data.model.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    article: Article? = null,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(article?.id ?: -1L) {
        if (article != null) viewModel.loadArticle(article)
        else viewModel.loadLatest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.title ?: "文章阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 全文翻译开关
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, null, modifier = Modifier.size(18.dp))
                        Switch(
                            checked = state.wholeTranslated,
                            onCheckedChange = { viewModel.toggleWholeTranslation() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.translatingAll) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("正在全文翻译…", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.sentenceList) { idx, sentence ->
                    SentenceCard(
                        sentence = sentence,
                        tappedWord = state.tappedWord,
                        showTranslation = state.wholeTranslated ||
                            sentence.translation.isNotBlank(),
                        onTapWord = { word, ax, ay -> viewModel.onWordTap(word, ax, ay) },
                        onTranslate = { viewModel.translateSentence(idx) },
                    )
                }
            }
        }
    }

    // ===== 点词翻译气泡 =====
    if (state.tappedWord.isNotBlank()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 关闭层放最底层 (先声明), 气泡在其上, 避免遮罩挡住气泡
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.dismissWordTip() }
            )
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
                    .widthIn(max = 320.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        state.tappedWord,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.wordMeaning.ifBlank { "翻译中…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

/** 单个句子卡片: 原文(可点词) + 翻译行 + 翻译按钮 */
@Composable
private fun SentenceCard(
    sentence: ReaderSentence,
    tappedWord: String,
    showTranslation: Boolean,
    onTapWord: (String, Float, Float) -> Unit,
    onTranslate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // 原文：点击单词弹翻译气泡
            WordSpans(
                text = sentence.original,
                tappedWord = tappedWord,
                onTapWord = onTapWord,
            )
            Spacer(Modifier.height(6.dp))
            if (showTranslation) {
                Text(
                    sentence.translation.ifBlank { "翻译中…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { /* 点击翻译行可重翻(留作扩展) */ },
                )
            } else {
                OutlinedButton(onClick = onTranslate, enabled = !sentence.translating) {
                    if (sentence.translating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("翻译中…")
                    } else {
                        Icon(Icons.Default.Translate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("翻译本句")
                    }
                }
            }
        }
    }
}

/** 把句子拆成单词 + 空白段，点击单词回调 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WordSpans(
    text: String,
    tappedWord: String,
    onTapWord: (String, Float, Float) -> Unit,
) {
    val segments: List<String> = remember(text) { splitKeepWhitespace(text) }
    // 被点词 (归一化用于匹配: 去标点/小写)
    val tappedKey = remember(tappedWord) {
        tappedWord.trim().trim(',', '.', ';', ':', '!', '?', '"', '(', ')', 's').lowercase()
    }
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { seg ->
            if (seg.trim().isEmpty()) {
                Text(seg, style = MaterialTheme.typography.bodyLarge)
            } else {
                val word = seg.trim()
                val key = word.trim(',', '.', ';', ':', '!', '?', '"', '(', ')', 's').lowercase()
                val isTapped = key == tappedKey && tappedKey.isNotEmpty()
                Text(
                    text = "$word ",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isTapped)
                            androidx.compose.ui.text.style.TextDecoration.Underline
                        else null,
                    ),
                    color = if (isTapped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isTapped) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    modifier = Modifier.clickable {
                        onTapWord(word, 0f, 0f)
                    },
                )
            }
        }
    }
}

private fun splitKeepWhitespace(text: String): List<String> =
    text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotBlank() || it == " " }