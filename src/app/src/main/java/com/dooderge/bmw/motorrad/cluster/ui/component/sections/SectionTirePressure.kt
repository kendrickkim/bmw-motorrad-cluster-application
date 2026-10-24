package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsPressureUnit
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsProtocol
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

private val ColorNormalPressure = Color(0xffaaaaaa)
private val ColorNormalMeta = Color(0xff888888)
private val ColorAlarm = Color(0xFFFF9800)

@Composable
fun SectionTirePressure(
    carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier
) {
    val simulating = carStatus.linCapturePlaying.value
    val unit = TpmsPressureUnit.fromLabel(carStatus.tpmsUnitLabel.value)
    val minKpa = carStatus.tpmsPressureMinKpa.value
    val maxKpa = carStatus.tpmsPressureMaxKpa.value

    val frontAlarm = !simulating && TpmsProtocol.isPressureOutOfRange(
        carStatus.tpmsFrontPressureRaw.value,
        minKpa,
        maxKpa
    )
    val rearAlarm = !simulating && TpmsProtocol.isPressureOutOfRange(
        carStatus.tpmsRearPressureRaw.value,
        minKpa,
        maxKpa
    )

    val frontP = if (simulating) {
        SIM_PRESSURE
    } else {
        TpmsProtocol.formatPressure(carStatus.tpmsFrontPressureRaw.value, unit)
    }
    val rearP = if (simulating) {
        SIM_PRESSURE
    } else {
        TpmsProtocol.formatPressure(carStatus.tpmsRearPressureRaw.value, unit)
    }
    val frontMeta = if (simulating) {
        SIM_META
    } else {
        formatTempBattery(
            carStatus.tpmsFrontTempC.value,
            carStatus.tpmsFrontBatteryPct.value
        )
    }
    val rearMeta = if (simulating) {
        SIM_META
    } else {
        formatTempBattery(
            carStatus.tpmsRearTempC.value,
            carStatus.tpmsRearBatteryPct.value
        )
    }

    SectionBox(
        modifier = modifier,
        alignment = alignment
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.designedDp(), end = 8.designedDp()),
            horizontalArrangement = Arrangement.spacedBy(24.designedDp()),
            verticalAlignment = Alignment.Top
        ) {
            WheelPressureColumn(
                pressure = frontP,
                meta = frontMeta,
                alarm = frontAlarm,
                modifier = Modifier.weight(1f)
            )
            WheelPressureColumn(
                pressure = rearP,
                meta = rearMeta,
                alarm = rearAlarm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelPressureColumn(
    pressure: String,
    meta: String,
    alarm: Boolean,
    modifier: Modifier = Modifier
) {
    val pressureColor = if (alarm) ColorAlarm else ColorNormalPressure
    val metaColor = if (alarm) ColorAlarm else ColorNormalMeta
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        ClusterText(
            text = pressure,
            fontSize = 78f * 0.8f,
            fontWeight = FontWeight.ExtraBold,
            color = pressureColor
        )
        ClusterText(
            text = meta,
            fontSize = 54f * 0.8f,
            fontWeight = FontWeight.Bold,
            color = metaColor
        )
    }
}

private const val SIM_PRESSURE = "100.0 PSI"
private const val SIM_META = "99.8 °C / 100%"

private fun formatTempBattery(celsius: Int, batteryPct: Int): String {
    val temp = if (celsius < 0) "-- °C" else "$celsius °C"
    val bat = if (batteryPct < 0) "--%" else "$batteryPct%"
    return "$temp / $bat"
}
