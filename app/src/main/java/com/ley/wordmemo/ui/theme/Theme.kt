package com.ley.wordmemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// ============ 高级配色（低饱和、耐看） ============
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),          // Indigo-600 靛蓝
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo-100
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF64748B),        // Slate-500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0D9488),         // Teal-600
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF8FAFC),       // 冷白
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),          // Indigo-400
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF94A3B8),        // Slate-400
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF2DD4BF),         // Teal-400
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF0F172A),       // 深蓝黑
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
)

/** 预设主题色板（低饱和高级感） */
data class ThemeColorOption(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
) {
    val primaryArgb: Long get() = primary.toArgb().toLong() and 0xFFFFFFFFL
    val secondaryArgb: Long get() = secondary.toArgb().toLong() and 0xFFFFFFFFL

    fun matches(customPrimary: Long, customSecondary: Long): Boolean =
        customPrimary != 0L && customPrimary == primaryArgb &&
            customSecondary != 0L && customSecondary == secondaryArgb
}

object ThemeOptions {
    val Indigo = ThemeColorOption("靛蓝", Color(0xFF4F46E5), Color(0xFF64748B), Color(0xFF0D9488))
    val Ocean = ThemeColorOption("海洋", Color(0xFF0284C7), Color(0xFF64748B), Color(0xFF0D9488))
    val Sand = ThemeColorOption("沙丘", Color(0xFFB45309), Color(0xFF8B5E34), Color(0xFF4B5563))
    val Violet = ThemeColorOption("紫晶", Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFF0EA5E9))
    val Rose = ThemeColorOption("玫瑰", Color(0xFFBE185D), Color(0xFF9D174D), Color(0xFF475569))
    val Night = ThemeColorOption("夜幕", Color(0xFF1E3A5F), Color(0xFF334155), Color(0xFF0D9488))
    val Sage = ThemeColorOption("鼠尾草", Color(0xFF3F6212), Color(0xFF6B7280), Color(0xFF0F766E))

    val all = listOf(Indigo, Ocean, Sand, Violet, Rose, Night, Sage)

    fun resolve(customPrimary: Long, customSecondary: Long): ThemeColorOption =
        all.firstOrNull { it.matches(customPrimary, customSecondary) } ?: Indigo
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