package com.ley.wordmemo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MIUI X 风格主题:
 * - MIUI 蓝主色 (#1967D2 / 浅 #0A84FF)
 * - 大圆角卡片 (MIUI 标志性圆润)
 * - 浅色冷白底 + 柔和灰面
 */
private val MiuiLight = lightColorScheme(
    primary = Color(0xFF1967D2),          // MIUI 蓝
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E6FF),
    onPrimaryContainer = Color(0xFF0A3A75),
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F0F5),
    onSecondaryContainer = Color(0xFF20343B),
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF3E0B53),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1B1F27),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F27),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5F6672),
    outline = Color(0xFFB4BAC4),
)

private val MiuiDark = darkColorScheme(
    primary = Color(0xFF8AB4F8),          // MIUI 暗色蓝
    onPrimary = Color(0xFF0A1F45),
    primaryContainer = Color(0xFF1B3F77),
    onPrimaryContainer = Color(0xFFD7E6FF),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF1C2A31),
    secondaryContainer = Color(0xFF354B54),
    onSecondaryContainer = Color(0xFFE1F0F5),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF3E0B53),
    tertiaryContainer = Color(0xFF5D2A6E),
    onTertiaryContainer = Color(0xFFF3E5F5),
    background = Color(0xFF12151B),
    onBackground = Color(0xFFE3E7ED),
    surface = Color(0xFF1A1E26),
    onSurface = Color(0xFFE3E7ED),
    surfaceVariant = Color(0xFF262B34),
    onSurfaceVariant = Color(0xFF9AA2AE),
    outline = Color(0xFF4A525E),
)

/** MIUI 标志性大圆角 */
private val MiuiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MiuiXTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MiuiDark else MiuiLight,
        shapes = MiuiShapes,
        content = content,
    )
}
