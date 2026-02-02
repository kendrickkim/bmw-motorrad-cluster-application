package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComponentGearIndicator(carStatus: CarStatus, width : Int = 200, height : Int = 200, onLongClick: () -> Unit) {
    fun getResourceId(gearNo: Int): Int {
        return when (gearNo) {
            0 -> R.drawable.icon_gear_0
            1 -> R.drawable.icon_gear_1
            2 -> R.drawable.icon_gear_2
            3 -> R.drawable.icon_gear_3
            4 -> R.drawable.icon_gear_4
            5 -> R.drawable.icon_gear_5
            6 -> R.drawable.icon_gear_6
            9 -> R.drawable.icon_gear_9
            else -> R.drawable.icon_gear_0
        }
    }

    Box(
        modifier = Modifier
            .size(width.designedDp(), height.designedDp())
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    }
                )
            },
		contentAlignment = Alignment.CenterEnd

    ) {
        Image(
            painter = painterResource(id = getResourceId(carStatus.gear.value)),
            contentDescription = "Gear Indicator",
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun ComponentGearIndicator2(
	carStatus: CarStatus,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.padding(bottom = 50.designedDp()),
		contentAlignment = Alignment.BottomCenter
	)
	{
		var colorGearLevel = Color(0xff00BE62)

		Box(
			modifier = Modifier
				.size(200.designedDp(), 200.designedDp())
				.border(
					width = 10.designedDp(),
					color = colorGearLevel,
					shape = RoundedCornerShape(20.designedDp())
				),
			contentAlignment = Alignment.Center
		)
		{
			ClusterText(
				text = "N",
				fontSize = 180f,
				fontWeight = FontWeight.ExtraBold,
				color = colorGearLevel
			)
		}
	}
}
