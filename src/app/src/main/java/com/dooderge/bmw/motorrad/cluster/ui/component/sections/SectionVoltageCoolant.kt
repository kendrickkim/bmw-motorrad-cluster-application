package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.clusterValueUnitText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

/** Voltage + Eng Temp — 고정 폭 Box로 분리해 전압 자리수 변화에도 온도 left 고정 */
@Composable
fun SectionVoltageCoolant(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
) {
	val voltageLeading = 10.dp
	val voltageBoxWidth = 280.designedDp()
	val tempGap = 10.dp
	SectionBox(
		modifier = modifier,
		alignment = alignment
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp())
		) {
			val voltage = carStatus.voltage.value.toFloat()
			val strVoltage = String.format("%.1f", voltage / 10.0)
			val celsius = carStatus.coolant_temperature.value / 10
			Box(
				modifier = Modifier
					.padding(start = voltageLeading)
					.width(voltageBoxWidth)
					.align(Alignment.TopStart)
			) {
				ClusterText(
					text = clusterValueUnitText(strVoltage, " v"),
					fontSize = 90f,
					fontWeight = FontWeight.ExtraBold,
				)
			}
			Box(
				modifier = Modifier
					.padding(start = voltageLeading + voltageBoxWidth + tempGap)
					.align(Alignment.TopStart)
			) {
				ClusterText(
					text = clusterValueUnitText("$celsius", " °C"),
					fontSize = 90f,
					fontWeight = FontWeight.ExtraBold,
				)
			}
		}
	}
}
