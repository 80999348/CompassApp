package com.example.compass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compass.sensor.CompassViewModel
import kotlin.math.sin

@Composable
fun CalibrateScreen(vm: CompassViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("传感器校准", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFECEFF1))
        Spacer(Modifier.height(12.dp))
        Text(
            "手持手机，在空中按下方轨迹缓慢画 \"8\" 字，" +
                "重复 10~20 秒。期间远离磁铁、音箱、电脑等磁性物体。",
            fontSize = 15.sp, color = Color(0xFF90A4AE), lineHeight = 22.sp
        )

        Spacer(Modifier.height(24.dp))
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val w = size.width
            val h = size.height
            val path = Path()
            // 8 字轨迹
            for (t in 0..720 step 4) {
                val rad = Math.toRadians(t.toDouble())
                val x = (w / 2) + (w / 3) * sin(rad).toFloat()
                val y = (h / 2) - (h / 3) * sin(rad * 2).toFloat()
                if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF4FC3F7), style = Stroke(4.dp.toPx()))
        }

        Spacer(Modifier.height(24.dp))

        // 磁场强度仪表
        val strength = vm.fieldStrength
        val ok = strength in 25f..65f
        LinearProgressIndicator(
            progress = { (strength / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = if (ok) Color(0xFF66BB6A) else Color(0xFFFFB74D),
            trackColor = Color(0xFF1C2530)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "当前磁场强度：${strength.toInt()} μT   " +
                (if (ok) "（正常范围 25~65 μT，校准效果良好）" else "（异常，请继续画 8 字或更换位置）"),
            fontSize = 14.sp,
            color = if (ok) Color(0xFF66BB6A) else Color(0xFFFFB74D)
        )

        Spacer(Modifier.height(16.dp))
        val status = when {
            vm.fieldAbnormal -> "传感器：不可靠，请校准"
            else -> "传感器：状态良好"
        }
        Text(status, fontSize = 16.sp, color = Color(0xFFECEFF1))

        Spacer(Modifier.height(32.dp))

        // 联网校准海拔
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141A22))) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("联网校准海拔", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFECEFF1))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            !vm.onlineCalibEnabled -> "已关闭，使用标准大气基准"
                            vm.onlineCalibState == "ok" ->
                                "已获取当地海平面气压 ${vm.qnh.toInt()} hPa，海拔更精准"
                            vm.onlineCalibState == "loading" -> "正在获取当地海平面气压…"
                            vm.onlineCalibState == "failed" -> "联网失败（需定位权限与网络），将使用标准基准"
                            else -> "开启后根据定位自动获取当地海平面气压"
                        },
                        fontSize = 12.sp, color = Color(0xFF607D8B), lineHeight = 17.sp
                    )
                }
                Switch(
                    checked = vm.onlineCalibEnabled,
                    onCheckedChange = { vm.toggleOnlineCalib(it) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "说明：海拔由气压换算时依赖\"海平面气压\"，该值随季节、天气变化。" +
                "开启联网校准后会根据当前位置从天气服务获取实时海平面气压，大幅提升精度。" +
                "若显示 ⚠，可点击海拔数字手动校准。",
            fontSize = 13.sp, color = Color(0xFF607D8B), lineHeight = 20.sp
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "提示：若读数始终漂移，可尝试重启手机或更换环境。" +
                "金属表面（汽车、电梯内）会严重干扰地磁场。",
            fontSize = 13.sp, color = Color(0xFF607D8B), lineHeight = 20.sp
        )
    }
}
