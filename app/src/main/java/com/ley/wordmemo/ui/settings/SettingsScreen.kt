package com.ley.wordmemo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apiForm by viewModel.apiForm.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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

            Text("深色模式", style = MaterialTheme.typography.titleMedium)
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
}