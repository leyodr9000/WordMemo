package com.ley.wordmemo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 环层数据: 名称/值/颜色 */
data class RingLayer(
    val label: String,
    val value: Float,   // 0..1
    val color: Color,
)

/**
 * 多层进度环 (运动手环样式):
 * 每层一个同心圆环, 各有配色与插值动画; 中央显示总进度百分比。
 */
@Composable
fun MultiRing(
    layers: List<RingLayer>,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,        // 环粗适当收窄, 三层不挤
    centerLabel: String = "掌握率",
) {
    val animatedLayers = layers.map {
        val a by animateFloatAsState(
            targetValue = it.value.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 900),
            label = "ring-${it.label}",
        )
        it.copy(value = a)
    }

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val stroke = strokeWidth.toPx()
            val count = animatedLayers.size.coerceAtLeast(1)
            // 层间距: 明显分开, 避免「重叠」观感
            val gap = stroke * 0.35f + 2f
            // 最内层半径: 至少留出中央文字空间
            val minInner = (sizeDp.value * 0.20f).dp.toPx()
            // 最外层半径: 留边距
            val maxRadius = (size.minDimension / 2f) - stroke - 2f
            animatedLayers.forEachIndexed { i, layer ->
                // 从外到内: 外层 i=0 用最大半径
                val radius = maxRadius - i * (stroke + gap)
                if (radius < minInner) return@forEachIndexed
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                // 底环 (全圆, 深一点)
                drawCircle(
                    color = layer.color.copy(alpha = 0.15f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // 进度弧 (从顶部顺时针)
                drawArc(
                    color = layer.color,
                    startAngle = -90f,
                    sweepAngle = 360f * layer.value,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                )
            }
        }
        // 中央: 总百分比 (带动画)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val total = animatedLayers.map { it.value }.average().toFloat()
            val pctText by animateFloatAsState(
                targetValue = total,
                animationSpec = tween(durationMillis = 900),
                label = "centerPct",
            )
            Text(
                text = "${(pctText * 100).toInt()}%",
                fontSize = 15.sp,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = centerLabel,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}