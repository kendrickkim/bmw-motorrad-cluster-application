package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComponentGearIndicator(gearNo: Int = 0, onLongClick: () -> Unit) {
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
            .size(130.dp, 130.dp)
            .padding(10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(id = getResourceId(gearNo)),
            contentDescription = "Gear Indicator",
            modifier = Modifier.fillMaxSize()
        )
    }
}