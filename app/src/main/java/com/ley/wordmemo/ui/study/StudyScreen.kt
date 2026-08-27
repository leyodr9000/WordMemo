package com.ley.wordmemo.ui.study

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.data.model.Word
import com.ley.wordmemo.data.model.WordStatus
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    onAskAi: (String, String) -> Unit = { _, _ -> },
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadQueue() }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                viewModel.tts?.stop()
            }
        }
        tts.language = Locale.US
        viewModel.tts = tts
        onDispose {
            tts.stop()
            tts.shutdown()
            viewModel.tts = null
        }
    }

    // ===== 3D 翻转状态 =====
    var flipped by remember { mutableStateOf(false) }
    val flipProgress = remember { Animatable(0f) }  // 0=正面 1=背面
    val flipScope = rememberCoroutineScope()
    val dragScope = rememberCoroutineScope()

    // 卡片拖拽位移
    val offsetX = remember { Animatable(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("卡片学习") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    val cur = state.currentWord
                    if (cur != null) {
                        IconButton(onClick = { onAskAi(cur.word, cur.meaning) }) {
                            Icon(Icons.Default.SmartToy, "问AI")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${state.progress} / ${state.total}   · 认识 ${state.stats.known}  忘记 ${state.stats.forgotten}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.progress.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(16.dp))

            val word = state.currentWord
            if (word == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("没有可学习的单词", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    Text("请先导入单词", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // ===== 3D 翻转卡片 =====
                FlipCard3D(
                    word = word,
                    flipped = flipped,
                    flipProgress = flipProgress.value,
                    offsetX = offsetX.value,
                    onFlip = {
                        flipped = !flipped
                        flipScope.launch {
                            flipProgress.animateTo(if (flipped) 1f else 0f, tween(350))
                        }
                    },
                    onSpeak = { viewModel.onSpeak() },
                    modifyCard = { mod ->
                        mod
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, drag ->
                                        change.consume()
                                        dragScope.launch { offsetX.snapTo(offsetX.value + drag) }
                                    },
                                    onDragEnd = {
                                        val dx = offsetX.value
                                        val flyOut = 900f
                                        if (dx < -80f) {
                                            dragScope.launch {
                                                offsetX.animateTo(-flyOut, tween(180))
                                                viewModel.next()
                                                flipped = false
                                                flipProgress.snapTo(0f)
                                                offsetX.snapTo(flyOut)
                                                offsetX.animateTo(0f, androidx.compose.animation.core.spring())
                                            }
                                        } else if (dx > 80f) {
                                            dragScope.launch {
                                                offsetX.animateTo(flyOut, tween(180))
                                                viewModel.previous()
                                                flipped = false
                                                flipProgress.snapTo(0f)
                                                offsetX.snapTo(-flyOut)
                                                offsetX.animateTo(0f, androidx.compose.animation.core.spring())
                                            }
                                        } else {
                                            dragScope.launch {
                                                offsetX.animateTo(0f, androidx.compose.animation.core.spring())
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        dragScope.launch { offsetX.animateTo(0f, androidx.compose.animation.core.spring()) }
                                    },
                                )
                            }
                            .graphicsLayer {
                                translationX = offsetX.value
                                rotationZ = offsetX.value / 90f
                            }
                    },
                )

                Spacer(Modifier.size(16.dp))

                // ===== 底部操作 (网页版: 认识/忘记常驻 + 上/下翻页) =====
                if (flipped) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.onKnown()
                                flipped = false
                                flipScope.launch { flipProgress.snapTo(0f) }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text("认识")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.onForgotten()
                                flipped = false
                                flipScope.launch { flipProgress.snapTo(0f) }
                            },
                        ) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("忘记")
                        }
                    }
                } else {
                    Text(
                        "点击卡片翻转查看释义",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.size(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { viewModel.previous() }) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                        Text("上一词")
                    }
                    OutlinedButton(onClick = { viewModel.next() }) {
                        Text("下一词")
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                    }
                }
            }
        }
    }
}

/** 网页版风格 3D 翻转卡片：点卡片 rotateY 翻转 */
@Composable
private fun FlipCard3D(
    word: Word,
    flipped: Boolean,
    flipProgress: Float,   // 0f 正面, 1f 背面
    offsetX: Float,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    modifyCard: @Composable (Modifier) -> Modifier,
) {
    // 用 rotationY 做 3D 翻转 (变量名不与 graphicsLayer 属性冲突)
    val flipAngle = when {
        flipProgress < 0.5f -> flipProgress * 180f
        else -> (1f - flipProgress) * -180f
    }
    Box(
        modifier = modifyCard(
            Modifier
                .graphicsLayer {
                    rotationY = flipAngle.coerceIn(-180f, 180f)
                    cameraDistance = 12f * density
                }
                .clickable(onClick = onFlip)
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (flipProgress < 0.5f) {
            // ===== 正面: 单词/音标/发音 =====
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (word.sourceBook.isNotBlank()) {
                        Text(word.sourceBook, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        word.word,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(8.dp))
                    if (word.phonetic.isNotBlank()) {
                        Text(word.phonetic, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.size(8.dp))
                    IconButton(onClick = onSpeak) {
                        Icon(Icons.Default.VolumeUp, "发音")
                    }
                    Spacer(Modifier.size(24.dp))
                    Text(
                        "🔄 点击卡片翻转查看释义",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // ===== 背面: 释义/例句/状态 =====
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "释义与词性",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        word.word,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${word.partOfSpeech} ${word.meaning}".trim(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (word.example.isNotBlank()) {
                        Spacer(Modifier.size(16.dp))
                        Text(
                            word.example,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (word.exampleTranslation.isNotBlank()) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            word.exampleTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    // 状态徽章
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (WordStatus.from(word.status)) {
                                    WordStatus.NEW -> MaterialTheme.colorScheme.tertiaryContainer
                                    WordStatus.MASTERED -> MaterialTheme.colorScheme.secondaryContainer
                                    WordStatus.FORGOTTEN -> MaterialTheme.colorScheme.errorContainer
                                },
                                shape = RoundedCornerShape(50),
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            when (WordStatus.from(word.status)) {
                                WordStatus.NEW -> "生词"
                                WordStatus.MASTERED -> "熟练"
                                WordStatus.FORGOTTEN -> "忘记"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(Modifier.size(20.dp))
                    Text(
                        "↩️ 点击卡片翻回正面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}