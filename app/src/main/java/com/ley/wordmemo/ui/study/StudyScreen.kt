package com.ley.wordmemo.ui.study

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadQueue() }

    // 初始化 TTS
    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                viewModel.tts?.stop()
            }
        }
        tts.language = java.util.Locale.US
        viewModel.tts = tts
        onDispose {
            tts.stop()
            tts.shutdown()
            viewModel.tts = null
        }
    }

    var offsetX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("卡片学习") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
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
            // 进度
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
                // 卡片（支持左右滑动切换）
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, drag ->
                                    change.consume()
                                    offsetX += drag
                                },
                                onDragEnd = {
                                    if (offsetX < -80f) viewModel.next()
                                    else if (offsetX > 80f) viewModel.previous()
                                    offsetX = 0f
                                },
                                onDragCancel = { offsetX = 0f },
                            )
                        }
                        .graphicsLayer {
                            translationX = offsetX
                            rotationZ = offsetX / 30f
                        },
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
                        IconButton(onClick = { viewModel.onSpeak() }) {
                            Icon(Icons.Default.VolumeUp, "发音")
                        }

                        Spacer(Modifier.size(20.dp))

                        if (state.showAnswer) {
                            Text(
                                "${word.partOfSpeech} ${word.meaning}".trim(),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            if (word.example.isNotBlank()) {
                                Spacer(Modifier.size(12.dp))
                                Text(
                                    word.example,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            if (word.exampleTranslation.isNotBlank()) {
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    word.exampleTranslation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(Modifier.size(16.dp))
                            // 认识 / 忘记
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.onKnown() },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                ) {
                                    Icon(Icons.Default.Check, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("认识")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.onForgotten() },
                                ) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(6.dp))
                                    Text("忘记")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.showAnswer() }) {
                                Text("显示答案")
                            }
                        }
                    }
                }

                Spacer(Modifier.size(16.dp))
                // 底部导航：上一词 / 下一页
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