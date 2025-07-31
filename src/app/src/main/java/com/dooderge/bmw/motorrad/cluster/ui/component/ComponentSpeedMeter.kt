package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.R


fun Modifier.textBrush(brush: Brush) = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        }
    }


@Composable
fun ComponentSpeedMeter(speed: Int = 0) {
    Row(
        modifier = Modifier
            .width(400.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    )

    {
        var str_speed = speed.toString();
        if (speed < 10) {
            str_speed = "  $speed";
        } else if (speed < 100) {
            str_speed = " $speed";
        }

//        str_speed = "888"

        Box(
            modifier = Modifier
                .height(130.dp)
                .offset(
                    y = -30.dp
                )

        )
        {
            Text(
                text = "${str_speed}",
                textAlign = TextAlign.Right,
                style = TextStyle.Default.copy(
                    fontSize = 110.sp,
                    fontFamily = FontFamily(Font(R.font.robot_mono_bold_italic)),
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                modifier = Modifier.textBrush(
                    Brush.verticalGradient(
                        listOf(
//                            Color.Red,
//                            Color.White,
//                            Color.Yellow
                            Color.Gray,
                            Color.White,
                            Color.Gray
                        )
                    )
                )

            )
        }
        Text(
            text = " km/h",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )


    }
}
