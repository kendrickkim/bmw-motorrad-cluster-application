package com.dooderge.bmw.motorrad.cluster.ui.component

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
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
            .width(350.dp),
//            .border(BorderStroke(1.dp, Color.White)),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    )
    {
        Box(
            modifier = Modifier.width(250.dp),
        )
        {
            Text(
                text = "000",
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                fontFamily = FontFamily(Font(R.font.f_e1234_i)),
                textAlign = TextAlign.Right,
                color = Color(0xFF333333),

                )

            var str_speed = speed.toString();
            if (speed < 10) {
                str_speed = "  $speed";
            } else if (speed < 100) {
                str_speed = " $speed";
            }

//            str_speed = "888"

//            Log.d("ComponentSpeedMeter", "Speed: $str_speed")

            Text(
                text = "${str_speed}",
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                fontFamily = FontFamily(Font(R.font.f_e1234_i)),
                textAlign = TextAlign.Right,
//                style = TextStyle.Default.copy(
//                    drawStyle = Stroke(width = 5f)
//                ),
                modifier = Modifier.textBrush(
                    Brush.verticalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow
                        )
                    )
                )

            )


        }
        Text(
            text = " km/h",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,

//            fontFamily = FontFamily(Font(R.font.wdxl))
        )
    }
}
