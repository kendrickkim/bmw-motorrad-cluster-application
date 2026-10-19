package com.dooderge.bmw.motorrad.cluster

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SettingsColorScheme = darkColorScheme(
    primary = Color(0xFF4376A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4376A1),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF4376A1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C4F6B),
    tertiary = Color(0xFF4376A1),
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF4376A1)
)

class SettingActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")

    fun getDeviceName(BleDevice: BluetoothDevice): String {
        return BleDevice.name ?: ("이름없음-" + " ${BleDevice.address}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 저장된 장치 이름 불러오기
        val savedDeviceName = SettingPreference.getDeviceName(this)

        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var selectedDeviceName by remember { mutableStateOf("") }
            var selectedDeviceAddress by remember { mutableStateOf("") }
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = remember {
                bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            }
            var linTestEnabled by remember {
                mutableStateOf(SettingPreference.isLinTestEnabled(this))
            }
            var linLogging by remember {
                mutableStateOf(SettingPreference.isLinLogging(this))
            }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        linLogging = intent?.getBooleanExtra(
                            BleService.EXTRA_LIN_LOGGING,
                            false
                        ) == true
                    }
                }
                val filter = IntentFilter(BleService.ACTION_LIN_LOG_STATE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    registerReceiver(receiver, filter)
                }
                onDispose { unregisterReceiver(receiver) }
            }
            var strSpeedFactor by remember {
                mutableStateOf(
                    SettingPreference.getWheelRpmToSpeedFactor(
                        this
                    ).toString()
                )
            }


            LaunchedEffect(Unit) {
                // 초기 선택된 장치 이름 설정
                if (savedDeviceName != null && savedDeviceName.isNotEmpty()) {
                    selectedDeviceName = savedDeviceName
                    selectedDeviceAddress = SettingPreference.getAddress(this@SettingActivity) ?: ""
                } else {
                    selectedDeviceName = "선택된 장치 없음"
                }
            }

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = 1f
                )
            ) {
                MaterialTheme(colorScheme = SettingsColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingLabel("Device")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .border(2.dp, Color.Gray)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = selectedDeviceName.ifEmpty { "선택된 장치 없음" },
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { showDialog = true },
                                        modifier = Modifier.height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = "선택",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingLabel("Speed Factor")
                                    TextField(
                                        value = strSpeedFactor,
                                        onValueChange = { strSpeedFactor = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black,
                                            unfocusedContainerColor = Color.Black,
                                            focusedIndicatorColor = Color(0xFF4376A1),
                                            unfocusedIndicatorColor = Color(0xFF666666),
                                            cursorColor = Color(0xFF4376A1),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        textStyle = TextStyle(
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingLabel("LIN Test")
                                    Switch(
                                        checked = linTestEnabled,
                                        onCheckedChange = {
                                            linTestEnabled = it
                                            SettingPreference.setLinTestEnabled(
                                                this@SettingActivity,
                                                it
                                            )
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF4376A1),
                                            checkedBorderColor = Color(0xFF4376A1),
                                            uncheckedThumbColor = Color(0xFFAAAAAA),
                                            uncheckedTrackColor = Color(0xFF333333),
                                            uncheckedBorderColor = Color(0xFF666666)
                                        )
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingLabel("Data Log")
                                    Button(
                                        onClick = { requestLinLog(!linLogging) },
                                        modifier = Modifier.height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = if (linLogging) "로깅 중지" else "로깅 시작",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(0.22f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingActionButton(
                                    label = "Save",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        SettingPreference.saveSettings(
                                            context = this@SettingActivity,
                                            deviceName = selectedDeviceName,
                                            address = selectedDeviceAddress
                                        )
                                        SettingPreference.setWheelRpmToSpeedFactor(
                                            context = this@SettingActivity,
                                            factor = strSpeedFactor.toFloatOrNull() ?: 1.0f
                                        )
                                        BleService.WHEEL_RPM_TO_SPEED =
                                            SettingPreference.getWheelRpmToSpeedFactor(this@SettingActivity)
                                        finish()
                                    }
                                )
                                SettingActionButton(
                                    label = "Cancel",
                                    modifier = Modifier.weight(1f),
                                    onClick = { finish() }
                                )
                                SettingActionButton(
                                    label = "Reset",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedDeviceName = ""
                                        selectedDeviceAddress = ""
                                        strSpeedFactor =
                                            SettingPreference.getWheelRpmToSpeedFactor(this@SettingActivity)
                                                .toString()
                                    }
                                )
                            }
                        }
                    }
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("블루투스 장치 선택") },
                            text = {
                                Column(
                                    Modifier
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text("페어링된 BMW-WWC 장치")
                                    if (pairedDevices.isEmpty()) {
                                        Text("페어링된 블루투스 장치가 없습니다.")
                                    } else {
                                        pairedDevices.forEach { device ->
                                            if (device.name != null && !device.name.isEmpty() && device.name.startsWith(
                                                    "BMW-WWC"
                                                )
                                            ) {
                                                // 장치 이름이 "BMW-WWC"로 시작하는 경우에만 표시
                                                Button(
                                                    onClick = {
                                                        showDialog = false

                                                        selectedDeviceName = getDeviceName(device)
                                                        selectedDeviceAddress = device.address
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Text(getDeviceName(device))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("닫기")
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }

    private fun requestLinLog(start: Boolean) {
        val intent = Intent(this, BleService::class.java).apply {
            action = if (start) BleService.ACTION_START_LIN_LOG else BleService.ACTION_STOP_LIN_LOG
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    @Composable
    private fun SettingLabel(text: String) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.width(140.dp)
        )
    }

    @Composable
    private fun SettingActionButton(
        label: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff333333),
                contentColor = Color.White
            )
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
} 
