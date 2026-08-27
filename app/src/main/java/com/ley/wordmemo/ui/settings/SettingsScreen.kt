package com.ley.wordmemo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.ui.components.ColorPickerDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)?,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apiForm by viewModel.apiForm.collectAsStateWithLifecycle()
    val modelsState by viewModel.modelsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // 自定义取色对话框状态 (哪一级, 当前ARGB)
    var showPicker by remember { mutableStateOf<Pair<String, Long>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== AI API 配置 =====
            Text("AI 识图 API（核心）", style = MaterialTheme.typography.titleMedium)
            Text(
                "兼容 OpenAI Chat Completions 格式。拍照后调用该 API 识别生词，解析为固定 JSON 导入。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = apiForm.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = apiForm.apiKey,
                onValueChange = viewModel::onKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = apiForm.model,
                onValueChange = viewModel::onModelChange,
                label = { Text("模型名") },
                placeholder = { Text("gpt-4o-mini") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // 获取可用模型列表
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "可用模型（自动拉取）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { viewModel.fetchModels() },
                    enabled = !modelsState.loading,
                ) {
                    if (modelsState.loading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("拉取中…")
                    } else {
                        Text("获取可用模型")
                    }
                }
            }

            modelsState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (modelsState.models.isNotEmpty()) {
                Text(
                    "点击模型填入上方：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 模型列表（可勾选，点击填入）
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    modelsState.models.forEach { m ->
                        androidx.compose.material3.FilterChip(
                            selected = apiForm.model == m,
                            onClick = { viewModel.selectModel(m) },
                            label = { Text(m, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
            }

            apiForm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { viewModel.saveApi() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !apiForm.saving,
            ) {
                if (apiForm.saving) {
                    Text("保存中…")
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (apiForm.saved) "已保存 ✓" else "保存配置")
                }
            }

            // ===== 学习设置 =====
            Text("学习", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("每日目标：${settings.dailyGoal}", modifier = Modifier.weight(1f))
            }
            Slider(
                value = settings.dailyGoal.toFloat(),
                onValueChange = { },
                onValueChangeFinished = { },
                valueRange = 5f..100f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30, 50).forEach { goal ->
                    Button(onClick = { viewModel.updateDailyGoal(goal) }, enabled = settings.dailyGoal != goal) {
                        Text("$goal")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("自动发音", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.autoSpeak,
                    onCheckedChange = { viewModel.updateAutoSpeak(it) },
                )
            }

            // 自测模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自测模式", style = MaterialTheme.typography.titleMedium)
                    Text("翻转前隐藏释义，先回忆再对照", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.selfTest,
                    onCheckedChange = { viewModel.updateSelfTest(it) },
                )
            }

            // 隐藏熟练词翻译 (网页版 hide-mastered-checkbox)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("隐藏熟练词翻译", style = MaterialTheme.typography.titleMedium)
                    Text("熟练词的释义模糊显示，点击揭示，便于自测回忆", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.hideMasteredTranslation,
                    onCheckedChange = { viewModel.updateHideMasteredTranslation(it) },
                )
            }

            // 语音选择
            Text("发音音色", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("默认" to "", "英音 A" to "en-GB", "英音 B" to "en-GB-x-isa", "美音 A" to "en-US", "美音 B" to "en-US-x-iwz")) { (label, tag) ->
                    androidx.compose.material3.FilterChip(
                        selected = settings.speechVoice == tag,
                        onClick = { viewModel.updateSpeechVoice(tag) },
                        label = { Text(label) },
                    )
                }
            }

            Text("深色模式", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                    Button(
                        onClick = { viewModel.updateDarkMode(mode) },
                        enabled = settings.darkMode != mode,
                    ) { Text(label) }
                }
            }

            Text("主题色（一级/二级强调色）", style = MaterialTheme.typography.titleMedium)
            Text(
                "选择后立即生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val selectedOption = com.ley.wordmemo.ui.theme.ThemeOptions.resolve(
                settings.primaryColor, settings.secondaryColor
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(com.ley.wordmemo.ui.theme.ThemeOptions.all.size) { idx ->
                    val opt = com.ley.wordmemo.ui.theme.ThemeOptions.all[idx]
                    val isSelected = opt == selectedOption
                    androidx.compose.material3.Card(
                        modifier = Modifier.width(96.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme(opt)
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(opt.primary, CircleShape)
                                )
                                Spacer(Modifier.size(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(opt.secondary, CircleShape)
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(
                                opt.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // ===== 自定义取色 =====
            Spacer(Modifier.size(16.dp))
            Text("自定义配色（自由取色）", style = MaterialTheme.typography.titleMedium)

            // 实时预览卡
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("主题预览", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.ley.wordmemo.ui.theme.ThemeOptions.resolve(
                                    settings.primaryColor, settings.secondaryColor
                                ).primary,
                            ),
                        ) { Text("主要按钮") }
                        FilledTonalButton(
                            onClick = {},
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = com.ley.wordmemo.ui.theme.ThemeOptions.resolve(
                                    settings.primaryColor, settings.secondaryColor
                                ).secondary,
                            ),
                        ) { Text("次要按钮") }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))

            // 一级色
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color(settings.primaryColor.toInt()),
                            CircleShape,
                        )
                )
                Spacer(Modifier.size(10.dp))
                Text("一级强调色", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    showPicker = "primary" to settings.primaryColor
                }) { Text("取色") }
            }
            // 二级色
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Color(settings.secondaryColor.toInt()),
                            CircleShape,
                        )
                )
                Spacer(Modifier.size(10.dp))
                Text("二级强调色", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    showPicker = "secondary" to settings.secondaryColor
                }) { Text("取色") }
            }
            Text(
                "提示：二级色建议用一级色降饱和/降亮度获得，整体更协调",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    // 恢复默认靛蓝
                    viewModel.updateTheme(com.ley.wordmemo.ui.theme.ThemeOptions.Indigo)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("恢复默认主题") }
        }
    }

    // ===== 自定义取色对话框 =====
    showPicker?.let { (level, argb) ->
        val initial = if (argb != 0L) Color(argb.toInt()) else Color(0xFF4F46E5)
        ColorPickerDialog(
            title = if (level == "primary") "选择一级强调色" else "选择二级强调色",
            initialColor = initial,
            onConfirm = { color ->
                val argb = color.toArgb().toLong() and 0xFFFFFFFFL
                if (level == "primary") {
                    viewModel.updatePrimary(color)
                } else {
                    viewModel.updateSecondary(color)
                }
                showPicker = null
            },
            onDismiss = { showPicker = null },
        )
    }
}

