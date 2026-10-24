package com.dooderge.bmw.motorrad.cluster.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
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
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterSpeedRpm
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.ComponentGearIndicator
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionDistance
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import com.dooderge.bmw.motorrad.cluster.ui.component.pxToDp
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionApps
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionCoolantTemp
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionFuelGuage
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionTime
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionTirePressure
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionVoltage
// DO NOT DELETE — SectionDirection import (슬롯은 Trip으로 임시 교체, 재사용 예정)
import com.dooderge.bmw.motorrad.cluster.ui.component.sections.SectionDirection
import com.dooderge.bmw.motorrad.cluster.ui.component.setDesignedSizeByWidth
import com.dooderge.bmw.motorrad.cluster.ui.component.setDesginedSizeByHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
data class HeaderInfo(
    var strNetworkInfo: MutableState<String> = mutableStateOf(""),
    var strTimeInfo: MutableState<String> = mutableStateOf(""),
    var strDateInfo: MutableState<String> = mutableStateOf(""),
    var repeaterLeft: MutableState<Int> = mutableIntStateOf(0),
    var repeaterRight: MutableState<Int> = mutableIntStateOf(0)
)

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
    onAndroidAutoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLauncherClick: () -> Unit = {},
    onTpmsClick: () -> Unit = {},
    onEngineClick: () -> Unit = {},
    context: Context,
    carStatus: CarStatus,
    firstBoot : Boolean
) {

    var sizeScreen by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeDisplayBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeRpmGuageImage by remember { mutableStateOf(IntSize(1, 1)) }
    var sizeDisplayRpmGuageImage by remember { mutableStateOf(IntSize(1, 1)) }
    // var ratioImageAndScreen by remember { mutableFloatStateOf(1f) }

    val scaleImage = remember {
        mutableFloatStateOf(1f)
    }

    val headerInfo = remember { HeaderInfo() }

    val btConnected = carStatus.bt_connection_status.value
    val linCapturePlaying = carStatus.linCapturePlaying.value
    LaunchedEffect(btConnected, linCapturePlaying) {
        if (!btConnected && !linCapturePlaying) {
            carStatus.gear.value = -1
            carStatus.rpm.value = 0
            carStatus.speed.value = 0
            carStatus.voltage.value = 0
            carStatus.coolant_temperature.value = 0
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    LaunchedEffect(Unit) {
        // 시간 / 날짜 리프레시
        while (isActive) {
            val now = LocalDateTime.now()
            val time = now.format(timeFormatter)
            val date = now.format(dateFormatter)
            if (headerInfo.strTimeInfo.value != time) headerInfo.strTimeInfo.value = time
            if (headerInfo.strDateInfo.value != date) headerInfo.strDateInfo.value = date
            val btName = SettingPreference.getDeviceName(context).orEmpty().ifEmpty { "[BT]NONE" }
            if (headerInfo.strNetworkInfo.value != btName) headerInfo.strNetworkInfo.value = btName
            delay(500)
        }
    }

    LaunchedEffect(firstBoot) {
        if (firstBoot) {
            delay(500)
            carStatus.rpm.value = 14000
            delay(2000)
        }
        carStatus.rpm.value = 0
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

            val ratioImageSize = if (ratioScreen < ratioBackgroundImage) {
                setDesignedSizeByWidth(sizeScreen.width.toFloat())
                sizeScreen.width.toFloat() / sizeBackgroundImage.width.toFloat()
            } else {
                setDesginedSizeByHeight(sizeScreen.height.toFloat())
                sizeScreen.height.toFloat() / sizeBackgroundImage.height.toFloat()
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

                        HeaderSection(
                            carStatus = carStatus,
                            headerInfo = headerInfo,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 50.designedDp()),
                            contentAlignment = Alignment.BottomCenter
                        )
                        {
                            ComponentGearIndicator(carStatus, 210, 210, onLongClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val wifiSettingsIntent =
                                        Intent(Settings.ACTION_WIFI_SETTINGS)
                                    wifiSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(wifiSettingsIntent)
                                }
                            })
                        }

                        ClusterSpeedRpm(
                            width = sizeDisplayRpmGuageImage.width,
                            height = sizeDisplayRpmGuageImage.height,
                            carStatus = carStatus
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

                        val top1 = 162.designedDp()
                        val top2 = 456.designedDp()
                        val top3 = 749.designedDp()
                        val left = 19.designedDp()
                        val left1 = 420.designedDp()
                        val right1 = 19.designedDp()
                        val right2 = 421.designedDp()

                        val status = arrayOf(
                            SectionInfo(
                                "Distance",
                                PaddingValues(start = left, top = top1),
                            ),
                            SectionInfo(
                                "Range",
                                PaddingValues(end = right1, top = top1),
                                Alignment.TopEnd
                            ),
                            // 구 Direction 슬롯 → Trip 표시 (Direction UI는 삭제하지 말 것)
                            SectionInfo(
                                "Trip",
                                PaddingValues(end = right2, top = top1),
                                Alignment.TopEnd
                            ),

                            SectionInfo(
                                "Status",
                                PaddingValues(start = left, top = top2),
                                Alignment.TopStart
                            ),
                            // 구 Fuel Guage 슬롯 → Tire Pressure 표시
                            SectionInfo(
                                "Tire Pressure",
                                PaddingValues(end = right1, top = top2),
                                Alignment.TopEnd
                            ),


                            SectionInfo(
                                "Voltage",
                                PaddingValues(start = left, top = top3),
                                Alignment.TopStart
                            ),
                            SectionInfo(
                                "Eng Temp.",
                                PaddingValues(start = left1, top = top3),
                                Alignment.TopStart
                            ),

                            SectionInfo(
                                "Apps",
                                PaddingValues(end = right1, top = top3),
                                Alignment.TopEnd
                            ),

                        )

                        status.forEach { s ->
                            SectionText(
                                text = s.text,
                                padding = s.padding,
                                align = s.alignment
                            )
                        }
                    }

                    // 클러스터 텍스트 정보

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart)
                    {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopStart
                        )
                        {
                            SectionDistance(
                                modifier = Modifier
                                    .padding(start = 60.designedDp(), top = 232.designedDp())
                                    .size(554.designedDp(), 161.designedDp()),
                                carStatus = carStatus
                            )

                            SectionStatus(
                                modifier = Modifier
                                    .padding(start = 30.designedDp(), top = 524.designedDp())
                                    .size(554.designedDp(), 161.designedDp()),
                                carStatus = carStatus,
                                onEngineClick = onEngineClick
                            )

                            SectionVoltage(
                                modifier = Modifier
                                    .padding(start = 40.designedDp(), top = 822.designedDp())
                                    .size(316.designedDp(), 161.designedDp()),
                                carStatus = carStatus,
                            )

                            SectionCoolantTemp(
                                modifier = Modifier
                                    .padding(start = 453.designedDp(), top = 822.designedDp())
                                    .size(226.designedDp(), 161.designedDp()),
                                carStatus = carStatus,
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd)
                        {

                            SectionTime(
                                modifier = Modifier
                                    .padding(end = 433.designedDp(), top = 232.designedDp())
                                    .size(226.designedDp(), 161.designedDp()),
                                carStatus = carStatus,
                                alignment = Alignment.TopEnd
                            )

                            /*
                             * =====================================================================
                             * DO NOT DELETE — SectionDirection (나침반/헤딩)
                             * 다른 에이전트: 아래 블록을 제거하지 말 것. 슬롯만 Trip으로 임시 교체.
                             * 파일: ui/component/sections/SectionDirection.kt 도 유지.
                             * =====================================================================
                             *
                             * SectionDirection(
                             *     modifier = Modifier
                             *         .padding(end = 433.designedDp(), top = 232.designedDp())
                             *         .size(226.designedDp(), 161.designedDp()),
                             *     carStatus = carStatus,
                             *     alignment = Alignment.TopEnd
                             * )
                             */

                            // 구 Time 슬롯 → Range
                            SectionFuelGuage(
                                modifier = Modifier
                                    .padding(end = 28.designedDp(), top = 232.designedDp())
                                    .size(316.designedDp(), 161.designedDp()),
                                carStatus = carStatus,
                                alignment = Alignment.TopEnd,
                                compact = true
                            )

                            // 구 Fuel Guage 슬롯 → Tire Pressure (앞/뒤 압력·온도)
                            SectionTirePressure(
                                modifier = Modifier
                                    .padding(end = 28.designedDp(), top = 524.designedDp())
                                    .size(554.designedDp(), 150.designedDp()),
                                carStatus = carStatus,
                                alignment = Alignment.TopEnd
                            )

                            SectionApps(
                                modifier = Modifier
                                    .padding(end = 30.designedDp(), top = 822.designedDp())
                                    .size(640.designedDp(), 150.designedDp()),
                                carStatus = carStatus,
                                alignment = Alignment.TopEnd,
                                onApplicationEvent = {
                                    when (it) {
                                        "setting" -> onSettingsClick()
                                        "android_auto" -> onAndroidAutoClick()
                                        "drawer" -> onLauncherClick()
                                        "tpms" -> onTpmsClick()
                                    }
                                }
                            )


                        }

                    }


                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    carStatus: CarStatus,
    headerInfo: HeaderInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(161.designedDp())
    )
    {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        )
        {
            ClusterText(
                text = if (carStatus.linCapturePlaying.value) {
                    "Simulation : ${carStatus.linCaptureLine.value}"
                } else {
                    headerInfo.strNetworkInfo.value
                },
                60f,
                FontWeight.ExtraBold
            )
        }


        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        )
        {
            ClusterText(headerInfo.strTimeInfo.value, 80f, FontWeight.ExtraBold)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        )
        {
            ClusterText(headerInfo.strDateInfo.value, 60f, FontWeight.ExtraBold)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 600.designedDp()),
            contentAlignment = Alignment.CenterStart

        )
        {
            Image(
                contentDescription = "Repeater Left",
                painter = painterResource(id = R.drawable.repeater_off),
                modifier = Modifier
                    .size(
                        111.designedDp(),
                        125.designedDp(),
                    )
                    .rotate(0f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 600.designedDp()),
            contentAlignment = Alignment.CenterEnd

        )
        {
            Image(
                contentDescription = "Repeater Right",
                painter = painterResource(id = R.drawable.repeater_off),
                modifier = Modifier
                    .size(
                        111.designedDp(),
                        125.designedDp(),
                    )
                    .rotate(180f),
            )
        }

    }
}


