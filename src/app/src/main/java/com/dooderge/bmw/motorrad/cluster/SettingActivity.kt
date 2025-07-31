package com.dooderge.bmw.motorrad.cluster

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
//            val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
//            val receiver = rememberUpdatedState(newValue = object :
//                BroadcastReceiver() {
//                override fun onReceive(context: Context?, intent: Intent?) {
//                    val action: String? = intent?.action
//                    if (BluetoothDevice.ACTION_FOUND == action) {
//                        val device: BluetoothDevice? =
//                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                        device?.let {
//                            var containe_same_device = false;
//                            discoveredDevices.forEach {
//                                if (it.address == device.address) {
//                                    containe_same_device = true
//                                    return@forEach
//                                }
//                            }
//                            if (!containe_same_device) {
//                                discoveredDevices.add(device)
//                            }
//                        }
//                    }
//                }
//            })


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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Wonder wheel 장치 설정",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp)

                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .padding(8.dp)
                                    .border(2.dp, Color.Gray),

                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedDeviceName.isNotEmpty()) selectedDeviceName else "선택된 장치 없음",
                                    fontSize = 28.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    // 블루투스 장치 검색 결과 수신

                                    showDialog = true

//                                    var intent_bt_action_found =
//                                        IntentFilter(BluetoothDevice.ACTION_FOUND)
//
//                                    registerReceiver(receiver.value, intent_bt_action_found)
//
//                                    // 블루투스 장치 검색 시작
//                                    bluetoothAdapter?.startDiscovery()

                                },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Text(
                                    text = "선택",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    selectedDeviceName = ""
                                    selectedDeviceAddress = ""
                                },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                border = BorderStroke(2.dp, Color.Gray)
                            ) {
                                Text(
                                    text = "초기화",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = Color.Black
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.BottomCenter
                    )
                    {
                        Row()
                        {
                            Button(modifier = Modifier.width(200.dp), onClick = {
                                SettingPreference.saveSettings(
                                    context = applicationContext,
                                    deviceName = selectedDeviceName,
                                    address = selectedDeviceAddress
                                )
                                // close activity
                                finish()
                            })
                            {
                                Text("저장")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(modifier = Modifier.width(200.dp), onClick = {
                                finish()
                            })
                            {
                                Text("취소")
                            }
                        }
                    }

                }
            }
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