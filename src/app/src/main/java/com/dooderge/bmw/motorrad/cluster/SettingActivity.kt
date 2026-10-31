package com.dooderge.bmw.motorrad.cluster

import com.dooderge.bmw.motorrad.cluster.ui.component.applyClusterImmersiveMode

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsSettingActivity
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterFixedFontScale
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterSideActionButton
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterTwoPaneScaffold
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSettingsColorScheme
import com.dooderge.bmw.motorrad.cluster.ui.theme.clusterSwitchColors

class SettingActivity : ComponentActivity() {
    private val showSpeedGearOverlay = mutableStateOf(false)

    @SuppressLint("MissingPermission")

    fun getDeviceName(BleDevice: BluetoothDevice): String {
        return BleDevice.name ?: ("Unnamed-" + " ${BleDevice.address}")
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
            var largeOverlayEnabled by remember {
                mutableStateOf(SettingPreference.isLargeOverlay(this))
            }
            var gpsLogging by remember {
                mutableStateOf(SettingPreference.isGpsLogging(this))
            }

            // Speed Factor UI 비활성 — 필요 시 복원
            // var strSpeedFactor by remember {
            //     mutableStateOf(
            //         SettingPreference.getWheelRpmToSpeedFactor(
            //             this
            //         ).toString()
            //     )
            // }

            LaunchedEffect(Unit) {
                // 초기 선택된 장치 이름 설정
                if (savedDeviceName != null && savedDeviceName.isNotEmpty()) {
                    selectedDeviceName = savedDeviceName
                    selectedDeviceAddress = SettingPreference.getAddress(this@SettingActivity) ?: ""
                } else {
                    selectedDeviceName = "No device selected"
                }
            }

            ClusterFixedFontScale {
                MaterialTheme(colorScheme = ClusterSettingsColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ClusterTwoPaneScaffold(
                        title = "Settings",
                        content = {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
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
                                            text = selectedDeviceName.ifEmpty { "No device selected" },
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
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = "Select",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                // Speed Factor 항목 제거 (주석 유지)
                                // Row(verticalAlignment = Alignment.CenterVertically) {
                                //     SettingLabel("Speed Factor")
                                //     TextField(
                                //         value = strSpeedFactor,
                                //         onValueChange = { strSpeedFactor = it },
                                //         singleLine = true,
                                //         modifier = Modifier.weight(1f),
                                //         colors = TextFieldDefaults.colors(
                                //             focusedContainerColor = Color.Black,
                                //             unfocusedContainerColor = Color.Black,
                                //             focusedIndicatorColor = Color(0xFF4376A1),
                                //             unfocusedIndicatorColor = Color(0xFF666666),
                                //             cursorColor = Color(0xFF4376A1),
                                //             focusedTextColor = Color.White,
                                //             unfocusedTextColor = Color.White
                                //         ),
                                //         textStyle = TextStyle(
                                //             fontSize = 20.sp,
                                //             color = Color.White,
                                //             fontWeight = FontWeight.Medium
                                //         )
                                //     )
                                // }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    SettingSwitchRow(
                                        label = "LIN Test",
                                        checked = linTestEnabled,
                                        onCheckedChange = {
                                            linTestEnabled = it
                                            SettingPreference.setLinTestEnabled(
                                                this@SettingActivity,
                                                it
                                            )
                                        }
                                    )
                                    SettingSwitchRow(
                                        label = "Speed / Gear Overlay",
                                        checked = showSpeedGearOverlay.value,
                                        onCheckedChange = { checked ->
                                            showSpeedGearOverlay.value = checked
                                            BleService.setOverlayPreview(checked)
                                        }
                                    )
                                    SettingSwitchRow(
                                        label = "Use Large Overlay",
                                        checked = largeOverlayEnabled,
                                        onCheckedChange = { checked ->
                                            largeOverlayEnabled = checked
                                            SettingPreference.setLargeOverlay(
                                                this@SettingActivity,
                                                checked
                                            )
                                            BleService.setLargeOverlay(checked)
                                        }
                                    )
                                    SettingSwitchRow(
                                        label = "GPS Logging",
                                        checked = gpsLogging,
                                        onCheckedChange = { checked ->
                                            gpsLogging = checked
                                            SettingPreference.setGpsLogging(
                                                this@SettingActivity,
                                                checked
                                            )
                                            BleService.setGpsLogging(checked)
                                        }
                                    )
                                    SettingSwitchRow(
                                        label = "LIN Logging",
                                        checked = linLogging,
                                        onCheckedChange = { checked ->
                                            linLogging = checked
                                            SettingPreference.setLinLogging(
                                                this@SettingActivity,
                                                checked
                                            )
                                            BleService.setLinLogging(checked)
                                        }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingLabel("TPMS")
                                    Button(
                                        onClick = {
                                            startActivity(
                                                Intent(
                                                    this@SettingActivity,
                                                    TpmsSettingActivity::class.java
                                                )
                                            )
                                        },
                                        modifier = Modifier.height(56.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = "Settings",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingLabel("View Log")
                                    Button(
                                        onClick = {
                                            startActivity(
                                                LogListActivity.intent(
                                                    this@SettingActivity,
                                                    LogKind.LIN
                                                )
                                            )
                                        },
                                        modifier = Modifier.height(56.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = "LIN",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            startActivity(
                                                LogListActivity.intent(
                                                    this@SettingActivity,
                                                    LogKind.GPS
                                                )
                                            )
                                        },
                                        modifier = Modifier.height(56.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text(
                                            text = "GPS",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        sideActions = {
                            ClusterSideActionButton(
                                label = "Save",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    SettingPreference.saveSettings(
                                        context = this@SettingActivity,
                                        deviceName = selectedDeviceName,
                                        address = selectedDeviceAddress
                                    )
                                    finish()
                                }
                            )
                            ClusterSideActionButton(
                                label = "Cancel",
                                modifier = Modifier.weight(1f),
                                onClick = { finish() }
                            )
                            ClusterSideActionButton(
                                label = "Reset",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedDeviceName = ""
                                    selectedDeviceAddress = ""
                                }
                            )
                        }
                    )
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = { Text("Select Bluetooth device") },
                            text = {
                                Column(
                                    Modifier
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text("Paired BMW-WWC devices")
                                    if (pairedDevices.isEmpty()) {
                                        Text("No paired Bluetooth devices.")
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
                                                        .padding(vertical = 2.dp),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(getDeviceName(device))
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { showDialog = false },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }

    /** 설정 행 라벨(고정 폭). */
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

    /** 라벨 + Switch 한 줄(토글 블록 전용 좁은 행 높이). */
    @Composable
    private fun SettingSwitchRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = clusterSwitchColors()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        showSpeedGearOverlay.value = false
        BleService.setOverlayPreview(false)
    }

    override fun onResume() {
        super.onResume()
        window.applyClusterImmersiveMode()
    }
} 
