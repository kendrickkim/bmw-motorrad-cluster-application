package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterConfirmDialog
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.clusterValueUnitText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSectionTitleColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterValueColor

/**
 * 총 주행거리 + Trip A/B. 롱프레스로 해당 Trip 기준점 초기화.
 */
@Composable
fun SectionDistance(
	carStatus: CarStatus,
	alignment: Alignment = Alignment.TopStart,
	modifier: Modifier = Modifier
) {
	val textColor = ClusterValueColor
	val distanceFontSize = 85f
	val tripLabelSize = 50f
	val tripValueSize = 45f
	val context = LocalContext.current
	val liveKm = carStatus.odometerKm.value
	// val liveKm = 999999
	val displayKm = liveKm.takeIf { it > 0 } ?: SettingPreference.getLastOdometerKm(context)
	// Compose가 base latch 변경을 감지하도록 읽기
	carStatus.tripABaseOdometerKm.value
	carStatus.tripABaseLatched.value
	carStatus.tripBBaseOdometerKm.value
	carStatus.tripBBaseLatched.value
	val tripAKm = SettingPreference.tripComputerKm(context, carStatus, 'A')
	val tripBKm = SettingPreference.tripComputerKm(context, carStatus, 'B')
	// val tripAKm = 50000
	// val tripBKm = 99999
	var pendingResetTrip by remember { mutableStateOf<Char?>(null) }

	// MainScreen: Distance 제목 top=162, SectionDistance top=232 → Trip을 제목과 맞춤
	val tripTitleOffsetY = (-55).designedDp()

	SectionBox(
		modifier = modifier,
		alignment = alignment
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			Box(
				modifier = Modifier
					.align(Alignment.TopStart)
					.padding(top = 20.designedDp())
			) {
				ClusterText(
					text = clusterValueUnitText("$displayKm", " Km", valueColor = textColor),
					fontSize = distanceFontSize,
					fontWeight = FontWeight.ExtraBold
				)
			}
			Column(
				modifier = Modifier
					.align(Alignment.TopStart)
					.offset(y = tripTitleOffsetY)
					.padding(end = 12.designedDp(), start = 410.designedDp())
					.width(400.designedDp())
					.height(500.designedDp()),
				verticalArrangement = Arrangement.spacedBy(8.designedDp()),
				horizontalAlignment = Alignment.Start
			) {
				TripComputerRow(
					label = "TRIP A",
					km = tripAKm,
					labelSize = tripLabelSize,
					valueSize = tripValueSize,
					titleColor = ClusterSectionTitleColor,
					color = textColor,
					onLongPress = { pendingResetTrip = 'A' }
				)
				Spacer(modifier = Modifier.height(10.designedDp()))
				TripComputerRow(
					label = "TRIP B",
					km = tripBKm,
					labelSize = tripLabelSize,
					valueSize = tripValueSize,
					titleColor = ClusterSectionTitleColor,
					color = textColor,
					onLongPress = { pendingResetTrip = 'B' }
				)
			}
		}
	}

	pendingResetTrip?.let { trip ->
		ClusterConfirmDialog(
			title = "Trip$trip 초기화",
			message = "Trip$trip 을(를) 초기화할까요?\n현재 총 주행거리($displayKm Km)를 기준으로 다시 셉니다.",
			onConfirm = {
				SettingPreference.resetTripComputer(context, carStatus, trip)
				pendingResetTrip = null
			},
			onDismiss = { pendingResetTrip = null }
		)
	}
}

/** Trip A/B 한 줄(라벨+km). 롱프레스로 리셋 요청. */
@Composable
private fun TripComputerRow(
	label: String,
	km: Int,
	labelSize: Float,
	valueSize: Float,
	titleColor: Color,
	color: Color,
	onLongPress: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.pointerInput(label) {
				detectTapGestures(onLongPress = { onLongPress() })
			},
		horizontalAlignment = Alignment.Start
	) {
		ClusterText(
			text = label,
			fontSize = labelSize,
			fontWeight = FontWeight.ExtraBold,
			color = titleColor,
			softWrap = false,
			maxLines = 1
		)
		ClusterText(
			text = clusterValueUnitText("  $km", " Km", valueColor = color),
			fontSize = valueSize,
			fontWeight = FontWeight.Bold,
			softWrap = false,
			maxLines = 1
		)
	}
}
