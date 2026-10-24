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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.screens.fanShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt


@Composable
fun ClusterSpeedRpm(
	width: Int,
	height: Int,
	carStatus: CarStatus
) {

	val rpmDegreeStart = -134f
	val rpmDegreeEnd = 134f
	val rpmMax = 14000.0f;

	var rpmInner by remember { mutableFloatStateOf(0f) }
	var degreeRpmPin by remember { mutableFloatStateOf(rpmDegreeStart) }
	var speedShown by remember { mutableIntStateOf(0) }

	LaunchedEffect(Unit) {
		var speedValue = 0f
		var rpmValue = 0f
		while (isActive) {
			val speedTarget = carStatus.speed.value.toFloat()
			if (speedValue != speedTarget) {
				speedValue += (speedTarget - speedValue) / 10f
				if (abs(speedTarget - speedValue) < 0.05f) speedValue = speedTarget
			}
			val roundedSpeed = speedValue.roundToInt()
			if (speedShown != roundedSpeed) speedShown = roundedSpeed

			val rpmTarget = carStatus.rpm.value.toFloat()
			if (rpmValue != rpmTarget) {
				val step = abs(rpmValue - rpmTarget) / 13f
				var next = if (rpmTarget > rpmValue) rpmValue + step else rpmValue - step
				if (abs(rpmTarget - next) < 10f) next = rpmTarget
				rpmValue = next
				rpmInner = next
				degreeRpmPin =
					rpmDegreeStart + (next / rpmMax) * (rpmDegreeEnd - rpmDegreeStart)
			}
			delay(20) // 50fps
		}
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
							.padding(top = 120.designedDp()),
						verticalArrangement = Arrangement.Top,
						horizontalAlignment = Alignment.CenterHorizontally
					)
					{

						Box(
							modifier = Modifier.size(412.designedDp(), 279.designedDp()),
							contentAlignment = Alignment.TopCenter
						)
						{
							Box(
								modifier = Modifier.fillMaxSize(),
								contentAlignment = Alignment.TopCenter
							)
							{
								ClusterText(
									text = speedShown.toString(),
									fontSize = 240f,
									fontWeight = FontWeight.ExtraBold,
									color = Color.White
								)
							}
							Box(
								modifier = Modifier.fillMaxSize().padding(top = 225.designedDp()),
								contentAlignment = Alignment.TopCenter
							)
							{
								ClusterText(
									text = "Km/h",
									fontSize = 50f,
									fontWeight = FontWeight.Bold
								)
							}
						}

						Spacer(Modifier.height(40.designedDp()))
						ClusterText(
							text = "RPM",
							fontSize = 40f,
							fontWeight = FontWeight.Bold
						)
						var colorRpmNumber = Color.White;
						if (rpmInner > 11000)
							colorRpmNumber = Color(0xffAD3333)
						else if (rpmInner > 4000)
							colorRpmNumber = Color(0xff01b2ff)

						ClusterText(
							text = rpmInner.toInt().toString(),
							fontSize = 90f,
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
