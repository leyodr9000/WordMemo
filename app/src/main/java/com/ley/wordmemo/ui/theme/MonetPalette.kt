package com.ley.wordmemo.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/**
 * Material You (Monet) 简化调色器：
 * 从一种子色自动派生整套 colorScheme，保证对比度可读、深浅两套。
 * 算法：基于 HSV 调色 + 固定明度梯度。
 */
object MonetPalette {

    private fun hsv(color: Color): FloatArray {
        val h = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), h)
        return h
    }

    private fun fromHsv(hue: Float, sat: Float, value: Float): Color =
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    private fun withValue(color: Color, v: Float): Color {
        val h = hsv(color)
        return fromHsv(h[0], h[1], v.coerceIn(0f, 1f))
    }

    private fun withSat(color: Color, s: Float): Color {
        val h = hsv(color)
        return fromHsv(h[0], s.coerceIn(0f, 1f), h[2])
    }

    /** 亮度 (perceived luminance 0..1) */
    private fun luminance(color: Color): Float {
        val c = color.toArgb()
        val r = android.graphics.Color.red(c) / 255f
        val g = android.graphics.Color.green(c) / 255f
        val b = android.graphics.Color.blue(c) / 255f
        fun lin(v: Float) = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * lin(r) + 0.7152f * lin(g) + 0.0722f * lin(b)
    }

    /** 对比度 (WCAG) */
    private fun contrast(a: Color, b: Color): Float {
        val l1 = luminance(a); val l2 = luminance(b)
        val hi = maxOf(l1, l2); val lo = minOf(l1, l2)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    /**
     * 从种子色生成完整亮色 scheme。
     * 保证: primary 与 onPrimary 对比度 >= 4.5 (可读性)
     */
    fun lightScheme(seed: Color, secondarySeed: Color? = null) = buildLight(seed, secondarySeed)

    fun darkScheme(seed: Color, secondarySeed: Color? = null) = buildDark(seed, secondarySeed)

    private fun buildLight(seed: Color, secondarySeed: Color? = null): androidx.compose.material3.ColorScheme {
        val hue = hsv(seed)[0]
        // primary: 用种子本身(通常已够深) 或压暗
        var primary = seed
        if (contrast(primary, Color.White) < 4.5f) {
            primary = withValue(seed, hsv(seed)[2] * 0.55f + 0.15f)
            // 仍不够就再压
            if (contrast(primary, Color.White) < 4.5f) primary = withValue(seed, 0.30f)
        }
        val onPrimary = Color.White

        // primaryContainer: 很浅的 tint (低饱和 + 高亮度)
        val lightTint = fromHsv(hue, 0.25f, 0.96f)
        val onPrimaryContainer = withValue(seed, 0.25f)

        // secondary: 用用户二级种子 或 从一级降饱和派生
        val secondary = secondarySeed?.let {
            val h2 = hsv(it)
            fromHsv(h2[0], (h2[1] * 0.8f + 0.2f).coerceAtMost(1f), 0.55f)
        } ?: fromHsv(hue, hsv(seed)[1] * 0.5f, 0.55f)
        val onSecondary = Color.White
        val secondaryContainer = fromHsv(hue, 0.20f, 0.88f)
        val onSecondaryContainer = withValue(seed, 0.30f)

        // tertiary: 色相偏移 +45 度做辅助色 (Monet 风格邻近色)
        val tertiaryHue = (hue + 45f) % 360f
        val tertiary = fromHsv(tertiaryHue, 0.5f, 0.55f)
        val onTertiary = Color.White
        val tertiaryContainer = fromHsv(tertiaryHue, 0.25f, 0.92f)
        val onTertiaryContainer = withValue(fromHsv(tertiaryHue, 0.4f, 0.9f), 0.25f)

        return lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = lightTint,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFF8FAFC),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFEDF1F7),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF94A3B8),
        )
    }

    private fun buildDark(seed: Color, secondarySeed: Color? = null): androidx.compose.material3.ColorScheme {
        val hue = hsv(seed)[0]
        val baseSat = hsv(seed)[1]

        // 深色模式: primary 用亮调 (值 >= 0.78 保证与深背景对比)
        val primary = fromHsv(hue, (baseSat * 0.85f + 0.15f).coerceAtMost(1f), 0.80f)
        val onPrimary = withValue(seed, 0.18f)

        val primaryContainer = fromHsv(hue, baseSat * 0.6f, 0.32f)
        val onPrimaryContainer = fromHsv(hue, 0.35f, 0.92f)

        val secondary = secondarySeed?.let {
            val h2 = hsv(it)
            fromHsv(h2[0], (h2[1] * 0.7f + 0.3f).coerceAtMost(1f), 0.72f)
        } ?: fromHsv(hue, baseSat * 0.5f, 0.72f)
        val onSecondary = withValue(seed, 0.15f)
        val secondaryContainer = fromHsv(hue, baseSat * 0.4f, 0.28f)
        val onSecondaryContainer = fromHsv(hue, 0.3f, 0.85f)

        val tertiaryHue = (hue + 45f) % 360f
        val tertiary = fromHsv(tertiaryHue, 0.5f, 0.75f)
        val onTertiary = withValue(fromHsv(tertiaryHue, 0.5f, 0.9f), 0.15f)
        val tertiaryContainer = fromHsv(tertiaryHue, 0.4f, 0.30f)
        val onTertiaryContainer = fromHsv(tertiaryHue, 0.3f, 0.9f)

        return darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = Color(0xFF0B1220),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF0B1220),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF475569),
        )
    }
}

private fun Float.pow(e: Float): Float = Math.pow(this.toDouble(), e.toDouble()).toFloat()

/** 种子色 -> ARGB Long (DataStore 存取) */
fun seedToLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL

fun longToSeed(argb: Long): Color? =
    if (argb == 0L) null else Color(argb.toInt())

/** 工具：十六进制与人阅读 */
fun colorToHexStr(color: Color): String =
    String.format("#%06X", 0xFFFFFF and color.toArgb())

fun hexStrToColor(hex: String): Color? {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6) return null
    return runCatching { Color(android.graphics.Color.parseColor("#$h")) }.getOrNull()
}