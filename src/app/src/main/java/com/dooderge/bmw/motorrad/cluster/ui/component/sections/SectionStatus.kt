package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun SectionStatus(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier
) {

	@Composable
	fun StatusBox(description : String, resouceId : Int, status : Int = 0)
	{
		Box(
			modifier = Modifier
				.size(200.designedDp(), 100.designedDp()),
			contentAlignment = Alignment.TopStart
		)
		{
			Image(
				contentDescription = description,
				painter = painterResource(id = resouceId),
				modifier = Modifier.fillMaxWidth(),
				contentScale = ContentScale.Fit,
				colorFilter = ColorFilter.tint(Color(0xff666666))
			)
		}

	}

	SectionBox(
		modifier = modifier,
		alignment = alignment
	)
	{
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp()),
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.Center
		)
		{
			Row(
				modifier = Modifier.fillMaxSize(),
				horizontalArrangement = Arrangement.SpaceEvenly,
				verticalAlignment = Alignment.CenterVertically
			)
			{
				StatusBox("Engine", R.drawable.icon_status_eng, 0)
				StatusBox("Gear", R.drawable.icon_status_warning, 0)
				StatusBox("BT", R.drawable.icon_status_bt, 0)

			}
		}
	}
}

