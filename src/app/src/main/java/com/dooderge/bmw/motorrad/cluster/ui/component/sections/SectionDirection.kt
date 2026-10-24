package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

/**
 * DO NOT DELETE — Direction (나침반/헤딩) 섹션.
 *
 * 클러스터 레이아웃에서 해당 슬롯은 일시적으로 Trip으로 교체됨.
 * MainScreen.kt 에서도 호출부가 주석 처리되어 있음. 다른 에이전트 작업 시
 * 이 파일·함수를 삭제하지 말 것. 재사용 예정.
 */
@Composable
fun SectionDirection(
    carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bearingDegrees = remember { mutableFloatStateOf(0f) }
    val linTestPlaying = carStatus.linCapturePlaying.value
    var testBearing by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(linTestPlaying) {
        if (!linTestPlaying) return@LaunchedEffect
        val startedAt = SystemClock.elapsedRealtime()
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            testBearing = (elapsed % 5000L) / 5000f * 360f
            withFrameNanos { }
        }
    }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { location ->
            if (location.hasBearing() && location.speed >= 1f) {
                bearingDegrees.floatValue = location.bearing
            }
        }
        if (hasLocationPermission(context)) {
            val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                null
            }
            if (provider != null) {
                manager.requestLocationUpdates(provider, 200L, 0f, listener)
            }
        }
        onDispose { manager.removeUpdates(listener) }
    }

    SectionBox(
        modifier = modifier,
        alignment = alignment
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                ClusterText("N", 30f, FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                ClusterText("W", 30f, FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ClusterText("E", 30f, FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                ClusterText("S", 30f, FontWeight.ExtraBold)
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                HeadingArrow(
                    bearingDegrees = if (linTestPlaying) testBearing else bearingDegrees.floatValue
                )
            }
        }
    }
}

@Composable
private fun HeadingArrow(bearingDegrees: Float) {
    Canvas(
        modifier = Modifier
            .size(100.designedDp())
            .rotate(bearingDegrees)
    ) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width * 0.82f, size.height)
            lineTo(size.width / 2f, size.height * 0.68f)
            lineTo(size.width * 0.18f, size.height)
            close()
        }
        drawPath(path, Color(0xffaaaaaa), style = Fill)
        drawLine(
            color = Color(0xff666666),
            start = Offset(size.width / 2f, size.height * 0.2f),
            end = Offset(size.width / 2f, size.height * 0.68f),
            strokeWidth = size.width * 0.06f
        )
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
