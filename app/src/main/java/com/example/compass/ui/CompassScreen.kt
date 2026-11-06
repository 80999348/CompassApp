package com.example.compass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compass.sensor.CompassViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(vm: CompassViewModel) {
    val heading = vm.smoothHeading
    // 动画插值，避免表盘抖动
    val animated by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(80),
        label = "dial"
    )
    var showAltDialog by remember { mutableStateOf(false) }

    if (showAltDialog) {
        AltitudeCalibrateDialog(
            current = vm.displayAltitude,
            hasGps = vm.gpsAltitude != null,
            onDismiss = { showAltDialog = false },
            onConfirm = { realAlt ->
                vm.calibrateAltitudeManually(realAlt)
                showAltDialog = false
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 顶部模式条
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = { vm.useTrueNorth = !vm.useTrueNorth },
                label = { Text(if (vm.useTrueNorth) "真北 (GNSS 修正)" else "磁北") }
            )
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        "磁场 ${vm.fieldStrength.toInt()} μT",
                        color = if (vm.fieldAbnormal) Color(0xFFEF5350) else Color(0xFF81C784)
                    )
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // 表盘
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0xFF141A22), radius = r, center = c)
            drawCircle(Color(0xFF2A3440), radius = r, center = c, style = Stroke(2.dp.toPx()))

            rotate(-animated, pivot = c) {
                // 刻度：每 2° 小刻度，每 15° 中刻度，每 45° 大刻度
                for (deg in 0 until 360 step 2) {
                    val isMajor = deg % 45 == 0
                    val isMid = deg % 15 == 0
                    val len = when {
                        isMajor -> 22f; isMid -> 14f; else -> 7f
                    }
                    val a = Math.toRadians(deg.toDouble())
                    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                    val inner = r - 10.dp.toPx()
                    val outer = r - 10.dp.toPx() - len * (size.minDimension / 400f)
                    drawLine(
                        if (isMajor) Color(0xFFECEFF1) else Color(0xFF78909C),
                        start = c + dir * inner,
                        end = c + dir * outer,
                        strokeWidth = if (isMajor) 3.dp.toPx() else 1.5f
                    )
                }
                // 东南西北四方位字样（随表盘旋转）
                val labels = mapOf(
                    0f to Triple("北", Color(0xFFE53935), 0.72f),   // 北 = 红色，醒目
                    90f to Triple("东", Color(0xFFB0BEC5), 0.66f),
                    180f to Triple("南", Color(0xFFB0BEC5), 0.66f),
                    270f to Triple("西", Color(0xFFB0BEC5), 0.66f)
                )
                val textPx = r * 0.16f
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = textPx
                    isFakeBoldText = true
                }
                for ((deg, item) in labels) {
                    val (label, color, radiusFactor) = item
                    val a = Math.toRadians((deg - 90f).toDouble())  // 0° 在正上方
                    val pos = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * r * radiusFactor
                    paint.color = color.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(label, pos.x, pos.y + textPx / 3f, paint)
                }
            }
            // 北向红色指针（固定向上）
            val n = Color(0xFFE53935)
            val s = Color(0xFFB0BEC5)
            val needleLen = r * 0.62f
            drawLine(n, c, c - Offset(0f, needleLen), 8f, cap = StrokeCap.Round)
            drawLine(s, c, c + Offset(0f, needleLen), 8f, cap = StrokeCap.Round)
            drawCircle(Color(0xFF263238), radius = 12f, center = c)
            drawCircle(Color(0xFF4FC3F7), radius = 6f, center = c)
        }

        // 数字读数（大号方位 + 角度，参考主流指南针布局）
        val deg = vm.heading.toInt()
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                CompassViewModel.directionName(vm.heading),
                fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFFECEFF1)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${deg}°",
                fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color(0xFFECEFF1)
            )
        }
        // 经纬度
        val ll = vm.latLon
        if (ll != null) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    CompassViewModel.toDms(ll.first, "北纬", "南纬"),
                    fontSize = 15.sp, color = Color(0xFF90A4AE)
                )
                Text(
                    CompassViewModel.toDms(ll.second, "东经", "西经"),
                    fontSize = 15.sp, color = Color(0xFF90A4AE)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "俯仰 %+.0f°   横滚 %+.0f°".format(vm.pitch, vm.roll),
            fontSize = 14.sp, color = Color(0xFF607D8B)
        )
        if (vm.fieldAbnormal) {
            Text(
                "⚠ 磁场异常，请远离磁铁/电器并到“校准”页画 8 字",
                color = Color(0xFFEF5350), fontSize = 13.sp
            )
        }

        Spacer(Modifier.weight(1f))

        // 底部信息栏：气压 / 海拔 / 磁场
        // 海拔优先 GPS（真实卫星高度），无 GPS 时用已校准的气压反演
        val alt = vm.displayAltitude
        val altitudeText = alt?.let { "%.1f".format(it) } ?: "--"
        Surface(
            color = Color(0xFF141A22),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(vertical = 14.dp)) {
                InfoItem(
                    value = if (vm.pressure > 0f) "%,.1f".format(vm.pressure) else "--",
                    label = "气压/百帕", weight = 1f
                )
                InfoItem(
                    value = altitudeText,
                    label = if (vm.displayAltitude != null) "海拔/米 ${vm.altitudeSource}" else "海拔/米",
                    weight = 1f,
                    valueColor = if (vm.showUncalibratedWarn) Color(0xFFFFB74D) else Color(0xFFECEFF1),
                    onClick = { showAltDialog = true }
                )
                InfoItem(
                    value = "${vm.fieldStrength.toInt()} μT",
                    label = "磁场", weight = 1f,
                    valueColor = if (vm.fieldAbnormal) Color(0xFFEF5350) else Color(0xFFECEFF1)
                )
            }
        }
    }
}

@Composable
private fun RowScope.InfoItem(
    value: String, label: String, weight: Float,
    valueColor: Color = Color(0xFFECEFF1),
    onClick: (() -> Unit)? = null
) {
    Column(
        Modifier
            .weight(weight)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFF607D8B))
    }
}

/** 海拔手动校准对话框：输入当前已知真实海拔，反推并锁定气压基准 */
@Composable
private fun AltitudeCalibrateDialog(
    current: Float?,
    hasGps: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(current?.let { "%.0f".format(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("海拔校准") },
        text = {
            Column {
                Text(
                    if (hasGps)
                        "当前使用 GPS 海拔（真实卫星高度），一般无需校准。\n如仍与实际不符，可在此输入已知海拔手动修正。"
                    else
                        "当前使用气压推算海拔，会受当天天气影响。\n请输入你所在位置的已知海拔（如查询地图或参照地标），用于校准气压基准。",
                    fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF90A4AE)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("真实海拔（米）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.toFloatOrNull()?.let(onConfirm) },
                enabled = text.toFloatOrNull() != null
            ) { Text("校准") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
