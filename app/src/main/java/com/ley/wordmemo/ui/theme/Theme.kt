package com.ley.wordmemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 预设初始配色（种子色对 primary/secondary）。
 * 整套 scheme 由 MonetPalette 从种子自动派生，保证对比度可读、全局一致。
 */
data class ThemeColorOption(
    val name: String,
    val primary: Color,      // 一级种子
    val secondary: Color,    // 二级种子
) {
    val primaryArgb: Long get() = primary.toArgb().toLong() and 0xFFFFFFFFL
    val secondaryArgb: Long get() = secondary.toArgb().toLong() and 0xFFFFFFFFL

    fun matches(customPrimary: Long, customSecondary: Long): Boolean =
        customPrimary != 0L && customPrimary == primaryArgb &&
            (customSecondary == 0L || customSecondary == secondaryArgb)
}

object ThemeOptions {
    // 7 套预设：低饱和、莫奈风格种子色
    val Indigo = ThemeColorOption("靛蓝", Color(0xFF4F46E5), Color(0xFF7C8CF8))
    val Ocean = ThemeColorOption("海洋", Color(0xFF0369A1), Color(0xFF0E9FBF))
    val Sand = ThemeColorOption("沙丘", Color(0xFFB45309), Color(0xFFD97706))
    val Violet = ThemeColorOption("紫晶", Color(0xFF7C3AED), Color(0xFFA78BFA))
    val Rose = ThemeColorOption("玫瑰", Color(0xFFBE185D), Color(0xFFF472B6))
    val Night = ThemeColorOption("夜幕", Color(0xFF1E3A5F), Color(0xFF3B82F6))
    val Sage = ThemeColorOption("鼠尾草", Color(0xFF3F6212), Color(0xFF65A30D))

    val all = listOf(Indigo, Ocean, Sand, Violet, Rose, Night, Sage)

    /** 解析当前存储的一二级色: 匹配预设则返回预设, 否则构造"自定义"选项 */
    fun resolve(customPrimary: Long, customSecondary: Long): ThemeColorOption {
        all.firstOrNull { it.matches(customPrimary, customSecondary) }?.let { return it }
        val p = longToSeed(customPrimary)
        val s = longToSeed(customSecondary)
        return ThemeColorOption(
            name = "自定义",
            primary = p ?: Indigo.primary,
            secondary = s ?: (p ?: Indigo.primary),
        )
    }
}

@Composable
fun WordMemoTheme(
    darkMode: String = "system",
    customPrimary: Long = 0L,
    customSecondary: Long = 0L,
    uiStyle: String = "monet",   // monet | miui
    content: @Composable () -> Unit,
) {
    val darkTheme = when (darkMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    // MIUI X 风格: 固定 MIUI 蓝 + 大圆角
    if (uiStyle == "miui") {
        MiuiXTheme(darkTheme = darkTheme, content = content)
        return
    }
    // 种子色: 用存储的自定义色, 无则默认靛蓝
    val primarySeed = longToSeed(customPrimary) ?: ThemeOptions.Indigo.primary
    val secondarySeed = longToSeed(customSecondary)

    // Monet 派生整套 scheme (深浅各一套), 保证全局可用且可读
    val colorScheme = if (darkTheme) {
        MonetPalette.darkScheme(primarySeed, secondarySeed)
    } else {
        MonetPalette.lightScheme(primarySeed, secondarySeed)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}