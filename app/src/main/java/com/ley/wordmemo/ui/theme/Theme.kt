package com.ley.wordmemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EFB8),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF52634F),
    tertiary = Color(0xFF39656B),
    background = Color(0xFFF6FBF1),
    surface = Color(0xFFF6FBF1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD59D),
    onPrimary = Color(0xFF00390C),
    primaryContainer = Color(0xFF155221),
    onPrimaryContainer = Color(0xFFB7EFB8),
    secondary = Color(0xFFB9CCB3),
    tertiary = Color(0xFF9CCFD6),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
)

@Composable
fun WordMemoTheme(
    darkMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    // API 31+ 支持动态取色，但保持品牌绿一致性，这里关闭动态色
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}