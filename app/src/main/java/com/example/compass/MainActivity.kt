package com.example.compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compass.sensor.CompassViewModel
import com.example.compass.ui.CalibrateScreen
import com.example.compass.ui.CompassScreen
import com.example.compass.ui.LevelScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = CompassViewModel(this)
        // 定位权限：经纬度显示 + 真北修正
        val permLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            if (granted.values.any { it }) vm.startLocationUpdates()
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0F14),
                    surface = Color(0xFF141A22),
                    primary = Color(0xFF4FC3F7),
                    secondary = Color(0xFFFFB74D)
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainTabs(vm)
                    LaunchedEffect(Unit) {
                        val needed = listOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ).filter {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                this@MainActivity, it
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        if (needed.isEmpty()) vm.startLocationUpdates()
                        else permLauncher.launch(needed.toTypedArray())
                    }
                }
            }
        }
    }
}

private enum class Tab(val label: String) { COMPASS("指南针"), LEVEL("水平仪"), CALIBRATE("校准") }

@Composable
private fun MainTabs(vm: CompassViewModel) {
    DisposableEffect(Unit) {
        vm.startSensors()
        onDispose {
            vm.stopSensors()
            vm.stopLocationUpdates()
        }
    }
    var tab by remember { mutableStateOf(Tab.COMPASS) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.COMPASS -> CompassScreen(vm)
                Tab.LEVEL -> LevelScreen(vm)
                Tab.CALIBRATE -> CalibrateScreen(vm)
            }
        }
        NavigationBar(containerColor = Color(0xFF141A22)) {
            Tab.entries.forEach { t ->
                NavigationBarItem(
                    selected = tab == t,
                    onClick = { tab = t },
                    label = { Text(t.label) },
                    icon = {}
                )
            }
        }
        // 版权信息
        Text(
            text = "小虫工作室 @2026",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141A22))
                .padding(bottom = 8.dp, top = 2.dp),
            color = Color(0xFF4A5A68),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
