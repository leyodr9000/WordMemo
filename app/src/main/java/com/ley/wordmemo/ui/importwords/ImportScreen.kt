package com.ley.wordmemo.ui.importwords

import android.net.Uri
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingWords.collectAsStateWithLifecycle()
    val book by viewModel.selectedBook.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun copyToCache(uri: Uri): File? = runCatching {
        val dir = File(context.cacheDir, "ocr").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { out -> input.copyTo(out) }
        }
        file
    }.getOrNull()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { copyToCache(it)?.let { f -> viewModel.onImagePicked(f) } }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok: Boolean ->
        if (ok) cameraUri?.let { uri ->
            copyToCache(uri)?.let { f -> viewModel.onImagePicked(f) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍照导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                OutlinedButton(onClick = {
                    cameraUri = androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider",
                        File(context.cacheDir, "camera_tmp.jpg").apply {
                            parentFile?.mkdirs()
                        }
                    )
                    cameraUri?.let { takePicture.launch(it) }
                }) {
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
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.size(12.dp))
                        Text("AI 识别中…")
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
                        Button(onClick = onBack) { Text("完成") }
                    }
                }
                else -> {
                    Text("选择书页照片，AI 将自动提取单词", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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