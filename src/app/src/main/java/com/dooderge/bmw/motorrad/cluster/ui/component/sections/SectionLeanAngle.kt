package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterConfirmDialog
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.clusterValueUnitText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import kotlin.math.atan2
import kotlin.math.round

/**
 * Lean Angle — 롱프레스로 현재 자세를 0° 보정.
 * 시계방향 +, 반시계방향 −, 소수 1자리. 보정값은 SharedPreferences에 저장·복원.
 */
@Composable
fun SectionLeanAngle(
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	var rawDegrees by remember { mutableFloatStateOf(0f) }
	var zeroOffset by remember {
		mutableFloatStateOf(SettingPreference.getLeanAngleZeroOffset(context))
	}
	var calibrated by remember {
		mutableStateOf(SettingPreference.isLeanAngleCalibrated(context))
	}
	var pendingReset by remember { mutableStateOf(false) }

	DisposableEffect(Unit) {
		val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
		val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
			?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
		if (sensor == null) {
			onDispose { }
		} else {
			// 디스플레이 회전은 마운트 중 거의 불변 → 이벤트마다 WindowManager 조회하지 않음
			val rotation = displayRotation(context)
			val listener = object : SensorEventListener {
				override fun onSensorChanged(event: SensorEvent) {
					rawDegrees = rawLeanDegrees(
						gx = event.values[0],
						gy = event.values[1],
						gz = event.values[2],
						displayRotation = rotation
					)
				}

				override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
			}
			sensorManager.registerListener(
				listener,
				sensor,
				SensorManager.SENSOR_DELAY_UI
			)
			onDispose { sensorManager.unregisterListener(listener) }
		}
	}

	// 보정 전에도 상대 각을 보여주되, 첫 보정 전까지는 현재 raw를 0으로 취급
	val effectiveZero = if (calibrated) zeroOffset else rawDegrees
	val displayDegrees = wrap180(-(rawDegrees - effectiveZero))
	val rounded = round(displayDegrees * 10f) / 10f
	// -0.0 → 0.0 (부호 제거)
	val displayText = String.format("%.1f", if (rounded == 0f) 0f else rounded)

	SectionBox(
		modifier = modifier,
		alignment = alignment
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 20.designedDp())
				.pointerInput(Unit) {
					detectTapGestures(onLongPress = { pendingReset = true })
				},
			contentAlignment = Alignment.TopCenter
		) {
			ClusterText(
				text = clusterValueUnitText(displayText, " °"),
				fontSize = 90f,
				fontWeight = FontWeight.ExtraBold,
			)
		}
	}

	if (pendingReset) {
		ClusterConfirmDialog(
			title = "Lean Angle 초기화",
			message = "현재 스마트폰 자세를 0°로 설정할까요?\n이후 시계방향은 +, 반시계방향은 − 로 표시됩니다.",
			onConfirm = {
				zeroOffset = rawDegrees
				calibrated = true
				SettingPreference.setLeanAngleZeroOffset(context, rawDegrees)
				pendingReset = false
			},
			onDismiss = { pendingReset = false }
		)
	}
}

/**
 * 디바이스 중력 → lean(°).
 * 화면 관통축(Z) 둘레의 롤만 측정해 앞뒤 피치에는 값이 바뀌지 않는다.
 */
@Suppress("UNUSED_PARAMETER")
private fun rawLeanDegrees(gx: Float, gy: Float, gz: Float, displayRotation: Int): Float {
	// gz(화면 관통축)는 앞뒤 피치에 섞이므로 lean에 쓰지 않는다.
	val radians = when (displayRotation) {
		Surface.ROTATION_90 -> atan2((-gy).toDouble(), (-gx).toDouble())
		Surface.ROTATION_270 -> atan2(gy.toDouble(), (-gx).toDouble())
		Surface.ROTATION_180 -> atan2((-gx).toDouble(), (-gy).toDouble())
		else -> atan2(gx.toDouble(), (-gy).toDouble())
	}
	return Math.toDegrees(radians).toFloat()
}

/** 각도를 (-180, 180] 로 정규화. */
private fun wrap180(degrees: Float): Float {
	var d = degrees
	while (d > 180f) d -= 360f
	while (d < -180f) d += 360f
	return d
}

/** 현재 디스플레이 Surface 회전값. */
@Suppress("DEPRECATION")
private fun displayRotation(context: Context): Int {
	val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
	return wm.defaultDisplay.rotation
}
