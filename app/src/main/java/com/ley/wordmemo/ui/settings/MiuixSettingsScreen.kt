package com.ley.wordmemo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider as M3Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ley.wordmemo.ui.components.ColorPickerDialog
import com.ley.wordmemo.ui.theme.ThemeOptions
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「MIUI X」界面模式下的设置页 — 真 Miuix (HyperOS) 组件实现:
 * Scaffold / SmallTopAppBar / TabRow / Card / SuperSwitch / Slider / TextField。
 * 功能与 Material3 版设置页完全对齐。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MiuixSettingsScreen(
    onBack: (() -> Unit)?,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apiForm by viewModel.apiForm.collectAsStateWithLifecycle()
    val modelsState by viewModel.modelsState.collectAsStateWithLifecycle()
    var keyVisible by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = { onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "返回",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
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
            TabRow(
                tabs = listOf("AI 配置", "学习", "外观"),
                selectedTabIndex = tab,
                onTabSelected = { tab = it },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> {
                        SmallTitle("AI 识图 API（兼容 OpenAI 格式）")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                TextField(
                                    value = apiForm.baseUrl,
                                    onValueChange = viewModel::onBaseUrlChange,
                                    label = "Base URL",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextField(
                                    value = apiForm.apiKey,
                                    onValueChange = viewModel::onKeyChange,
                                    label = "API Key",
                                    singleLine = true,
                                    visualTransformation = if (keyVisible)
                                        androidx.compose.ui.text.input.VisualTransformation.None
                                    else
                                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { keyVisible = !keyVisible }) {
                                            Icon(
                                                if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (keyVisible) "隐藏" else "显示",
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextField(
                                    value = apiForm.model,
                                    onValueChange = viewModel::onModelChange,
                                    label = "模型名",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        SmallTitle("可用模型（自动拉取）")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("点击「获取」拉取 /models 列表", style = MaterialTheme.typography.bodySmall)
                                    Button(
                                        onClick = { viewModel.fetchModels() },
                                        enabled = !modelsState.loading,
                                    ) {
                                        if (modelsState.loading) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(if (modelsState.loading) "拉取中…" else "获取可用模型")
                                    }
                                }
                                modelsState.error?.let {
                                    Spacer(Modifier.size(6.dp))
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                if (modelsState.models.isNotEmpty()) {
                                    Spacer(Modifier.size(8.dp))
                                    FlowRow(
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
                                }
                            }
                        }

                        SmallTitle("AI 助教人设（System Prompt）")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                var persona by remember(settings.chatPersona) { mutableStateOf(settings.chatPersona) }
                                OutlinedTextField(
                                    value = persona,
                                    onValueChange = { persona = it },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        persona = com.ley.wordmemo.data.settings.AppSettings.DEFAULT_CHAT_PERSONA
                                    }) { Text("恢复默认") }
                                    Button(
                                        onClick = { viewModel.updateChatPersona(persona) },
                                        enabled = persona.isNotBlank() && persona != settings.chatPersona,
                                    ) { Text("保存人设") }
                                }
                                var temp by remember(settings.temperature) { mutableStateOf(settings.temperature.toFloat()) }
                                Text(
                                    "对话温度：${"%.1f".format(temp)}  （低=稳定严谨，高=发散有趣）",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Slider(
                                    value = temp,
                                    onValueChange = { temp = it },
                                    onValueChangeFinished = { viewModel.updateTemperature(temp.toDouble()) },
                                    valueRange = 0f..1.5f,
                                )
                            }
                        }

                        apiForm.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        apiForm.notice?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { viewModel.saveApi() },
                            enabled = !apiForm.saving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (apiForm.saving) {
                                Text("保存中…")
                            } else {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (apiForm.saved) "已保存 ✓" else "保存配置")
                            }
                        }
                    }

                    1 -> {
                        SmallTitle("学习")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SuperSwitch(
                                    checked = settings.autoSpeak,
                                    onCheckedChange = { viewModel.updateAutoSpeak(it) },
                                    title = "自动发音",
                                    summary = "切换卡片时自动朗读当前单词",
                                )
                                SuperSwitch(
                                    checked = settings.selfTest,
                                    onCheckedChange = { viewModel.updateSelfTest(it) },
                                    title = "自测模式",
                                    summary = "翻转前隐藏释义，先回忆再对照",
                                )
                                SuperSwitch(
                                    checked = settings.hideMasteredTranslation,
                                    onCheckedChange = { viewModel.updateHideMasteredTranslation(it) },
                                    title = "隐藏熟练词翻译",
                                    summary = "熟练词释义模糊显示，点击揭示",
                                )
                            }
                        }

                        SmallTitle("每日目标")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                var goalValue by remember(settings.dailyGoal) {
                                    mutableStateOf(settings.dailyGoal.toFloat())
                                }
                                Text("每日目标：${goalValue.toInt()}")
                                Slider(
                                    value = goalValue,
                                    onValueChange = { goalValue = it },
                                    onValueChangeFinished = { viewModel.updateDailyGoal(goalValue.toInt()) },
                                    valueRange = 5f..100f,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(10, 20, 30, 50).forEach { goal ->
                                        Button(
                                            onClick = { viewModel.updateDailyGoal(goal) },
                                            enabled = settings.dailyGoal != goal,
                                        ) { Text("$goal") }
                                    }
                                }
                            }
                        }

                        SmallTitle("发音 / 动画 / 翻译源")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ChipRow(
                                    label = "翻译源",
                                    options = listOf("offline" to "📖 内置词典（离线）", "ai" to "🤖 AI 翻译"),
                                    selected = settings.translationSource,
                                    onSelect = { viewModel.updateTranslationSource(it) },
                                )
                                ChipRow(
                                    label = "卡片切换动画",
                                    options = listOf("slide" to "左右平移", "flip" to "翻转", "scale" to "缩放", "fade" to "淡入淡出"),
                                    selected = settings.cardAnimation,
                                    onSelect = { viewModel.updateCardAnimation(it) },
                                )
                                ChipRow(
                                    label = "发音音色",
                                    options = listOf("默认" to "", "英音 A" to "en-GB", "英音 B" to "en-GB-x-isa", "美音 A" to "en-US", "美音 B" to "en-US-x-iwz"),
                                    selected = settings.speechVoice,
                                    onSelect = { viewModel.updateSpeechVoice(it) },
                                )
                            }
                        }
                    }

                    2 -> {
                        SmallTitle("外观")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ChipRow(
                                    label = "界面风格（MIUI X = 真 Miuix HyperOS）",
                                    options = listOf("monet" to "🌿 Material You", "miui" to "💠 MIUI X"),
                                    selected = settings.uiStyle,
                                    onSelect = { viewModel.updateUiStyle(it) },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                                        Button(
                                            onClick = { viewModel.updateDarkMode(mode) },
                                            enabled = settings.darkMode != mode,
                                        ) { Text(label) }
                                    }
                                }
                            }
                        }

                        SmallTitle("主题色（一级/二级强调色）")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                val selectedOption = ThemeOptions.resolve(settings.primaryColor, settings.secondaryColor)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(ThemeOptions.all.size) { idx ->
                                        val opt = ThemeOptions.all[idx]
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
                                                    .clickable { viewModel.updateTheme(opt) }
                                                    .padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Row {
                                                    Box(Modifier.size(22.dp).background(opt.primary, CircleShape))
                                                    Spacer(Modifier.size(4.dp))
                                                    Box(Modifier.size(22.dp).background(opt.secondary, CircleShape))
                                                }
                                                Spacer(Modifier.size(6.dp))
                                                Text(opt.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.size(10.dp))
                                PickColorRow("一级强调色", settings.primaryColor) { showPicker = "primary" to settings.primaryColor }
                                PickColorRow("二级强调色", settings.secondaryColor) { showPicker = "secondary" to settings.secondaryColor }
                                Spacer(Modifier.size(8.dp))
                                Button(
                                    onClick = { viewModel.updateTheme(ThemeOptions.Indigo) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("恢复默认主题") }
                            }
                        }

                        SmallTitle("自定义背景壁纸")
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val pickBackground = androidx.activity.compose.rememberLauncherForActivityResult(
                                    androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                                ) { uri ->
                                    if (uri != null) {
                                        runCatching {
                                            context.contentResolver.takePersistableUriPermission(
                                                uri,
                                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                            )
                                        }
                                        viewModel.updateBackgroundUri(uri.toString())
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { pickBackground.launch(arrayOf("image/*")) }) {
                                        Icon(Icons.Default.Image, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("选择背景图")
                                    }
                                    Button(
                                        onClick = { viewModel.updateBackgroundUri("") },
                                        enabled = settings.backgroundUri.isNotBlank(),
                                    ) { Text("清除背景") }
                                }
                                if (settings.backgroundUri.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = settings.backgroundUri,
                                        contentDescription = "背景预览",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showPicker?.let { (level, argb) ->
        val initial = if (argb != 0L) Color(argb.toInt()) else Color(0xFF4F46E5)
        ColorPickerDialog(
            title = if (level == "primary") "选择一级强调色" else "选择二级强调色",
            initialColor = initial,
            onConfirm = { color ->
                if (level == "primary") viewModel.updatePrimary(color) else viewModel.updateSecondary(color)
                showPicker = null
            },
            onDismiss = { showPicker = null },
        )
    }
}

@Composable
private fun PickColorRow(label: String, argb: Long, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(argb.toInt()), CircleShape)
        )
        Spacer(Modifier.size(10.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onPick) { Text("取色") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (key, text) ->
            androidx.compose.material3.FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(text) },
            )
        }
    }
}
