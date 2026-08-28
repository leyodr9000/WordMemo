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
import androidx.compose.material3.TextButton
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
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
                    selfTest = state.selfTest,
                    cardAnimation = state.cardAnimation,
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
                                // 纯左右平移 (无倾斜, 更干净)
                                translationX = offsetX.value
                            }
                    },
                )

                Spacer(Modifier.size(14.dp))

                // ===== 标熟操作 (网页版 card-rating-actions: 常驻两道大按钮) =====
                // 标完自动进下一词, 同时翻回正面
                fun rate(known: Boolean) {
                    flipScope.launch {
                        if (known) viewModel.onKnown() else viewModel.onForgotten()
                        flipped = false
                        flipProgress.snapTo(0f)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 「仍陌生 ←」 红色 (网页版 btn-rating-unfamiliar)
                    OutlinedButton(
                        onClick = { rate(false) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                        Spacer(Modifier.width(4.dp))
                        Text("仍陌生")
                    }
                    // 「已掌握 →」 绿色 (网页版 btn-rating-mastered)
                    Button(
                        onClick = { rate(true) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("已掌握")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, null)
                    }
                }

                Spacer(Modifier.size(6.dp))

                // 翻页提示 (网页版 card-pagination)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { viewModel.previous() }, enabled = true) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, null)
                        Text("上一词")
                    }
                    if (!flipped) {
                        Text(
                            "点击卡片翻转",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "点击卡片翻回",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { viewModel.next() }) {
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
    selfTest: Boolean = false,
    cardAnimation: String = "slide",
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
                .graphicsLayer {
                    // 拖动/切词跟随效果: 按所选动画样式
                    val dx = offsetX
                    when (cardAnimation) {
                        "flip" -> {
                            rotationZ = dx / 120f          // 轻微倾斜
                            scaleX = 1f + kotlin.math.abs(dx) / 8000f
                            scaleY = 1f + kotlin.math.abs(dx) / 8000f
                        }
                        "scale" -> {
                            scaleX = 1f - kotlin.math.abs(dx) / 3000f
                            scaleY = 1f - kotlin.math.abs(dx) / 3000f
                        }
                        "fade" -> {
                            alpha = 1f - kotlin.math.abs(dx) / 2200f
                        }
                        else -> {
                            // slide: 纯平移
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,  // 无涟漪矩形, 避免与滑动手势视觉重叠
                    onClick = onFlip,
                )
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
                    if (selfTest && WordStatus.from(word.status) == WordStatus.MASTERED) {
                        // 自测模式: 熟练词隐藏释义, 先回忆
                        Text(
                            "💭 自测：先回忆这个词的意思",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "（熟练词已隐藏释义，正式环境会正常显示）",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        )
                    } else {
                        Text(
                            "${word.partOfSpeech} ${word.meaning}".trim(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
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