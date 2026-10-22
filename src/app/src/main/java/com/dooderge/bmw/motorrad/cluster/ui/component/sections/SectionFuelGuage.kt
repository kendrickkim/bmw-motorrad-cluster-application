package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

@Composable
fun SectionFuelGuage(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
	/** 구 Direction 슬롯(좁은 영역)용 콤팩트 표시 */
	compact: Boolean = false
) {
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
			val rangeKm = carStatus.fuelRangeKm.value
			val gauge = (rangeKm * 100 / 250).coerceIn(0, 100)
			if (compact) {
				// 좁은 Direction 슬롯에서 줄바꿈되면 "Km"이 잘리므로 한 줄·너비 제한 해제
				Column(
					modifier = Modifier.wrapContentWidth(
						align = Alignment.End,
						unbounded = true
					),
					horizontalAlignment = Alignment.End
				) {
					ClusterText(
						text = "$gauge %",
						fontSize = lineSize,
						fontWeight = FontWeight.ExtraBold,
						color = lineColor,
						softWrap = false,
						maxLines = 1
					)
					ClusterText(
						text = "$rangeKm Km",
						fontSize = distanceSize,
						fontWeight = FontWeight.ExtraBold,
						color = lineColor,
						softWrap = false,
						maxLines = 1
					)
				}
			} else {
				ClusterText(
					text = "$gauge % / $rangeKm Km",
					fontSize = 135f,
					fontWeight = FontWeight.ExtraBold,
					color = lineColor
				)
			}
		}
	}
}
