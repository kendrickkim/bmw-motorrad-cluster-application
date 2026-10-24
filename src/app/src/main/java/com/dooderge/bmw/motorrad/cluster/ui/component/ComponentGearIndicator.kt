package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R

@Composable
fun ComponentGearIndicator(carStatus: CarStatus, width: Int = 200, height: Int = 200, onLongClick: () -> Unit) {
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
