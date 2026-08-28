package com.ley.wordmemo.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ley.wordmemo.ui.books.BooksScreen
import com.ley.wordmemo.ui.home.HomeListScreen
import com.ley.wordmemo.ui.reader.ReaderScreen
import com.ley.wordmemo.ui.importwords.ImportScreen
import com.ley.wordmemo.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 主 Tab 容器：底部 4 个按钮 = 左右平移切换页面（HorizontalPager + NavigationBar）
 * 性能优化：
 *  - beyondViewportPageCount=0 不预渲染相邻页
 *  - key(page) 让每页重组局部化
 *  - settledPage 用于选中态, 滑动中选中不闪动
 *  - 点击用 scrollToPage 瞬切 (不用 animateScrollToPage, 避免与手势动画叠加掉帧)
 */
@Composable
fun MainTabsScreen(
    onOpenStudy: () -> Unit,
    navController: androidx.navigation.NavHostController,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val settledPage by remember { derivedStateOf { pagerState.settledPage } }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true,
            beyondViewportPageCount = 0,
        ) { page ->
            key(page) {
                when (page) {
                    0 -> HomeListScreen(onOpenStudy = onOpenStudy)
                    1 -> BooksScreen(
                        onBack = null,
                        onOpenImport = { mode ->
                            when (mode) {
                                "reader" -> navController.navigate("reader")
                                "ai" -> navController.navigate("ai?word=&meaning=")
                                "json" -> {}  // 本地按钮处理
                                else -> navController.navigate("import?mode=$mode")
                            }
                        },
                    )
                    2 -> ReaderScreen(onBack = {})
                    3 -> SettingsScreen(onBack = null)
                }
            }
        }
        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            NavigationBarItem(
                selected = settledPage == 0,
                onClick = { scope.launch { pagerState.scrollToPage(0) } },
                icon = { Icon(Icons.Default.School, null) },
                label = { Text("列表") },
            )
            NavigationBarItem(
                selected = settledPage == 1,
                onClick = { scope.launch { pagerState.scrollToPage(1) } },
                icon = { Icon(Icons.Default.MenuBook, null) },
                label = { Text("词书") },
            )
            NavigationBarItem(
                selected = settledPage == 2,
                onClick = { scope.launch { pagerState.scrollToPage(2) } },
                icon = { Icon(Icons.Default.AutoStories, null) },
                label = { Text("阅读") },
            )
            NavigationBarItem(
                selected = settledPage == 3,
                onClick = { scope.launch { pagerState.scrollToPage(3) } },
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("设置") },
            )
        }
    }
}

