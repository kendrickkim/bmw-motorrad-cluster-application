package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
//		modifier = modifier.border(width = 1.dp, color = Color.Red),
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

	SectionBox(
		modifier = modifier,
		alignment = alignment
	)
	{
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp()),
			horizontalAlignment = Alignment.Start
		)
		{
			val textColor = Color(0xffaaaaaa)

			Box(
				modifier = Modifier,
				contentAlignment = Alignment.TopStart
			)
			{
				ClusterText(
					text = "${12345} Km",
					fontSize = 75f,
					fontWeight = FontWeight.ExtraBold,
					color = textColor
				)
			}

			Box(
			)
			{
				Box(
					modifier = Modifier, contentAlignment = Alignment.TopStart
				)
				{
					ClusterText(
						text = "${0} Km",
						fontSize = 45f,
						fontWeight = FontWeight.ExtraBold,
						color = textColor
					)
				}
				Box(
					modifier = Modifier
						.padding(start = 280.designedDp()),
					contentAlignment = Alignment.TopStart
				)
				{
					ClusterText(
						text = "${0} Km",
						fontSize = 45f,
						fontWeight = FontWeight.ExtraBold,
						color = textColor
					)
				}
			}
		}
	}
}

