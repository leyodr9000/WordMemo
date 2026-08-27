package com.ley.wordmemo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * HSV 取色器对话框：色相横条 + 饱和度/亮度盘 + 十六进制输入。
 * 基于 android.graphics.Color 的 HSV 工具实现（稳定可靠）。
 */
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember { colorToHsv(initialColor) }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }
    var hex by remember { mutableStateOf(colorToHex(initialColor)) }

    fun currentColor(): Color = Color(hsvToColor(hue, saturation, value))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("色相", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(4.dp))
                HueBar(hue = hue) { h ->
                    hue = h
                    hex = colorToHex(Color(hsvToColor(h, saturation, value)))
                }
                Spacer(Modifier.size(14.dp))

                Text("饱和度 / 亮度", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(4.dp))
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                ) { s, v ->
                    saturation = s; value = v
                    hex = colorToHex(Color(hsvToColor(hue, s, v)))
                }
                Spacer(Modifier.size(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(currentColor(), CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { input ->
                            hex = input
                            val c = parseHexColor(input)
                            if (c != null) {
                                val h = colorToHsv(c)
                                hue = h[0]; saturation = h[1]; value = h[2]
                            }
                        },
                        label = { Text("#RRGGBB") },
                        singleLine = true,
                        modifier = Modifier.width(150.dp),
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "二级强调色可先选一级色，再调低饱和度/亮度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentColor()) }) { Text("应用") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 色相横条 (0..360)，点击/拖动选色相 */
@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val h = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                    onHueChange(h)
                }
            }
    ) {
        val n = 18
        for (i in 0 until n) {
            val h = (360f / n) * i
            drawRect(
                color = Color(hsvToColor(h, 1f, 1f)),
                topLeft = Offset(size.width * i / n, 0f),
                size = androidx.compose.ui.geometry.Size(size.width / n + 1, size.height.toFloat()),
            )
        }
        // 指示点
        val x = (hue / 360f) * size.width
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(x.coerceIn(0f, size.width), size.height / 2f),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = 8.dp.toPx(),
            center = Offset(x.coerceIn(0f, size.width), size.height / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )
    }
}

/** 饱和度/亮度盘：x=饱和度, y=亮度(上亮下暗) */
@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSVChange: (Float, Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSVChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val n = 22
            for (ix in 0 until n) {
                for (iy in 0 until n) {
                    val s = ix / (n - 1).toFloat()
                    val v = 1f - iy / (n - 1).toFloat()
                    drawRect(
                        color = Color(hsvToColor(hue, s, v)),
                        topLeft = Offset(size.width * ix / n, size.height * iy / n),
                        size = androidx.compose.ui.geometry.Size(size.width / n + 1, size.height / n + 1),
                    )
                }
            }
            // 当前点
            val px = saturation * size.width
            val py = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(px.coerceIn(0f, size.width), py.coerceIn(0f, size.height)),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 8.dp.toPx(),
                center = Offset(px.coerceIn(0f, size.width), py.coerceIn(0f, size.height)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

// ===== HSV 工具 (基于 android.graphics.Color) =====

private fun colorToHsv(color: Color): FloatArray {
    val argb = color.toArgb()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

private fun hsvToColor(hue: Float, sat: Float, value: Float): Int {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return android.graphics.Color.HSVToColor(hsv)
}

fun colorToHex(color: Color): String {
    return String.format("#%06X", 0xFFFFFF and color.toArgb())
}

fun parseHexColor(hex: String): Color? {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6) return null
    return runCatching { Color(android.graphics.Color.parseColor("#$h")) }.getOrNull()
}