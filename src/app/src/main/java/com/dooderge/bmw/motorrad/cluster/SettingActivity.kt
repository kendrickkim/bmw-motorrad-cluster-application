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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

    /** 설정 본문(Device/버튼) 공통 높이·글자 크기 */
    private val contentControlHeight = 40.dp
    private val contentControlFontSize = 15.sp
    private val contentButtonFontSize = 15.sp
    private val contentLabelFontSize = 13.sp
    private val contentLabelWidth = 108.dp
    /** 토글 제목은 길 수 있어 더 넓게 */
    private val contentToggleLabelWidth = 168.dp

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

            var strSpeedFactor by remember {
                mutableStateOf(
                    formatSpeedFactor(SettingPreference.getWheelRpmToSpeedFactor(this))
                )
            }

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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Device: full-width single row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SettingLabel("Device")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(contentControlHeight)
                                            .border(2.dp, Color.Gray)
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = selectedDeviceName.ifEmpty { "No device selected" },
                                            fontSize = contentControlFontSize,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SettingContentButton(
                                        label = "Select",
                                        onClick = { showDialog = true }
                                    )
                                }
                                SettingPairRow(
                                    left = {
                                        SettingLabel("Speed Factor")
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(contentControlHeight)
                                                .border(2.dp, Color.Gray)
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            BasicTextField(
                                                value = strSpeedFactor,
                                                onValueChange = { strSpeedFactor = it },
                                                singleLine = true,
                                                cursorBrush = SolidColor(Color(0xFF4376A1)),
                                                textStyle = TextStyle(
                                                    fontSize = contentControlFontSize,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    right = {
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
                                    }
                                )
                                SettingPairRow(
                                    left = {
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
                                    },
                                    right = {
                                        SettingSwitchRow(
                                            label = "Speed / Gear Overlay",
                                            checked = showSpeedGearOverlay.value,
                                            onCheckedChange = { checked ->
                                                showSpeedGearOverlay.value = checked
                                                BleService.setOverlayPreview(checked)
                                            }
                                        )
                                    }
                                )
                                SettingPairRow(
                                    left = {
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
                                    },
                                    right = {
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
                                    }
                                )
                                SettingPairRow(
                                    left = {
                                        SettingLabel("TPMS")
                                        SettingContentButton(
                                            label = "Settings",
                                            onClick = {
                                                startActivity(
                                                    Intent(
                                                        this@SettingActivity,
                                                        TpmsSettingActivity::class.java
                                                    )
                                                )
                                            }
                                        )
                                    },
                                    right = {
                                        SettingLabel("View Log")
                                        SettingContentButton(
                                            label = "LIN",
                                            onClick = {
                                                startActivity(
                                                    LogListActivity.intent(
                                                        this@SettingActivity,
                                                        LogKind.LIN
                                                    )
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SettingContentButton(
                                            label = "GPS",
                                            onClick = {
                                                startActivity(
                                                    LogListActivity.intent(
                                                        this@SettingActivity,
                                                        LogKind.GPS
                                                    )
                                                )
                                            }
                                        )
                                    }
                                )
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
                                    strSpeedFactor.toFloatOrNull()?.takeIf { it > 0f }?.let { factor ->
                                        SettingPreference.setWheelRpmToSpeedFactor(
                                            this@SettingActivity,
                                            factor
                                        )
                                        BleService.applySpeedFactor(factor)
                                    }
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

    /** Speed Factor 표시용 문자열(불필요한 꼬리 0 제거). */
    private fun formatSpeedFactor(factor: Float): String {
        val text = String.format(java.util.Locale.US, "%.4f", factor)
        return text.trimEnd('0').trimEnd('.')
    }

    /** 한 행에 설정 2칸(좌/우). */
    @Composable
    private fun SettingPairRow(
        left: @Composable RowScope.() -> Unit,
        right: @Composable RowScope.() -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                content = left
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                content = right
            )
        }
    }

    /** 설정 행 라벨(2열 배치용 작은 글자). */
    @Composable
    private fun SettingLabel(
        text: String,
        width: Dp = contentLabelWidth,
    ) {
        Text(
            text = text,
            fontSize = contentLabelFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.width(width)
        )
    }

    /** 설정 본문용 낮은 높이 버튼(Device/TPMS/View Log). */
    @Composable
    private fun SettingContentButton(
        label: String,
        onClick: () -> Unit,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.height(contentControlHeight),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = label,
                fontSize = contentButtonFontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }

    /** 라벨 + Switch 한 칸(페어 행의 좌/우). */
    @Composable
    private fun RowScope.SettingSwitchRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        SettingLabel(label, width = contentToggleLabelWidth)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = clusterSwitchColors(),
            modifier = Modifier.height(contentControlHeight)
        )
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
