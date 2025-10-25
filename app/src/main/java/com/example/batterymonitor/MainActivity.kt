package com.example.batterymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryMonitorApp()
        }
    }

    @Composable
    fun BatteryMonitorApp() {
        var voltage by remember { mutableStateOf(0L) }
        var current by remember { mutableStateOf(0L) }
        var isDualBattery by remember { mutableStateOf(true) }
        var showRawInfo by remember { mutableStateOf(false) }

        // 循环刷新数据
        LaunchedEffect(Unit) {
            while (true) {
                voltage = readSysFile("/sys/class/power_supply/battery/voltage_now") ?: 0L
                current = readSysFile("/sys/class/power_supply/battery/current_now") ?: 0L
                delay(1000)
            }
        }

        val vTotal = if (isDualBattery) voltage * 2 else voltage
        val iTotal = -current
        val power = vTotal * iTotal / 1_000_000_000.0

        Column(modifier = Modifier.padding(16.dp)) {
            Text("电压: %.3f V".format(vTotal / 1_000_000.0))
            Text("电流: %.3f A".format(iTotal / 1_000_000.0))
            Text("功率: %.3f W".format(power))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("双电芯")
                Switch(checked = isDualBattery, onCheckedChange = { isDualBattery = it })
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("显示原始信息")
                Switch(checked = showRawInfo, onCheckedChange = { showRawInfo = it })
            }

            if (showRawInfo) {
                Text("原始电压: $voltage µV")
                Text("原始电流: $current µA")
            }
        }
    }

    // 读取系统文件，使用 Root 权限
    suspend fun readSysFile(path: String): Long? = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val value = reader.readLine()?.trim()?.toLongOrNull()
            process.waitFor()
            value
        } catch (e: Exception) { null }
    }
}
