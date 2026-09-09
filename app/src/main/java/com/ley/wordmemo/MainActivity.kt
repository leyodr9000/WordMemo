package com.ley.wordmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ley.wordmemo.ui.navigation.AppNavHost
import com.ley.wordmemo.ui.theme.WordMemoMiuixTheme
import com.ley.wordmemo.ui.theme.WordMemoTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.ley.wordmemo.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.darkMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val isMiuix = settings.uiStyle == "miui"
            val appContent: @Composable () -> Unit = {
                AppNavHost(
                    navController = rememberNavController(),
                    uiStyle = settings.uiStyle,
                )
            }
            if (isMiuix) {
                // 真 Miuix (HyperOS): KernelSU 系管理器同款组件体系
                WordMemoMiuixTheme(
                    darkTheme = darkTheme,
                    customPrimary = settings.primaryColor,
                ) {
                    WallpaperLayer(uri = settings.backgroundUri, dark = darkTheme, content = appContent)
                }
            } else {
                WordMemoTheme(
                    darkMode = settings.darkMode,
                    customPrimary = settings.primaryColor,
                    customSecondary = settings.secondaryColor,
                ) {
                    WallpaperLayer(uri = settings.backgroundUri, dark = darkTheme, content = appContent)
                }
            }
        }
    }
}

/** 自定义背景壁纸 (参考网页版自定义背景): 图片 + 深浅色蒙层, Material3 容器透明化 */
@Composable
private fun WallpaperLayer(
    uri: String,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    if (uri.isBlank()) {
        content()
        return
    }
    val scrim = if (dark) Color(0xFF14161B).copy(alpha = 0.84f)
                else Color(0xFFF5F6F8).copy(alpha = 0.86f)
    val transparentScheme = MaterialTheme.colorScheme.copy(
        background = Color.Transparent,
        surface = Color.Transparent,
    )
    androidx.compose.material3.MaterialTheme(
        colorScheme = transparentScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(scrim)
            )
            content()
        }
    }
}
