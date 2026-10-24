package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import kotlinx.coroutines.delay

@Composable
fun SectionTime(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier
) {
	var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
	val ignitionOn = carStatus.isIgnitionOn.value
	LaunchedEffect(ignitionOn) {
		if (!ignitionOn) return@LaunchedEffect
		while (true) {
			nowMillis = System.currentTimeMillis()
			delay(1000)
		}
	}
	val startedAt = carStatus.tripStartedAtMillis.value
	val elapsedMillis = if (ignitionOn && carStatus.tripActive.value && startedAt > 0L) {
		(nowMillis - startedAt).coerceAtLeast(0L)
	} else {
		0L
	}
	val totalMinutes = elapsedMillis / 60000L
	val hours = totalMinutes / 60L
	val minutes = totalMinutes % 60L
	val tripKm = if (ignitionOn && carStatus.tripBaseLatched.value) {
		(carStatus.odometerKm.value - carStatus.tripBaseOdometerKm.value).coerceAtLeast(0)
	} else {
		0
	}

	val lineColor = Color(0xffaaaaaa)
	val lineSize = 75f
	val distanceSize = lineSize * 0.8f

	SectionBox(
		modifier = modifier,
		alignment = alignment
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp()),
			horizontalAlignment = Alignment.End
		) {
			Column(
				modifier = Modifier.wrapContentWidth(
					align = Alignment.End,
					unbounded = true
				),
				horizontalAlignment = Alignment.End
			) {
				ClusterText(
					text = "%02d:%02d".format(hours, minutes),
					fontSize = lineSize,
					fontWeight = FontWeight.ExtraBold,
					color = lineColor,
					softWrap = false,
					maxLines = 1
				)
				ClusterText(
					text = "$tripKm Km",
					fontSize = distanceSize,
					fontWeight = FontWeight.ExtraBold,
					color = lineColor,
					softWrap = false,
					maxLines = 1
				)
			}
		}
	}
}
