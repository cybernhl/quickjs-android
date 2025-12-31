package com.shiqi.testquickjs

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shiqi.quickjs.JSNumber
import com.shiqi.testquickjs.ui.theme.QuickJSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private lateinit var jsEngine: QuickJsEngine
    private lateinit var hrmGattValue: HrmGattValue
    private lateinit var batteryGattValue: BatteryGattValue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jsEngine = QuickJsEngine(this).apply {
            init()
        }
        loadCoreScripts()
        hrmGattValue = HrmGattValue(jsEngine.getJsContext())
        batteryGattValue = BatteryGattValue(jsEngine.getJsContext())

        setContent {
            QuickJSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        jsEngine = jsEngine,
                        onLoadScript = { fileName -> loadAndExecuteScript(fileName) },
                        hrmGattValue = hrmGattValue,
                        batteryGattValue = batteryGattValue
                    )
                }
            }
        }
    }
    private fun loadAndExecuteScript(fileName: String) {
        try {
            val scriptContent = assets.open(fileName)
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
            val context = jsEngine.getJsContext()
            context.evaluate(scriptContent)

            while (context.executePendingJob()) { /* no-op */ }
            Log.i("MainActivity", "Successfully loaded and executed: $fileName")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load and execute script: $fileName", e)
        }
    }
    private fun loadCoreScripts() {
        loadAndExecuteScript("clock.js")
        loadAndExecuteScript("gatt-core.js")
    }

    override fun onDestroy() {
        super.onDestroy()
        jsEngine.destroy()
    }
}

@Composable
fun MainScreen(
    jsEngine: QuickJsEngine,
    onLoadScript: (String) -> Unit,
    hrmGattValue: HrmGattValue,
    batteryGattValue: BatteryGattValue
) {
    // --- 直接從 JS 讀取的 UI 狀態 ---
    var hrmValueDirect by remember { mutableStateOf("Direct HRM: --") }
    var batteryValueDirect by remember { mutableStateOf("Direct Battery: --") }

    // --- 透過 GattValue 類別讀取的 UI 狀態 ---
    var hrmValueFromClass by remember { mutableStateOf("Class HRM: --") }
    var batteryValueFromClass by remember { mutableStateOf("Class Battery: --") }

    var isHrmLoaded by remember { mutableStateOf(false) }
    var isBatteryLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // JS 狀態更新排程器 (保持不變)
    LaunchedEffect(Unit) {
        val jsContext = jsEngine.getJsContext()
        while (isActive) {
            withContext(Dispatchers.Default) {
                val now = System.currentTimeMillis()
                jsContext.evaluate("Clock.tick($now);")
                jsContext.evaluate("Object.keys(__gattRegistry).forEach(uuid => next(uuid));")
            }
            delay(100)
        }
    }

    // UI 更新迴圈
    LaunchedEffect(Unit) {
        val jsContext = jsEngine.getJsContext()
        // 在 JS 全域定義一個臨時變數，用 var 確保是全域
        jsContext.evaluate("var __temp_result;")

        while (isActive) {
            withContext(Dispatchers.IO) {
                if (isHrmLoaded) {
                    // --- 方式一：直接讀取 ---
                    jsContext.evaluate("__temp_result = getValue('2A37');")
                    val hrmResult = jsContext.globalObject.getProperty("__temp_result")
                    if (hrmResult is JSNumber) {
                        val bpm = hrmResult.int
                        withContext(Dispatchers.Main) { hrmValueDirect = "Direct HRM: $bpm BPM" }
                    }

                    // --- 方式二：透過 HrmGattValue 類別 ---
                    // 它的 .gattValue 會觸發內部的 get() 來獲取數據
                    val bpmFromClass = hrmGattValue.gattValue[1].toInt() and 0xFF // byteArray to Unsigned Int
                    withContext(Dispatchers.Main) { hrmValueFromClass = "Class HRM: $bpmFromClass BPM" }
                }

                if (isBatteryLoaded) {
                    // --- 方式一：直接讀取 ---
                    jsContext.evaluate("__temp_result = getValue('2A19');")
                    val batteryResult = jsContext.globalObject.getProperty("__temp_result")
                    if (batteryResult is JSNumber) {
                        val level = batteryResult.int
                        withContext(Dispatchers.Main) { batteryValueDirect = "Direct Battery: $level%" }
                    }

                    // --- 方式二：透過 BatteryGattValue 類別 ---
                    val levelFromClass = batteryGattValue.gattValue[0].toInt() and 0xFF
                    withContext(Dispatchers.Main) { batteryValueFromClass = "Class Battery: $levelFromClass%" }
                }
            }
            delay(1000)
        }
    }

    // Column 和 Button 部分保持不變
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "QuickJS Dynamic GATT", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // 顯示區域
        Text(text = "Direct from JS:", color = Color.Gray)
        Text(text = hrmValueDirect, fontSize = 20.sp)
        Text(text = batteryValueDirect, fontSize = 20.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Via GattValue Class:", color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = hrmValueFromClass, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = batteryValueFromClass, fontSize = 20.sp, fontWeight = FontWeight.Bold)


        Spacer(modifier = Modifier.height(32.dp))
        Text("Manual Load", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    if (!isHrmLoaded) {
                        scope.launch(Dispatchers.IO) {
                            onLoadScript("hrm.js")
                            withContext(Dispatchers.Main) { isHrmLoaded = true }
                        }
                    }
                },
                enabled = !isHrmLoaded
            ) {
                Text("Load 2A37 (HRM)")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    if (!isBatteryLoaded) {
                        scope.launch(Dispatchers.IO) {
                            onLoadScript("battery.js")
                            withContext(Dispatchers.Main) { isBatteryLoaded = true }
                        }
                    }
                },
                enabled = !isBatteryLoaded
            ) {
                Text("Load 2A19 (Battery)")
            }
        }
    }
}
