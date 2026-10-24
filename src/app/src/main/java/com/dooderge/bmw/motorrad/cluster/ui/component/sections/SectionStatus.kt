package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
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
    modifier: Modifier = Modifier,
    onEngineClick: () -> Unit = {}
) {

    val colorOff = Color(0xff666666)
    val colorBtStatusOn = Color(0xff327DFF)
    val colorENGStatusOn = Color(0xFFFFC107)
    val colorCAUStatusOn = Color(0xFFF44336)

    var blinkingBtStatusOn by remember { mutableStateOf(false) }
    var blinkingENGStatusOn by remember { mutableStateOf(false) }
    // var blinkingCAUStatusOn by remember { mutableStateOf(false) }

    var colorBtStatus by remember { mutableStateOf(colorOff) }
    var colorENGStatus by remember { mutableStateOf(colorOff) }
    var colorCAUStatus by remember { mutableStateOf(colorOff) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val btColor = if (carStatus.bt_connection_status.value) {
                colorBtStatusOn
            } else {
                blinkingBtStatusOn = !blinkingBtStatusOn
                if (blinkingBtStatusOn) colorBtStatusOn else colorOff
            }
            if (colorBtStatus != btColor) colorBtStatus = btColor

            val warningOn = !carStatus.isIgnitionOn.value && run {
                blinkingENGStatusOn = !blinkingENGStatusOn
                blinkingENGStatusOn
            }
            val engColor = if (warningOn) colorENGStatusOn else colorOff
            val cauColor = if (warningOn) colorCAUStatusOn else colorOff
            if (colorENGStatus != engColor) colorENGStatus = engColor
            if (colorCAUStatus != cauColor) colorCAUStatus = cauColor
            delay(200)
        }
    }

    @Composable
    fun StatusBox(
        description: String,
        resouceId: Int,
        color: Color = Color(0xff666666),
        onClick: (() -> Unit)? = null
    ) {
        Box(
            modifier = Modifier
                .size(200.designedDp(), 200.designedDp())
                .then(
                    if (onClick != null) {
                        Modifier.pointerInput(onClick) {
                            detectTapGestures(onTap = { onClick() })
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.TopStart
        )
        {
            Image(
                contentDescription = description,
                painter = painterResource(id = resouceId),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(color)
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
                .padding(top = 0.designedDp()),
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
                StatusBox("Engine", R.drawable.icon_status_eng, colorENGStatus, onEngineClick)
                StatusBox("Caution", R.drawable.icon_status_cau, colorCAUStatus)
                StatusBox("BT", R.drawable.icon_status_bt, colorBtStatus)

            }
        }
    }
}

