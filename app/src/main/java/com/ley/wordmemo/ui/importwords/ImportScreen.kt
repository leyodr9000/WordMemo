package com.ley.wordmemo.ui.importwords

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.ley.wordmemo.data.api.ExtractedWord
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImportScreen(
    onBack: (() -> Unit)?,
    initialMode: String = "",   // 从加号菜单进入: "camera" | "gallery" | ""
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentImage by viewModel.currentImage.collectAsStateWithLifecycle()
    val pending by viewModel.pendingWords.collectAsStateWithLifecycle()
    val book by viewModel.selectedBook.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    fun copyToCache(uri: Uri): File? = runCatching {
        val dir = File(context.cacheDir, "ocr").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { out -> input.copyTo(out) }
        }
        file
    }.getOrNull()

    // 页面提示文案（拍照失败等反馈）
    var hintText by remember { mutableStateOf("") }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            hintText = ""
            copyToCache(uri)?.let { f -> viewModel.onImagePicked(f) }
        } else {
            hintText = "未选择图片"
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean ->
        if (ok) {
            hintText = ""
            cameraUri?.let { uri ->
                copyToCache(uri)?.let { f -> viewModel.onImagePicked(f) }
            }
        } else {
            // 相机不可用 / 用户取消 / 拍照失败: 必须给反馈, 否则"毫无反应"
            hintText = "相机不可用或已取消，请改用「相册」选择书页照片"
        }
    }

    fun startCamera() {
        val tmp = File(context.cacheDir, "camera_tmp.jpg").apply {
            parentFile?.mkdirs()
            if (!exists()) createNewFile()
        }
        cameraUri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", tmp
        )
        cameraUri?.let { takePicture.launch(it) }
    }

    /** 检查并请求相机权限, 授权后启动拍照 */
    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    // 从加号菜单进入时自动触发对应入口
    androidx.compose.runtime.LaunchedEffect(initialMode) {
        when (initialMode) {
            "camera" -> launchCamera()
            "gallery" -> pickImage.launch("image/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍照导入") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            // 1. 选词书
            OutlinedTextField(
                value = book,
                onValueChange = { viewModel.selectedBook.value = it },
                label = { Text("来源词书（可留空=默认词库）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.size(12.dp))

            // 2. 选择图片来源
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { launchCamera() }) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.size(6.dp))
                    Text("拍照")
                }
                OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.size(6.dp))
                    Text("相册")
                }
            }
            Spacer(Modifier.size(12.dp))

            when (val s = state) {
                is ImportState.NoApi -> Text("请先在「设置」中配置 AI API", color = MaterialTheme.colorScheme.error)
                is ImportState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                is ImportState.Recognizing -> {
                    // AI 对话框式可视化: 上传→分析→解析 步骤
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("📖 AI 识别中", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(16.dp))
                        RecognizeStep(
                            step = 0,
                            current = s.stage,
                            title = "上传图片",
                            desc = "将书页图片发送给 AI",
                        )
                        Spacer(Modifier.size(10.dp))
                        RecognizeStep(
                            step = 1,
                            current = s.stage,
                            title = "AI 分析",
                            desc = "识别生词与释义",
                        )
                        Spacer(Modifier.size(10.dp))
                        RecognizeStep(
                            step = 2,
                            current = s.stage,
                            title = "解析词条",
                            desc = "整理为固定 JSON 格式",
                        )
                    }
                }
                is ImportState.Preview -> {
                    Text("识别到 ${s.words.size} 个单词，确认后导入：",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(pending) { index, w ->
                            if (w.word != "__REMOVE__") {
                                PreviewRow(
                                    word = w,
                                    onToggle = { viewModel.togglePending(index) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = { viewModel.importAll() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("导入 ${pending.count { it.word != "__REMOVE__" }} 个单词")
                    }
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("重新识别") }
                }
                is ImportState.Done -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("导入成功：${s.count} 个单词 🎉", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(12.dp))
                        Button(onClick = { onBack?.invoke() }) { Text("完成") }
                    }
                }
                else -> {
                    // 有已选图片: 显示预览 + 开始识别
                    if (currentImage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                val painter = rememberAsyncImagePainter(currentImage)
                                Image(
                                    painter = painter,
                                    contentDescription = "书页预览",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    contentScale = ContentScale.Fit,
                                )
                                Spacer(Modifier.size(12.dp))
                                Button(
                                    onClick = { viewModel.recognize() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Image, null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("开始识别")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.reset() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("重新选择") }
                            }
                        }
                    } else {
                        if (hintText.isNotEmpty()) {
                            Text(
                                hintText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                "选择书页照片，AI 将自动提取单词",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(word: ExtractedWord, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.word, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${word.partOfSpeech} ${word.meaning}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(Icons.Default.Close, "移除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
@Composable
private fun RecognizeStep(
    step: Int,
    current: Int,
    title: String,
    desc: String,
) {
    val (icon, color) = when {
        current > step -> "✅" to MaterialTheme.colorScheme.tertiary
        current == step -> "⏳" to MaterialTheme.colorScheme.primary
        else -> "⬜" to MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (current == step)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (current == step) {
                        Spacer(Modifier.size(8.dp))
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
