package com.ley.wordmemo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/**
 * Miuix (HyperOS 风格, KernelSU 系管理器同款组件库) 与 Material3 的双主题桥。
 *
 * 「MIUI X」界面模式下:
 *  - 外层 MiuixTheme 提供真正的 HyperOS 组件配色 (miuix NavigationBar/Card/SuperSwitch 等)
 *  - 内层 MaterialTheme 使用 [miuixToMaterialScheme] 映射后的配色,
 *    让既有 Material3 屏幕无缝继承同一套 HyperOS 色板
 */

/** 构建 Miuix 配色: 用户自定义种子色或 HyperOS 默认蓝 */
fun buildMiuixColors(darkTheme: Boolean, customPrimary: Long): Colors {
    val seed = longToSeed(customPrimary)
    val base = if (darkTheme) miuixDarkColorScheme() else miuixLightColorScheme()
    if (seed == null) return base
    // 用户自定义一级强调色: 覆盖 primary / primaryVariant, 其余保持 HyperOS 派生
    return base.copy(primary = seed, primaryVariant = seed)
}

/** Miuix Colors -> Material3 ColorScheme */
fun miuixToMaterialScheme(c: Colors, darkTheme: Boolean): ColorScheme {
    val builder: (Color, Color) -> ColorScheme =
        if (darkTheme) ::darkColorScheme else ::lightColorScheme
    return builder(
        c.primary, c.onPrimary,
    ).copy(
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.primaryVariant,
        onTertiary = c.onPrimaryVariant,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        background = c.background,
        onBackground = c.onBackground,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceSecondary,
        outline = c.outline,
    )
}

/** 双主题包裹: MiuixTheme(真 HyperOS) + MaterialTheme(映射色板) */
@Composable
fun WordMemoMiuixTheme(
    darkTheme: Boolean,
    customPrimary: Long,
    content: @Composable () -> Unit,
) {
    // remember 化: 避免每次重组重建整套配色 (性能, 参考 KernelSU 实践)
    val miuixColors = remember(darkTheme, customPrimary) {
        buildMiuixColors(darkTheme, customPrimary)
    }
    val materialScheme = remember(miuixColors, darkTheme) {
        miuixToMaterialScheme(miuixColors, darkTheme)
    }
    top.yukonga.miuix.kmp.theme.MiuixTheme(colors = miuixColors) {
        androidx.compose.material3.MaterialTheme(colorScheme = materialScheme) {
            content()
        }
    }
}
