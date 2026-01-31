package com.dooderge.bmw.motorrad.cluster.ui.screens

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.ui.component.ComponentCluster
import com.dooderge.bmw.motorrad.cluster.ui.component.ComponentGearIndicator
import com.dooderge.bmw.motorrad.cluster.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

@Composable
fun MainScreen(
    onPermissionsGranted: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    onAndroidAutoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLauncherClick: () -> Unit = {},
    context: Context,
    carStatus: CarStatus
) {
    val strMutTime = remember {
        mutableStateOf("00:00:00")
    }

    val strMutDate = remember {
        mutableStateOf("2026-01-01")
    }

    val strMutWifiNetworkName = remember {
        mutableStateOf("Unknown")
    }

    val strMutBtDeviceName = remember {
        mutableStateOf("NONE")
    }

    LaunchedEffect(Unit) {
        val timerRefreshDateTime = Timer()

        timerRefreshDateTime.schedule(object : TimerTask() {  // 시간 / 날짜 리프레시 타이머
            override fun run() {
                strMutTime.value = java.text.SimpleDateFormat(
                    "HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                strMutDate.value = java.text.SimpleDateFormat(
                    "yyyy-MM-dd",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                strMutWifiNetworkName.value = wifiInfo.ssid
                val btDeviceName: String? = SettingPreference.getDeviceName(context)
                if (btDeviceName == null || btDeviceName.isEmpty()) btDeviceName == "NONE"
                strMutBtDeviceName.value = btDeviceName.toString()
            }
        }, 0, 100)
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = 1f
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
            )
            {
                val wifi_name = strMutWifiNetworkName.value.replace("\"", "")
                Text(
                    text = "       ${wifi_name} / ${strMutBtDeviceName.value}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(
                        text = "${strMutTime.value} ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily(Font(R.font.robot_mono))
                    )
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                // 좌측 버튼 영역
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(150.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Spacer(modifier = Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(128.dp, 128.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.DarkGray)
                                .clickable {
                                    onAndroidAutoClick()
                                }

                        )
                        {
                            Image(
                                contentDescription = "Android auto",
                                painter = painterResource(id = R.drawable.icon_android_auto),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Box(
//                            modifier = Modifier
//                                .size(128.dp, 128.dp)
//                                .clip(RoundedCornerShape(15.dp))
//                                .background(Color.DarkGray)
//                                .clickable {
//                                    onLauncherClick()
//                                }
//
//                        )
//                        {
//                            Image(
//                                contentDescription = "Apps",
//                                painter = painterResource(id = R.drawable.ic_main),
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .padding(20.dp),
//                                contentScale = ContentScale.Fit
//                            )
//                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ComponentGearIndicator(carStatus.gear.value, onLongClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val wifiSettingsIntent =
                                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                            wifiSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(wifiSettingsIntent)

                        }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ComponentCluster(context = context, carStatus = carStatus)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd)
        {
            Box(
                modifier = Modifier
                    .size(60.dp, 60.dp)
                    .padding(10.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onSettingsClick()
                            }
                        )
                    })
            {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(
                        id = R.drawable.icon_setting
                    ),
                    contentDescription = "icon_setting",

                    )
            }
        }
    }
}

@Composable
fun Speedometer(
    speed: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = speed.toString(),
            color = Color.White,
            style = TextStyle(
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "km/h",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 100.dp)
        )
    }
}

@Composable
fun RpmGauge(
    rpm: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width.coerceAtMost(size.height) * 0.4f

        // RPM 게이지 배경
        drawArc(
            color = Color.DarkGray,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = 20f),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // RPM 게이지 표시
        val rpmPercentage = (rpm / 10000f).coerceIn(0f, 1f)
        drawArc(
            color = when {
                rpmPercentage > 0.9f -> Color.Red
                rpmPercentage > 0.7f -> Color.Yellow
                else -> Color.Green
            },
            startAngle = 180f,
            sweepAngle = 180f * rpmPercentage,
            useCenter = false,
            style = Stroke(width = 20f),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // RPM 값 표시
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                rpm.toString(),
                center.x,
                center.y + 20f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

@Composable
fun FuelGauge(
    fuelLevel: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width.coerceAtMost(size.height) * 0.4f

            // 연료 게이지 배경
            drawArc(
                color = Color.DarkGray,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 8f),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // 연료량 표시
            drawArc(
                color = when {
                    fuelLevel < 0.2f -> Color.Red
                    fuelLevel < 0.4f -> Color.Yellow
                    else -> Color.Green
                },
                startAngle = -90f,
                sweepAngle = 180f * fuelLevel,
                useCenter = false,
                style = Stroke(width = 8f),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
    }
}