package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

@Composable
fun SectionWeather
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
		Row()
		{
			Box(modifier = Modifier.fillMaxHeight().width(300.designedDp()),
				contentAlignment = Alignment.Center)
			{
				Image(
					contentDescription = "weather",
					painter = painterResource(id = R.drawable.icon_weather_sunny_cloudy),
					modifier = Modifier.fillMaxWidth(),
					contentScale = ContentScale.Fit,
					colorFilter = ColorFilter.tint(Color(0xffaaaaaa))
				)
			}
			Box(modifier = Modifier.fillMaxHeight().width(250.designedDp()),
				contentAlignment = Alignment.CenterEnd)
			{
				ClusterText(
					text = "${20} ℃",
					fontSize = 90f,
					fontWeight = FontWeight.ExtraBold,
				)
			}
		}
	}
}

