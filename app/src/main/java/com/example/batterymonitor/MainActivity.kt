package com.example.batterymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class MainActivity : ComponentActivity() {

    private val updateInterval = 1000L // 毫秒

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BatteryMonitorUI()
        }
    }

    @Composable
    fun BatteryMonitorUI() {
        var voltage by remember { mutableStateOf(0.0) }
        var current by remember { mutableStateOf(0.0) }
        var power by remember { mutableStateOf(0.0) }
        var status by remember { mutableStateOf("未知") }

        var dualBattery by remember { mutableStateOf(true) }      // 双电芯开关
        var showRaw by remember { mutableStateOf(false) }         // 显示原始信息开关

        LaunchedEffect(Unit) {
            while (true) {
                val rawV = readFile("/sys/class/power_supply/battery/voltage_now")
                val rawC = readFile("/sys/class/power_supply/battery/current_now")

                val v = rawV / 1_000_000 * if (dualBattery) 2 else 1
                val c = -rawC / 1_000_000
                val p = v * c

                voltage = if (showRaw) rawV.toDouble() else v
                current = if (showRaw) -rawC.toDouble() else c
                power = if (showRaw) (rawV * -rawC).toDouble() else p

                status = if (c < 0 || c > 5) "电流异常" else "校准正常"

                kotlinx.coroutines.delay(updateInterval)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("电压: %.3f ${if (showRaw) "uV" else "V"}".format(voltage), fontSize = 24.sp)
            Text("电流: %.3f ${if (showRaw) "uA" else "A"}".format(current), fontSize = 24.sp)
            Text("功率: %.3f ${if (showRaw) "uW" else "W"}".format(power), fontSize = 24.sp)
            Text("状态: $status", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("双电芯模式", fontSize = 16.sp)
                Switch(checked = dualBattery, onCheckedChange = { dualBattery = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示原始信息", fontSize = 16.sp)
                Switch(checked = showRaw, onCheckedChange = { showRaw = it })
            }
        }
    }

    private fun readFile(path: String): Long {
        return try {
            File(path).readText().trim().toLong()
        } catch (e: Exception) {
            0L
        }
    }
}
