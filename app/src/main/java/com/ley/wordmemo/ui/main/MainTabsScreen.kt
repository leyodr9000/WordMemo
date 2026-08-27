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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ley.wordmemo.ui.books.BooksScreen
import com.ley.wordmemo.ui.home.HomeListScreen
import com.ley.wordmemo.ui.importwords.ImportScreen
import com.ley.wordmemo.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 主 Tab 容器：底部 4 个按钮 = 左右平移切换页面（HorizontalPager + NavigationBar）
 */
@Composable
fun MainTabsScreen(
    onOpenStudy: () -> Unit,   // 从列表页进入卡片学习（push）
    navController: NavHostController,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val currentPage by androidx.compose.runtime.derivedStateOf { pagerState.currentPage }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true, // 允许左右滑动平移
        ) { page ->
            when (page) {
                0 -> HomeListScreen(onOpenStudy = onOpenStudy)
                1 -> ImportScreen(onBack = null)
                2 -> BooksScreen(onBack = null)
                3 -> SettingsScreen(onBack = null)
            }
        }
        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            NavigationBarItem(
                selected = currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                icon = { Icon(Icons.Default.School, null) },
                label = { Text("列表") },
            )
            NavigationBarItem(
                selected = currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                icon = { Icon(Icons.Default.Add, null) },
                label = { Text("导入") },
            )
            NavigationBarItem(
                selected = currentPage == 2,
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                icon = { Icon(Icons.Default.MenuBook, null) },
                label = { Text("词书") },
            )
            NavigationBarItem(
                selected = currentPage == 3,
                onClick = { scope.launch { pagerState.animateScrollToPage(3) } },
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("设置") },
            )
        }
    }
}