package com.dooderge.bmw.motorrad.cluster.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterSpeedRpm
import com.dooderge.bmw.motorrad.cluster.ui.component.GearLeveLIndicator
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import com.dooderge.bmw.motorrad.cluster.ui.component.pxToDp
import com.dooderge.bmw.motorrad.cluster.ui.component.setDesignedSizeByWidth
import com.dooderge.bmw.motorrad.cluster.ui.component.setDesginedSizeByHeight
import com.dooderge.bmw.motorrad.cluster.viewmodel.MainViewModel
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

fun fanShape(targetDegree: Float) = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            // 1. 부채꼴의 꼭짓점을 이미지의 정중앙으로 이동합니다.
            moveTo(size.width / 2, size.height / 2)

            // 2. 호(arc)를 그릴 사각형(Rect)을 정의합니다.
            //    - Rect의 크기를 2배로 하여 중앙점을 기준으로 전체 이미지를 덮도록 합니다.
            //    - Rect의 시작점을 (0,0)으로 설정합니다.
            val rect = Rect(Offset.Zero, size)

            // 3. 중앙점에서 시작하여 호의 시작점까지 선을 긋고, 호를 그립니다.
            arcTo(
                rect = rect,
                startAngleDegrees = -270f,           // 0도 (3시 방향) 에서 시작
                sweepAngleDegrees = targetDegree, // 입력받은 각도만큼 그림
                forceMoveTo = false               // 현재 위치에서 호의 시작점까지 선을 연결함
            )

            // 4. 경로를 닫아 호의 끝점에서 중앙점으로 선을 연결합니다.
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

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

    var strMutTime by remember { mutableStateOf("00:00:00") }
    var strMutDate by remember { mutableStateOf("2026-01-01") }
    var strWifiName by remember { mutableStateOf("Unknown") }
    var strBtDeviceName by remember { mutableStateOf("NONE") }

    var sizeScreen by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeDisplayBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeRpmGuageImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeDisplayRpmGuageImage by remember { mutableStateOf(IntSize(1, 1)) }
    var ratioImageAndScreen by remember { mutableFloatStateOf(1f) }

    val scaleImage = remember {
        mutableFloatStateOf(1f)
    }

    LaunchedEffect(Unit) {
//		val timerRefreshDateTime = Timer()
//
//		timerRefreshDateTime.schedule(object : TimerTask() {  // 시간 / 날짜 리프레시 타이머
//			override fun run() {
//				strMutTime = java.text.SimpleDateFormat(
//					"HH:mm:ss",
//					java.util.Locale.getDefault()
//				).format(java.util.Date())
//
//				strMutDate = java.text.SimpleDateFormat(
//					"yyyy-MM-dd",
//					java.util.Locale.getDefault()
//				).format(java.util.Date())
//
//				val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//				val wifiInfo = wifiManager.connectionInfo
//				strWifiName = wifiInfo.ssid
//				val btDeviceName: String? = SettingPreference.getDeviceName(context)
//				if (btDeviceName == null || btDeviceName.isEmpty()) btDeviceName == "NONE"
//				strBtDeviceName = btDeviceName.toString()
//
////				carStatus.rpm.value = (carStatus.rpm.value + 1) % 12000
//
//			}
//		}, 0, 100)

        var tmrChangeRpm = Timer()
        tmrChangeRpm.schedule(object : TimerTask() {
            override fun run() {
                carStatus.rpm.value = Random.nextInt(0, 14000)
            }
        }, 0, 1000 * 5)
    }

    LaunchedEffect(sizeScreen)
    {
        if (sizeScreen.width > 1) {

            if (sizeBackgroundImage.width == 1) {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.cluster_background_black,
                    options
                )

                sizeBackgroundImage = IntSize(options.outWidth, options.outHeight)

                android.graphics.BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.rpm_guage,
                    options
                )

                sizeRpmGuageImage = IntSize(options.outWidth, options.outHeight)
            }


            val ratioScreen = sizeScreen.width.toFloat() / sizeScreen.height.toFloat()
            val ratioBackgroundImage =
                sizeBackgroundImage.width.toFloat() / sizeBackgroundImage.height.toFloat()

            var ratioImageSize = 1f
            if (ratioScreen < ratioBackgroundImage) {
                ratioImageSize = sizeScreen.width.toFloat() / sizeBackgroundImage.width.toFloat()
                setDesignedSizeByWidth(sizeScreen.width.toFloat())

            } else {
                ratioImageSize = sizeScreen.height.toFloat() / sizeBackgroundImage.height.toFloat()
                setDesginedSizeByHeight(sizeScreen.height.toFloat())
            }

            sizeDisplayBackgroundImage =
                IntSize(
                    (sizeBackgroundImage.width.toFloat() * ratioImageSize).toInt(),
                    (sizeBackgroundImage.height.toFloat() * ratioImageSize).toInt()
                )


            scaleImage.floatValue = ratioImageSize

            sizeDisplayRpmGuageImage = IntSize(
                (sizeRpmGuageImage.width * scaleImage.floatValue).toInt(),
                (sizeRpmGuageImage.height * scaleImage.floatValue).toInt()
            )
        }

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
                .onSizeChanged({
                    sizeScreen = IntSize(it.width, it.height)

                }), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Box(    // 화면 전체 background
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // 화면의 가로, 세로 크기를 가져옵니다.
                        val screenWidth = this.size.width
                        val screenHeight = this.size.height

                        // 타원의 반지름을 설정합니다. 가로/세로 비율에 따라 타원이 됩니다.
                        // 가로 길이를 기준으로 반지름을 설정하고, 세로 길이는 화면 비율에 맞춥니다.
                        val radiusX = screenWidth
                        val radiusY = screenHeight

                        // 방사형 그라데이션 브러시를 생성합니다.
                        // Canvas 내부에서 사용할 때는 center와 radius를 drawOval 내부에서 처리하므로
                        // 브러시 자체에는 지정할 필요가 없습니다.
                        val radialBrush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xff383838),
                                0.8f to Color(0xff0f0f0f),
                                1.0f to Color.Black // 가장자리를 더 어둡게 처리
                            ),
                            // 중심은 drawOval에서 지정
                            center = this.center,
                            // 반지름은 더 큰 값을 기준으로 설정하여 타원을 완전히 덮도록 합니다.
                            radius = kotlin.comparisons.maxOf(radiusX, radiusY) * 0.5f
                        )

                        // 1. 검은색 배경을 먼저 전체에 그립니다.
                        drawRect(color = Color.Black)

                        // 2. 그 위에 타원형 그라데이션을 그립니다.
                        drawOval(
                            brush = radialBrush,
                            // 타원의 위치와 크기를 지정합니다.
                            topLeft = Offset(center.x - radiusX, center.y - radiusY),
                            size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2)
                        )
                    },
                contentAlignment = Alignment.Center
            )
            {
                Box(            // 그림 사이즈에 맞춘 box
                    modifier = Modifier
                        .size(
                            sizeDisplayBackgroundImage.width.pxToDp(),
                            sizeDisplayBackgroundImage.height.pxToDp()
                        ),
                    contentAlignment = Alignment.Center
                )
                {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    )// 클러스터 디자인 background
                    {
                        Image(
                            contentDescription = "Cluster Background",
                            painter = painterResource(id = R.drawable.cluster_background_black),
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        GearLeveLIndicator(
                            carStatus = carStatus,
                            modifier = Modifier.width(300.designedDp())
                        )


                        ClusterSpeedRpm(
                            width = sizeDisplayRpmGuageImage.width,
                            height = sizeDisplayRpmGuageImage.height,
                            carStatus = carStatus,
                            scale = scaleImage.floatValue
                        )
                    }

                    // Section Text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                    {
                        @Composable
                        fun SectionText(
                            text: String,
                            padding: PaddingValues,
                            align: Alignment = Alignment.TopStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = align
                            )
                            {
                                Text(
                                    text = text,
                                    fontSize = 17.sp,
                                    color = Color(0xff4b4b4b),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }


                        data class SectionInfo(
                            val text: String,
                            val padding: PaddingValues,
                            val alignment: Alignment = Alignment.TopStart
                        )

                        val top1 = 202.designedDp()
                        val top2 = 550.dp
                        val top3 = 885.dp
                        val left = 15.dp
                        val right1 = 25.dp
                        val right2 = 380.dp

                        val status = arrayOf(
                            SectionInfo(
                                "Distance",
                                PaddingValues(start = left, top = top1),
                            ),
                            SectionInfo(
                                "Time",
                                PaddingValues(end = right1, top = top1),
                                Alignment.TopEnd
                            ),
                            SectionInfo(
                                "Fuel Guage",
                                PaddingValues(end = right2, top = top1),
                                Alignment.TopEnd
                            ),

                            SectionInfo(
                                "Status",
                                PaddingValues(start = left, top = top2),
                                Alignment.TopStart
                            ),
                            SectionInfo(
                                "Weather",
                                PaddingValues(end = right1, top = top2),
                                Alignment.TopEnd
                            ),

                            SectionInfo(
                                "Apps",
                                PaddingValues(start = left, top = top3),
                                Alignment.TopStart
                            ),
                            SectionInfo(
                                "Voltage",
                                PaddingValues(end = right1, top = top3),
                                Alignment.TopEnd
                            ),
                            SectionInfo(
                                "Temp.",
                                PaddingValues(end = right2, top = top3),
                                Alignment.TopEnd
                            ),
//
//                            SectionInfo(
//                                "Time",
//                                SesionPadding(0, 202, 0, 5),
//                                Alignment.TopEnd
//                            ),

                        )

                        status.forEach { s ->

//                            val newPadding = PaddingValues(
//                                s.padding.calculateStartPadding(LayoutDirection.Ltr) * scaleImage.value,
//                                s.padding.calculateTopPadding() * scaleImage.value,
//                                s.padding.calculateEndPadding(LayoutDirection.Ltr) * scaleImage.value,
//                                s.padding.calculateBottomPadding() * scaleImage.value,
//                            )

                            SectionText(
                                text = s.text,
                                padding = s.padding,
                                align = s.alignment
                            )
                        }
                    }

                    // 클러스터 텍스트 정보
//                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart)
//                    {
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 98.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus
//                        )
//
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 209.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus
//                        )
//
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 320.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus
//                        )
//
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 98.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus,
//                            alignment = Alignment.TopEnd
//                        )
//
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 209.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus
//                            , alignment = Alignment.TopEnd
//                        )
//
//                        SectionDistance(
//                            modifier = Modifier
//                                .padding(top = 320.dp)
//                                .size(250.dp, 70.dp),
//                            carStatus = carStatus
//                            , alignment = Alignment.TopEnd
//                        )
//                    }


//              Text(
//                text = "       ${wifi_name} / ${strMutBtDeviceName.value}",
//                fontSize = 15.sp,
//                fontWeight = FontWeight.Bold,
//              )
//              Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//              )
//              {
//                Text(
//                  text = "${strMutTime.value} ",
//                  fontSize = 28.sp,
//                  fontWeight = FontWeight.ExtraBold,
//                  fontFamily = FontFamily(Font(R.font.robot_mono))
//                )
//              }
//            }
//
//
//            Row(
//              modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .padding(8.dp)
//            ) {
//              // 좌측 버튼 영역
//              Column(
//                modifier = Modifier
//                  .fillMaxHeight()
//                  .width(150.dp),
//                verticalArrangement = Arrangement.SpaceBetween,
//                horizontalAlignment = Alignment.CenterHorizontally
//              ) {
//
//
//                Spacer(modifier = Modifier.height(8.dp))
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                  Box(
//                    modifier = Modifier
//                      .size(128.dp, 128.dp)
//                      .clip(RoundedCornerShape(15.dp))
//                      .background(Color.DarkGray)
//                      .clickable {
//                        onAndroidAutoClick()
//                      }
//
//                  )
//                  {
//                    Image(
//                      contentDescription = "Android auto",
//                      painter = painterResource(id = R.drawable.icon_android_auto),
//                      modifier = Modifier
//                        .fillMaxSize()
//                        .padding(20.dp),
//                      contentScale = ContentScale.Fit
//                    )
//                  }
////                        Spacer(modifier = Modifier.height(16.dp))
////                        Box(
////                            modifier = Modifier
////                                .size(128.dp, 128.dp)
////                                .clip(RoundedCornerShape(15.dp))
////                                .background(Color.DarkGray)
////                                .clickable {
////                                    onLauncherClick()
////                                }
////
////                        )
////                        {
////                            Image(
////                                contentDescription = "Apps",
////                                painter = painterResource(id = R.drawable.ic_main),
////                                modifier = Modifier
////                                    .fillMaxSize()
////                                    .padding(20.dp),
////                                contentScale = ContentScale.Fit
////                            )
////                        }
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//                ComponentGearIndicator(carStatus.gear.value, onLongClick = {
//                  CoroutineScope(Dispatchers.Main).launch {
//                    val wifiSettingsIntent =
//                      Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
//                    wifiSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startActivity(wifiSettingsIntent)
//
//                  }
//                })
//                Spacer(modifier = Modifier.height(8.dp))
//              }
//              Spacer(modifier = Modifier.width(16.dp))
//              Box(
//                modifier = Modifier
//                  .fillMaxSize()
//                  .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
//                contentAlignment = Alignment.BottomCenter
//              ) {
//                ComponentCluster(context = context, carStatus = carStatus)
//              }
//            }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        )
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


