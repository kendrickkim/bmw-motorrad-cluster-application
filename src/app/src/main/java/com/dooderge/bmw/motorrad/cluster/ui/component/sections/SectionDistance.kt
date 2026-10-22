package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

@Composable
fun SectionBox(
	modifier: Modifier = Modifier,
	alignment: Alignment = Alignment.TopStart,
	content: @Composable BoxScope.() -> Unit
) {
	Box(
		modifier = modifier,
		contentAlignment = alignment,
		content = content
	)
}

@Composable
fun SectionDistance(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier
) {
	val textColor = Color(0xffaaaaaa)
	val distanceFontSize = 90f

	SectionBox(
		modifier = modifier,
		alignment = alignment
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 8.designedDp()),
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.Top
		) {
			ClusterText(
				text = "${carStatus.odometerKm.value} Km",
				fontSize = distanceFontSize,
				fontWeight = FontWeight.ExtraBold,
				color = textColor
			)
		}
	}
}
