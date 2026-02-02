package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.screens.fanShape
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs


@Composable
fun ClusterSpeedRpm(
    width: Int,
    height: Int,
    scale: Float,
    carStatus: CarStatus,
//	modifier: Modifier = Modifier
) {

    val rpmDegreeStart = -131.5f
    val rpmDegreeEnd = 131.5f
    val rpmMax = 14000.0f;

    var rpmInner by remember { mutableFloatStateOf(0f) }
    var degreeRpmPin by remember { mutableFloatStateOf(rpmDegreeStart) }


    LaunchedEffect(Unit) {
        val tmrRpmChangeTest = Timer()

        tmrRpmChangeTest.schedule(object : TimerTask() {  // 시간 / 날짜 리프레시 타이머
            override fun run() {
                val rpm = carStatus.rpm.value.toFloat()
//                val rpm = 11000f
                if (rpmInner != rpm) {
                    val unitDiff = abs(rpmInner - rpm) / 23
                    var _rpm = rpmInner
                    if (rpm > rpmInner) {
                        _rpm = _rpm + unitDiff
                    } else {
                        _rpm = _rpm - unitDiff
                    }

                    if (abs(rpm - _rpm) < 10) {
                        _rpm = rpm;
                    }
                    rpmInner = _rpm
                    degreeRpmPin =
                        rpmDegreeStart + (_rpm / rpmMax) * (rpmDegreeEnd - rpmDegreeStart)
                }
            }
        }, 0, 20) // 50fps
    }


    val paddingTop = 161.designedDp()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,

        ) {

        val pinScale: Float = 663f / 840f

        Spacer(Modifier.height(paddingTop))
        Box(
            modifier = Modifier
                .height(height.pxToDp())
                .width(width.pxToDp())
        )
        {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            )
            {
                Box(
                    modifier = Modifier.size(width.pxToDp() * pinScale, height.pxToDp() * pinScale),
                    contentAlignment = Alignment.Center
                )
                {
                    // Speed String

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 150.designedDp()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                    {


                        ClusterText(
                            text = "888",
                            fontSize = 150f,
                            fontWeight = FontWeight.ExtraBold
                        )
                        ClusterText(
                            text = "Km/h",
                            fontSize = 40f,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(40.designedDp()))
                        ClusterText(
                            text = "RPM",
                            fontSize = 32f,
                            fontWeight = FontWeight.Bold
                        )
                        var colorRpmNumber = Color.White;
                        if (rpmInner > 11000)
                            colorRpmNumber = Color(0xffAD3333)
                        else if (rpmInner > 4000)
                            colorRpmNumber = Color(0xff01b2ff)

                        ClusterText(
                            text = rpmInner.toInt().toString(),
                            fontSize = 64f,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorRpmNumber
                        )


                    }

                }

                Box(
                    modifier = Modifier.fillMaxSize()
                )
                {
                    Image(
                        contentDescription = "RPM Guage",
                        painter = painterResource(id = R.drawable.rpm_guage),
                        modifier = Modifier
                            .size(
                                width.pxToDp(),
                                height.pxToDp()
                            )
                            .graphicsLayer {
                                // 2. Set the custom shape and enable clipping.
                                this.shape = fanShape(180 + degreeRpmPin)
                                this.clip = true
                            },
                    )
                }


                // Rpm Image
                Box(
                    modifier = Modifier.size(width.pxToDp() * pinScale, height.pxToDp() * pinScale)
                )
                {
                    Image(
                        contentDescription = "RPM pin",
                        painter = painterResource(id = R.drawable.rpm_pin_area),
                        modifier = Modifier
                            .size(
                                width.pxToDp(),
                                height.pxToDp()
                            )
                            .rotate(degreeRpmPin),
                    )
                }


            }

        }
    }
}

@Composable
fun GearLeveLIndicator(
    carStatus: CarStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.designedDp()),
        contentAlignment = Alignment.BottomCenter
    )
    {
        var colorGearLevel = Color(0xff00BE62)

        Box(
            modifier = Modifier
                .size(200.designedDp(), 200.designedDp())
                .border(
                    width = 10.designedDp(),
                    color = colorGearLevel,
                    shape = RoundedCornerShape(20.designedDp())
                ),
            contentAlignment = Alignment.TopCenter
        )
        {
            ClusterText(
                text = "中",
                fontSize = 180f,
                fontWeight = FontWeight.ExtraBold,
                color = colorGearLevel
            )
        }
    }
}


@Composable
fun ClusterText(
    text: String,
    fontSize: Float,
    fontWeight: FontWeight,
    color: Color = Color.White
) {
    Text(
        text = text,
        fontSize = fontSize.designedSp(),
        fontWeight = fontWeight,
        color = color,
        style = TextStyle(
            // 폰트의 기본 상하단 여백을 제거합니다.
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            ),
            // 추가적으로 줄 높이 조절이 필요할 경우 사용합니다.
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )
    )
}

@Composable
fun rpmString(carStatus: CarStatus) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp),
        contentAlignment = Alignment.BottomCenter
    )
    {
        Text(
            text = carStatus.rpm.toString(),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
    }
}