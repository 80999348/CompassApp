package com.example.compass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compass.sensor.CompassViewModel
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun LevelScreen(vm: CompassViewModel) {
    val g = vm.gravity
    val tiltX = g[0] / 9.81f   // 左右倾斜
    val tiltY = -g[1] / 9.81f  // 前后倾斜
    val tiltDeg = Math.toDegrees(hypot(tiltX.toDouble(), tiltY.toDouble()))

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(Color(0xFF141A22), radius = r, center = c)
            for (ring in floatArrayOf(1f, 0.66f, 0.33f)) {
                drawCircle(
                    Color(0xFF2A3440), radius = r * ring,
                    center = c, style = Stroke(2.dp.toPx())
                )
            }
            drawLine(Color(0xFF2A3440), c - Offset(r, 0f), c + Offset(r, 0f), 1.5f)
            drawLine(Color(0xFF2A3440), c - Offset(0f, r), c + Offset(0f, r), 1.5f)

            // 气泡：随倾斜移动，放大 3 倍便于观察
            val bx = c.x + tiltX * r * 0.9f
            val by = c.y + tiltY * r * 0.9f
            val bubble = Offset(bx.coerceIn(c.x - r * 0.85f, c.x + r * 0.85f),
                by.coerceIn(c.y - r * 0.85f, c.y + r * 0.85f))
            val level = tiltDeg < 1.0
            drawCircle(
                if (level) Color(0xFF66BB6A) else Color(0xFF4FC3F7),
                radius = 26f, center = bubble
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "%.1f°".format(tiltDeg),
            fontSize = 52.sp, fontWeight = FontWeight.Bold,
            color = if (tiltDeg < 1.0) Color(0xFF66BB6A) else Color(0xFFECEFF1)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                tiltDeg < 1.0 -> "✓ 完全水平"
                abs(tiltX) > abs(tiltY) && tiltX > 0 -> "向右倾斜"
                abs(tiltX) > abs(tiltY) -> "向左倾斜"
                tiltY > 0 -> "向上倾斜"
                else -> "向下倾斜"
            },
            fontSize = 18.sp, color = Color(0xFF90A4AE)
        )
        Spacer(Modifier.height(16.dp))
        Text("左右 %+.1f°   前后 %+.1f°".format(
            Math.toDegrees(tiltX.toDouble()),
            Math.toDegrees(tiltY.toDouble())),
            fontSize = 14.sp, color = Color(0xFF607D8B)
        )
    }
}
