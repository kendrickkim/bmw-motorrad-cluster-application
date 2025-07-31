package com.dooderge.bmw.motorrad.cluster.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Dp.dpToPx(): Float = with(LocalDensity.current) { this@dpToPx.toPx() }

@Composable
fun Int.pxToSp() = with(LocalDensity.current) { this@pxToSp.toSp() }

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ComponentCluster(
    context: Context,
    carStatus: CarStatus
) {
    RpmGuage(context = context, carStatus = carStatus)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        ComponentSpeedMeter(carStatus.speed.value)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    )
    {
        ComponentStatusIcons(carStatus)
    }
}

fun imageToPainter(image: Bitmap): Painter {

    // Convert the Bitmap to an ImageBitmap
    image.asImageBitmap()

    // conver to Painter
    return BitmapPainter(image.asImageBitmap())
}

fun cropImage(percentage: Float, resourceId: Int, context: Context): Bitmap {
    // get size of image
    var p = percentage;
    val image = BitmapFactory.decodeResource(context.resources, resourceId)
    val width = image.width
    val height = image.height
    var croppedWidth = (width * p).toInt()

    if (croppedWidth > width) croppedWidth = width;
    if (croppedWidth <= 0) croppedWidth = 1

    val canvasImage = createBitmap(width, height);
    var cropImage = Bitmap.createBitmap(image, 0, 0, croppedWidth, height)
    var canvas = android.graphics.Canvas(canvasImage)
    canvas.drawBitmap(cropImage, 0f, 0f, null)

    // canvas to Bitmap


    return canvasImage
}


@Composable
fun RpmGuage(
    context: Context,
    carStatus: CarStatus
) {
    val max_rpm = 14000.0f;


    data class RPMValue(
        var value: MutableState<Int> = mutableStateOf(0),
        var offsetX: MutableState<Float> = mutableStateOf(0f),
        var offsetY: MutableState<Float> = mutableStateOf(0f),
        var color: Color = Color.White,
    ) {
        fun set(value: Int, offsetY: Float, color: Color = Color.White): RPMValue {
            this.value.value = value
            this.offsetY.value = offsetY
            this.color = color;

            return this
        }

    }

    val rpmValues = remember {
        mutableStateOf<List<RPMValue>>(emptyList())
    }

    LaunchedEffect(Unit) {
        rpmValues.value = listOf<RPMValue>(
            RPMValue().set(0, 0.16f, Color(0xff6ae2e3)),
            RPMValue().set(2, 0.18f, Color(0xff78cbec)),
            RPMValue().set(4, 0.31f, Color(0xff84b3f4)),
            RPMValue().set(6, 0.48f, Color(0xff828be0)),
            RPMValue().set(8, 0.59f, Color(0xffae7ccf)),
            RPMValue().set(10, 0.64f, Color(0xffd85880)),
            RPMValue().set(12, 0.65f, Color(0xffff3838)),

            )
    }


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    )
    {
        val guageSize_width = remember {
            mutableStateOf(0)
        }
        val guageSize_height = remember {
            mutableStateOf(0)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp)
                .onSizeChanged(
                    onSizeChanged = {
                        Log.d("onSizeChanged", "width : ${it.width}, height : ${it.height}")
                        if (it.height > 0 && guageSize_height.value != it.height)
                            guageSize_height.value = it.height
                        if (it.width > 0 && guageSize_width.value != it.width)
                            guageSize_width.value = it.width
                    }
                )
                .onSizeChanged(
                    onSizeChanged = {
                    }
                )
                .background(Color.Black),
            contentAlignment = Alignment.BottomStart
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxSize()
//                    .border(BorderStroke(2.dp, Color.Red)),

            )
            {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.BottomCenter,
                    painter = painterResource(
                        id = R.drawable.rpm_guage_back
                    ),
                    contentDescription = "background",
//                    contentScale = ContentScale.Inside,

                )

            }

            val rpmGuagePercentage = carStatus.rpm.value.toFloat() / 12000.0f

            if (rpmGuagePercentage > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.BottomCenter,
                        painter = imageToPainter(
                            cropImage(
                                rpmGuagePercentage,
                                R.drawable.rpm_guage_front_2,
                                context
                            )
                        ),
                        contentDescription = "background"
                    )
                }
            }

        }

        var rpmColor: Color = Color.White
        Box()
        {
            rpmValues.value.forEachIndexed { index, r ->
                val rpmValue = r
                var fontBig = false;
                if (carStatus.rpm.value.toFloat() / 1000.0 >= r.value.value) {
                    rpmColor = r.color;
                    fontBig = true;
                }

                var offsetX =
                    ((rpmValue.value.value / 12.0) * guageSize_width.value.toFloat()).toInt()
                        .pxToDp()
                var offsetY =
                    (rpmValue.offsetY.value * guageSize_height.value.toFloat()).toInt().pxToDp()

                Box(
                    modifier = Modifier
                        .offset(
                            offsetX - 40.dp,
                            offsetY * -1f,
                        )
                        .height(60.dp)
                        .width(40.dp),

                    contentAlignment = Alignment.BottomEnd
                )
                {
                    Text(
                        "${rpmValue.value.value}",
                        color = rpmValue.color,
                        fontSize = if (fontBig) 40.sp else 25.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.wdxl))
                    )
                }
            }
        }



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .offset(0.dp, guageSize_height.value.pxToDp() * -0.20f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        )
        {
            Row(
                modifier = Modifier.width(250.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            )
            {
                Text(
                    "${carStatus.rpm.value}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.f_e1234_i)),
                    color = rpmColor
                )
            }
            Text(
                text = " rpm",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

    }


}