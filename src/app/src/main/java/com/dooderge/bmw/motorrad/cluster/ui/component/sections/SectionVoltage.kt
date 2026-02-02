package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun SectionVoltage
		(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier
) {
	SectionBox(
		modifier = modifier,
		alignment = alignment
	)
	{
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp()),
			horizontalAlignment = Alignment.End
		)
		{
			val voltage = carStatus.voltage.value.toFloat()
			val strVoltage = String.format("%.1f", voltage / 10.0)
			ClusterText(
				text = "${strVoltage} V",
				fontSize = 90f,
				fontWeight = FontWeight.ExtraBold,
			)
		}
	}
}

