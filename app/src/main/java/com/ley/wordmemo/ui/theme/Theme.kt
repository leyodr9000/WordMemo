package com.ley.wordmemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EFB8),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CE),
    onSecondaryContainer = Color(0xFF101F0F),
    tertiary = Color(0xFF39656B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF002023),
    background = Color(0xFFF6FBF1),
    surface = Color(0xFFF6FBF1),
    surfaceVariant = Color(0xFFDFE4DA),
    onSurfaceVariant = Color(0xFF43483F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD59D),
    onPrimary = Color(0xFF00390C),
    primaryContainer = Color(0xFF155221),
    onPrimaryContainer = Color(0xFFB7EFB8),
    secondary = Color(0xFFB9CCB3),
    onSecondary = Color(0xFF243524),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8CE),
    tertiary = Color(0xFF9CCFD6),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF1F4D53),
    onTertiaryContainer = Color(0xFFBCEBF1),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BE),
)

/** 预设主题色板（一级/二级强调色可选） */
data class ThemeColorOption(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
) {
    /** ARGB Long，用于与 DataStore 存取 */
    val primaryArgb: Long get() = primary.toArgb().toLong() and 0xFFFFFFFFL
    val secondaryArgb: Long get() = secondary.toArgb().toLong() and 0xFFFFFFFFL

    fun matches(customPrimary: Long, customSecondary: Long): Boolean =
        customPrimary != 0L && customPrimary == primaryArgb &&
            customSecondary != 0L && customSecondary == secondaryArgb
}

object ThemeOptions {
    val Green = ThemeColorOption("薄荷绿", Color(0xFF2E7D32), Color(0xFF52634F), Color(0xFF39656B))
    val Blue = ThemeColorOption("蔚蓝", Color(0xFF1565C0), Color(0xFF546E7A), Color(0xFF00695C))
    val Orange = ThemeColorOption("暖橙", Color(0xFFE65100), Color(0xFF795548), Color(0xFF6D4C41))
    val Purple = ThemeColorOption("紫罗兰", Color(0xFF6A1B9A), Color(0xFF5E35B1), Color(0xFF37474F))
    val Pink = ThemeColorOption("樱粉", Color(0xFFC2185B), Color(0xFFE91E63), Color(0xFF8D6E63))
    val Dark = ThemeColorOption("曜石黑", Color(0xFF37474F), Color(0xFF455A64), Color(0xFF607D8B))
    val Red = ThemeColorOption("中国红", Color(0xFFC62828), Color(0xFFD32F2F), Color(0xFF6D4C41))

    val all = listOf(Green, Blue, Orange, Purple, Pink, Dark, Red)

    /** 根据 DataStore 里存的 ARGB 找到匹配预设；匹配不到用默认绿 */
    fun resolve(customPrimary: Long, customSecondary: Long): ThemeColorOption =
        all.firstOrNull { it.matches(customPrimary, customSecondary) } ?: Green
}

@Composable
fun WordMemoTheme(
    darkMode: String = "system",
    customPrimary: Long = 0L,
    customSecondary: Long = 0L,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val option = ThemeOptions.resolve(customPrimary, customSecondary)
    val base = if (darkTheme) DarkColors else LightColors
    val colorScheme = base.copy(
        primary = option.primary,
        secondary = option.secondary,
        tertiary = option.tertiary,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}