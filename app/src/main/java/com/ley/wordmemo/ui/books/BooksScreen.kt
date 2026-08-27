package com.ley.wordmemo.ui.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ley.wordmemo.data.model.BookStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onBack: (() -> Unit)?,
    viewModel: BooksViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val active by viewModel.activeBook.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<BookStat?>(null) }
    var deleteTarget by remember { mutableStateOf<BookStat?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词书管理") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建词书")
            }
        },
    ) { padding ->
        if (!loaded) {
            // 首次加载完成前
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.size(12.dp))
                Text("正在加载词书…", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 「全部」入口
            item {
                BookRow(
                    book = BookStat(book = "全部", total = books.sumOf { it.total }),
                    isActive = active.isEmpty(),
                    onSelect = { viewModel.selectBook("") },
                    onRename = null,
                    onDelete = null,
                )
            }
            if (books.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.MenuBook, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "暂无词书，点右下角 + 新建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(books, key = { it.book }) { stat ->
                    BookRow(
                        book = stat,
                        isActive = active == stat.book,
                        onSelect = { viewModel.selectBook(stat.book) },
                        onRename = { renameTarget = stat },
                        onDelete = { deleteTarget = stat },
                    )
                }
            }
        }
    }

    if (showCreate) {
        BookNameDialog(
            title = "新建词书",
            confirmText = "创建",
            initial = "",
            onConfirm = { name ->
                viewModel.createBook(name) { ok ->
                    if (ok) showCreate = false
                }
            },
            onDismiss = { showCreate = false },
        )
    }
    renameTarget?.let { target ->
        BookNameDialog(
            title = "重命名词书",
            confirmText = "重命名",
            initial = target.book,
            onConfirm = { name ->
                viewModel.renameBook(target.book, name) { renameTarget = null }
            },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除词书") },
            text = { Text("确定删除「${target.book}」及其全部 ${target.total} 个单词？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(target.book) { deleteTarget = null }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BookRow(
    book: BookStat,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onSelect),
        leadingContent = {
            if (isActive) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "当前词书",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(Icons.Default.MenuBook, contentDescription = null)
            }
        },
        headlineContent = {
            Text(
                book.book,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (isActive) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.bodyLarge,
            )
        },
        supportingContent = {
            Column {
                Text("共 ${book.total} 词 · 生词 ${book.newCount} · 熟练 ${book.masteredCount} · 忘记 ${book.forgottenCount}")
                Spacer(Modifier.size(4.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        trailingContent = {
            Row {
                onRename?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名")
                    }
                }
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BookNameDialog(
    title: String,
    confirmText: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("词书名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

