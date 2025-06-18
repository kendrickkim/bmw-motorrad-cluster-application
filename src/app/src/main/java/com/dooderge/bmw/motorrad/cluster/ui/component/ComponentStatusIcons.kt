package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.Image
import com.dooderge.bmw.motorrad.cluster.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.room.util.wrapMappedColumns
import com.dooderge.bmw.motorrad.cluster.CarStatus


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentStatusIcons(
    carStatus: CarStatus
) {
    var icon_resources = listOf<Int>(
        R.drawable.icon_status_egn,
        R.drawable.icon_status_lin,
        R.drawable.icon_status_eng,
        R.drawable.icon_status_warning,
        R.drawable.icon_status_bt
    )

    @Composable
    fun Dp.dpToPx(): Float = with(LocalDensity.current) { this@dpToPx.toPx() }

    @Composable
    fun Int.pxToSp() = with(LocalDensity.current) { this@pxToSp.toSp() }

    Column {
        FlowRow(
            modifier = Modifier.width(170.dp),
        )
        {
            icon_resources.forEachIndexed({ index, it ->
                Image(
                    modifier = Modifier
                        .height(25.dp)
                        .padding(end = 2.dp, bottom = 2.dp),
                    painter = painterResource(id = it),
                    contentDescription = "status_icon" + index.toString(),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(Color(0xFF333333))
                )
            })
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        )
        {
            var color = Color(0xffAA0000)

            Image(
                modifier = Modifier.height(40.dp),
                painter = painterResource(id = R.drawable.icon_battery),
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(color),
                contentDescription = "iconbattery"
            )
            Spacer(modifier = Modifier.width(10.dp))

            val voltage = carStatus.voltage.value.toFloat()
            var str_voltage = String.format("%.1f v", voltage / 10.0)

            Text(
                str_voltage,
                fontSize = 20.dp.dpToPx().toInt().pxToSp(),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        )
        {
            var color = Color(0xffAA0000)
            Image(
                modifier = Modifier.height(40.dp),
                painter = painterResource(id = R.drawable.icon_temperature),
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(color),
                contentDescription = "iconbattery"
            )
            Spacer(modifier = Modifier.width(5.dp))

            val temperature = carStatus.coolant_temperature.value.toFloat()
            var str_temperature = String.format("%.1f \u2103", temperature / 10.0)

            Text(
                str_temperature,
                fontSize = 20.dp.dpToPx().toInt().pxToSp(),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }


}