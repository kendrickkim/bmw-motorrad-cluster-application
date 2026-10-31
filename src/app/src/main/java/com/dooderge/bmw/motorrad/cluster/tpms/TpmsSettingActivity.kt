package com.dooderge.bmw.motorrad.cluster.tpms

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import com.dooderge.bmw.motorrad.cluster.ui.component.applyClusterImmersiveMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterFixedFontScale
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterSideActionButton
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterTwoPaneScaffold
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterActionButtonContainerColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterActionButtonContentColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSettingsColorScheme
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSideButtonContainerColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.clusterSwitchColors

/**
 * LYTPMS 설정: Advertise 스캔으로 센서 발견 → 6hex ID 바인딩.
 * GATT Connect / Write 없음 (공식 LYTPMS 앱과 동일).
 */
class TpmsSettingActivity : ComponentActivity() {
    private var tpmsService: TpmsService? = null
    private var bound = false
    private var onServiceReady: (() -> Unit)? = null
    private var qrScanTarget: TirePosition? = null
    private var onQrResult: ((String) -> Unit)? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            tpmsService = (service as TpmsService.LocalBinder).getService()
            bound = true
            onServiceReady?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            tpmsService = null
            bound = false
        }
    }

    private val qrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val raw = result.data?.getStringExtra(TpmsQrScanActivity.EXTRA_RESULT)
        if (result.resultCode == RESULT_OK && !raw.isNullOrBlank()) {
            onQrResult?.invoke(raw)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TpmsService.start(this)
        bindService(Intent(this, TpmsService::class.java), connection, Context.BIND_AUTO_CREATE)

        setContent {
            var state by remember {
                mutableStateOf(
                    TpmsDeviceState(
                        frontDevice = WheelDeviceInfo(
                            name = SettingPreference.getTpmsFrontName(this).orEmpty(),
                            address = SettingPreference.getTpmsFrontAddress(this).orEmpty(),
                            sensorId = if (SettingPreference.hasTpmsFrontSensor(this)) {
                                SettingPreference.getTpmsFrontSensorId(this)
                            } else {
                                Int.MIN_VALUE
                            }
                        ),
                        rearDevice = WheelDeviceInfo(
                            name = SettingPreference.getTpmsRearName(this).orEmpty(),
                            address = SettingPreference.getTpmsRearAddress(this).orEmpty(),
                            sensorId = if (SettingPreference.hasTpmsRearSensor(this)) {
                                SettingPreference.getTpmsRearSensorId(this)
                            } else {
                                Int.MIN_VALUE
                            }
                        ),
                        pressureMinKpa = SettingPreference.getTpmsPressureMin(this),
                        pressureMaxKpa = SettingPreference.getTpmsPressureMax(this),
                        temperatureMax = SettingPreference.getTpmsTemperatureMax(this),
                        displayUnit = SettingPreference.getTpmsUnit(this)
                    )
                )
            }
            var enabled by remember {
                mutableStateOf(SettingPreference.isTpmsEnabled(this))
            }
            var manualTarget by remember { mutableStateOf<TirePosition?>(null) }
            var manualHex by remember { mutableStateOf("") }
            var registerHint by remember { mutableStateOf("") }

            fun applyState(next: TpmsDeviceState) {
                // 500ms 폴링이 서비스 latestState로 UI를 덮어씀.
                // 단위·임계값은 이미 prefs에 저장된 편집 선택이므로 prefs 기준으로 유지.
                val ctx = this@TpmsSettingActivity
                state = next.copy(
                    displayUnit = SettingPreference.getTpmsUnit(ctx),
                    pressureMinKpa = SettingPreference.getTpmsPressureMin(ctx),
                    pressureMaxKpa = SettingPreference.getTpmsPressureMax(ctx),
                    temperatureMax = SettingPreference.getTpmsTemperatureMax(ctx),
                )
            }

            DisposableEffect(Unit) {
                onServiceReady = {
                    tpmsService?.latestState?.let { applyState(it) }
                }
                val ticker = android.os.Handler(mainLooper)
                val refresh = object : Runnable {
                    override fun run() {
                        tpmsService?.latestState?.let { applyState(it) }
                        ticker.postDelayed(this, 500)
                    }
                }
                ticker.post(refresh)
                onDispose {
                    ticker.removeCallbacksAndMessages(null)
                    onServiceReady = null
                    sendTpmsAction(TpmsService.ACTION_STOP_SCAN, "both")
                }
            }

            fun applyQr(raw: String, target: TirePosition) {
                when (val payload = TpmsQrParser.parse(raw)) {
                    is TpmsQrPayload.SensorHexId -> {
                        tpmsService?.bindSensorId(target, payload.sensorId, "LYTPMS ${payload.hex}")
                        registerHint = "Bound: ${payload.hex}"
                        Toast.makeText(this, registerHint, Toast.LENGTH_SHORT).show()
                    }
                    is TpmsQrPayload.MultiSensorIds -> {
                        payload.ids.forEach { (slot, id) ->
                            tpmsService?.bindSensorId(slot, id, "LYTPMS")
                        }
                        registerHint = "QR bound ${payload.ids.size} sensors"
                        Toast.makeText(this, registerHint, Toast.LENGTH_SHORT).show()
                    }
                    null -> {
                        registerHint = "QR not recognized (6 hex or HHHHHH&HHHHHH)"
                        Toast.makeText(this, registerHint, Toast.LENGTH_SHORT).show()
                    }
                }
                tpmsService?.latestState?.let { applyState(it) }
            }

            ClusterFixedFontScale {
                MaterialTheme(colorScheme = ClusterSettingsColorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        ClusterTwoPaneScaffold(
                            title = "TPMS Settings",
                            content = {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Enabled", color = Color.White, modifier = Modifier.widthIn(100.dp))
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = {
                                                enabled = it
                                                SettingPreference.setTpmsEnabled(this@TpmsSettingActivity, it)
                                                sendTpmsAction(
                                                    if (it) TpmsService.ACTION_START_POLLING
                                                    else TpmsService.ACTION_STOP_POLLING,
                                                    "both"
                                                )
                                            },
                                            colors = clusterSwitchColors()
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Unit", color = Color.White, modifier = Modifier.widthIn(100.dp))
                                        listOf(
                                            TpmsPressureUnit.BAR,
                                            TpmsPressureUnit.KPA,
                                            TpmsPressureUnit.PSI
                                        ).forEach { u ->
                                            val selected = state.displayUnit == u
                                            Button(
                                                onClick = {
                                                    SettingPreference.setTpmsUnit(this@TpmsSettingActivity, u)
                                                    state = state.copy(displayUnit = u)
                                                    // 폴링/브로드캐스트가 옛 단위로 되돌리지 않도록 서비스도 맞춤
                                                    tpmsService?.updateDisplayUnit(u)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (selected) {
                                                        ClusterActionButtonContainerColor
                                                    } else {
                                                        ClusterSideButtonContainerColor
                                                    }
                                                )
                                            ) { Text(u.label) }
                                        }
                                    }

                                    Text(state.statusMessage, color = Color(0xFFAAAAAA), fontSize = 13.sp)

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        WheelBindRow(
                                            title = "Front",
                                            info = state.frontDevice,
                                            reading = state.front,
                                            unit = state.displayUnit,
                                            pressureMinKpa = state.pressureMinKpa,
                                            pressureMaxKpa = state.pressureMaxKpa,
                                            modifier = Modifier.weight(1f),
                                            onManual = {
                                                manualHex = ""
                                                manualTarget = TirePosition.FRONT
                                            },
                                            onQr = {
                                                qrScanTarget = TirePosition.FRONT
                                                onQrResult = { applyQr(it, TirePosition.FRONT) }
                                                qrLauncher.launch(
                                                    Intent(this@TpmsSettingActivity, TpmsQrScanActivity::class.java)
                                                )
                                            }
                                        )
                                        WheelBindRow(
                                            title = "Rear",
                                            info = state.rearDevice,
                                            reading = state.rear,
                                            unit = state.displayUnit,
                                            pressureMinKpa = state.pressureMinKpa,
                                            pressureMaxKpa = state.pressureMaxKpa,
                                            modifier = Modifier.weight(1f),
                                            onManual = {
                                                manualHex = ""
                                                manualTarget = TirePosition.REAR
                                            },
                                            onQr = {
                                                qrScanTarget = TirePosition.REAR
                                                onQrResult = { applyQr(it, TirePosition.REAR) }
                                                qrLauncher.launch(
                                                    Intent(this@TpmsSettingActivity, TpmsQrScanActivity::class.java)
                                                )
                                            }
                                        )
                                    }

                                    if (registerHint.isNotBlank()) {
                                        Text(registerHint, color = Color(0xFF8FBC8F), fontSize = 12.sp)
                                    }

                                    PressureThresholdRow(
                                        label = "Low ${state.displayUnit.label}",
                                        valueKpa = state.pressureMinKpa,
                                        unit = state.displayUnit
                                    ) { kpa ->
                                        SettingPreference.setTpmsPressureMin(
                                            this@TpmsSettingActivity,
                                            kpa
                                        )
                                        state = state.copy(
                                            pressureMinKpa = SettingPreference.getTpmsPressureMin(
                                                this@TpmsSettingActivity
                                            )
                                        )
                                    }
                                    PressureThresholdRow(
                                        label = "High ${state.displayUnit.label}",
                                        valueKpa = state.pressureMaxKpa,
                                        unit = state.displayUnit
                                    ) { kpa ->
                                        SettingPreference.setTpmsPressureMax(
                                            this@TpmsSettingActivity,
                                            kpa
                                        )
                                        state = state.copy(
                                            pressureMaxKpa = SettingPreference.getTpmsPressureMax(
                                                this@TpmsSettingActivity
                                            )
                                        )
                                    }
                                    ThresholdRow("Temp °C", state.temperatureMax) {
                                        SettingPreference.setTpmsTemperatureMax(this@TpmsSettingActivity, it)
                                        state = state.copy(temperatureMax = it)
                                    }
                                }

                            },
                            sideActions = {
                                ClusterSideActionButton(
                                    label = "Save",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingPreference.saveTpmsSettings(
                                        context = this@TpmsSettingActivity,
                                        enabled = enabled,
                                        frontName = state.frontDevice.name,
                                        frontAddress = state.frontDevice.address,
                                        frontSensorId = state.frontDevice.sensorId,
                                        rearName = state.rearDevice.name,
                                        rearAddress = state.rearDevice.address,
                                        rearSensorId = state.rearDevice.sensorId,
                                        unit = state.displayUnit,
                                        pressureMin = state.pressureMinKpa,
                                        pressureMax = state.pressureMaxKpa,
                                        temperatureMax = state.temperatureMax
                                    )
                                    if (enabled &&
                                        SettingPreference.hasTpmsSensorsConfigured(this@TpmsSettingActivity)
                                    ) {
                                        sendTpmsAction(TpmsService.ACTION_START_POLLING, "both")
                                    } else {
                                        sendTpmsAction(TpmsService.ACTION_STOP_POLLING, "both")
                                    }
                                    setResult(RESULT_OK)
                                    finish()
                                }
                                ClusterSideActionButton(
                                    label = "Fetch reading",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    sendTpmsAction(TpmsService.ACTION_REFRESH, "both")
                                }
                                ClusterSideActionButton(
                                    label = "Back",
                                    modifier = Modifier.weight(1f)
                                ) { finish() }
                            }
                        )

                        val target = manualTarget
                        if (target != null) {
                            AlertDialog(
                                onDismissRequest = { manualTarget = null },
                                title = {
                                    Text(
                                        when (target) {
                                            TirePosition.FRONT -> "Front sensor ID"
                                            TirePosition.REAR -> "Rear sensor ID"
                                        }
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            "Enter 6 hex characters",
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 13.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        TextField(
                                            value = manualHex,
                                            onValueChange = { manualHex = it },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Black,
                                                unfocusedContainerColor = Color.Black,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            placeholder = { Text("e.g. 1A2B3C", color = Color.Gray) }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val p = TpmsQrParser.parse(manualHex)
                                            if (p is TpmsQrPayload.SensorHexId) {
                                                applyQr(manualHex, target)
                                                manualTarget = null
                                            } else {
                                                Toast.makeText(
                                                    this@TpmsSettingActivity,
                                                    "6 hex ID required",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Apply") }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { manualTarget = null },
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        window.applyClusterImmersiveMode()
    }

    private fun sendTpmsAction(action: String, wheel: String) {
        startService(
            Intent(this, TpmsService::class.java).apply {
                this.action = action
                putExtra(TpmsService.EXTRA_WHEEL, wheel)
            }
        )
    }

    @Composable
    private fun WheelBindRow(
        title: String,
        info: WheelDeviceInfo,
        reading: TireReading,
        unit: TpmsPressureUnit,
        pressureMinKpa: Int,
        pressureMaxKpa: Int,
        onManual: () -> Unit,
        onQr: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val alarm = TpmsProtocol.isPressureOutOfRange(
            reading.pressureKpaCents,
            pressureMinKpa,
            pressureMaxKpa
        )
        val readingColor = if (alarm) Color(0xFFFF9800) else Color.White
        Column(
            modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF444444))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                info.labelOrPlaceholder(),
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                reading.pressureWithUnit(unit) + " / " +
                    (if (reading.temperatureC >= 0) "${reading.temperatureC}°C" else "--°C") +
                    " / " +
                    (if (reading.battery >= 0) "${reading.battery}%" else "--%"),
                color = readingColor,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onManual, shape = RoundedCornerShape(10.dp)) { Text("Manual") }
                Button(
                    onClick = onQr,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClusterActionButtonContainerColor,
                        contentColor = ClusterActionButtonContentColor
                    )
                ) { Text("QR") }
            }
        }
    }

    @Composable
    private fun PressureThresholdRow(
        label: String,
        valueKpa: Int,
        unit: TpmsPressureUnit,
        onCommitKpa: (Int) -> Unit
    ) {
        var text by remember(valueKpa, unit) {
            mutableStateOf(TpmsProtocol.formatThresholdValue(valueKpa, unit))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, modifier = Modifier.widthIn(100.dp), fontSize = 14.sp)
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black,
                    unfocusedContainerColor = Color.Black,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            TextButton(
                onClick = {
                    TpmsProtocol.parseThresholdToKpa(text, unit)?.let { onCommitKpa(it) }
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Apply") }
        }
    }

    @Composable
    private fun ThresholdRow(label: String, value: Int, onCommit: (Int) -> Unit) {
        var text by remember(value) { mutableStateOf(value.toString()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, modifier = Modifier.widthIn(100.dp), fontSize = 14.sp)
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black,
                    unfocusedContainerColor = Color.Black,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            TextButton(
                onClick = {
                    text.toIntOrNull()?.let { onCommit(it) }
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Apply") }
        }
    }

}
