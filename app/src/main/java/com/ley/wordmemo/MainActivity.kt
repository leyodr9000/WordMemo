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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ley.wordmemo.ui.navigation.AppNavHost
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
            WordMemoTheme(
                darkMode = settings.darkMode,
                customPrimary = settings.primaryColor,
                customSecondary = settings.secondaryColor,
                uiStyle = settings.uiStyle,
            ) {
                val bgUri = settings.backgroundUri
                if (bgUri.isNotBlank()) {
                    // 自定义壁纸 (参考网页版自定义背景): 图片 + 深浅色蒙层, 主题背景透明化
                    val dark = when (settings.darkMode) {
                        "light" -> false
                        "dark" -> true
                        else -> isSystemInDarkTheme()
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
                                model = bgUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(scrim)
                            )
                            AppNavHost(navController = rememberNavController())
                        }
                    }
                } else {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }
}
