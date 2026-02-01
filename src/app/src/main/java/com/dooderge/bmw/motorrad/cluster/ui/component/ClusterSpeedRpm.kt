package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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


    val paddingTop = (300.toFloat() * scale).pxToDp()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {

        val pinScale: Float = 663f / 840f

        Spacer(Modifier.height(paddingTop))
        Box(
            modifier = Modifier.size(
                width.pxToDp(),
                height.pxToDp()
            )
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.TopCenter
                    )
                    {
                        Text(
                            text = "888",
                            fontSize = 55.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 110.dp),
                        contentAlignment = Alignment.TopCenter
                    )
                    {
                        Text(
                            text = "Km/h",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Rpm String
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.BottomCenter
                    )
                    {
                        Text(
                            text = "RPM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 50.dp),
                        contentAlignment = Alignment.BottomCenter
                    )
                    {
                        Text(
                            text = rpmInner.toInt().toString(),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
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