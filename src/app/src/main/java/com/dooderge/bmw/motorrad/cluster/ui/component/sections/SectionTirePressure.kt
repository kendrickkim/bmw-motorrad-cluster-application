package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsPressureUnit
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsProtocol
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.clusterValueUnitText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterAccentColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterValueColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterValueMetaColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ColorAlarm = Color(0xFFFF9800)
private val LastReadFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm")

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
    val zone = remember { ZoneId.systemDefault() }

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
        clusterValueUnitText(SIM_PRESSURE_VALUE, SIM_PRESSURE_UNIT)
    } else {
        formatPressureAnnotated(carStatus.tpmsFrontPressureRaw.value, unit)
    }
    val rearP = if (simulating) {
        clusterValueUnitText(SIM_PRESSURE_VALUE, SIM_PRESSURE_UNIT)
    } else {
        formatPressureAnnotated(carStatus.tpmsRearPressureRaw.value, unit)
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
    val lastRead = if (simulating) {
        "--"
    } else {
        formatLastReadAt(carStatus.tpmsLastReadAtMillis.value, zone)
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
                lastRead = lastRead,
                alarm = frontAlarm,
                modifier = Modifier.weight(1f)
            )
            WheelPressureColumn(
                pressure = rearP,
                meta = rearMeta,
                lastRead = lastRead,
                alarm = rearAlarm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelPressureColumn(
    pressure: AnnotatedString,
    meta: AnnotatedString,
    lastRead: String,
    alarm: Boolean,
    modifier: Modifier = Modifier
) {
    val pressureColor = if (alarm) ColorAlarm else ClusterValueColor
    val metaColor = if (alarm) ColorAlarm else ClusterValueMetaColor
    // 알람 시 수치 색만 덮어쓰고, 단위는 ClusterAccentColor 유지
    val pressureShown = if (alarm) recolorValuesKeepUnits(pressure, pressureColor) else pressure
    val metaShown = if (alarm) recolorValuesKeepUnits(meta, metaColor) else meta
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        ClusterText(
            text = pressureShown,
            fontSize = 78f * 0.8f,
            fontWeight = FontWeight.ExtraBold
        )
        ClusterText(
            text = metaShown,
            fontSize = 54f * 0.8f,
            fontWeight = FontWeight.Bold
        )
        ClusterText(
            text = lastRead,
            fontSize = 40f * 0.8f,
            fontWeight = FontWeight.Bold,
            color = metaColor
        )
    }
}

private const val SIM_PRESSURE_VALUE = "100.0"
private const val SIM_PRESSURE_UNIT = " PSI"
private val SIM_META: AnnotatedString = formatTempBatteryAnnotated("99.8", "100")

private fun formatPressureAnnotated(cents: Int, unit: TpmsPressureUnit): AnnotatedString {
    if (cents < 0) {
        return clusterValueUnitText("--", " ${unit.label}")
    }
    val kpa = TpmsProtocol.centsToKpa(cents)
    val value = when (unit) {
        TpmsPressureUnit.BAR -> String.format("%.2f", kpa / 100f)
        TpmsPressureUnit.KPA -> String.format("%.1f", kpa)
        TpmsPressureUnit.PSI -> String.format("%.1f", kpa / TpmsProtocol.PSI_TO_KPA)
    }
    return clusterValueUnitText(value, " ${unit.label}")
}

private fun formatTempBattery(celsius: Int, batteryPct: Int): AnnotatedString {
    val temp = if (celsius < 0) "--" else "$celsius"
    val bat = if (batteryPct < 0) "--" else "$batteryPct"
    return formatTempBatteryAnnotated(temp, bat)
}

private fun formatTempBatteryAnnotated(
    tempValue: String,
    batteryValue: String,
    valueColor: Color = ClusterValueMetaColor,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = valueColor)) { append(tempValue) }
    withStyle(SpanStyle(color = ClusterAccentColor)) { append(" °C") }
    withStyle(SpanStyle(color = valueColor)) { append(" / ") }
    withStyle(SpanStyle(color = valueColor)) { append(batteryValue) }
    withStyle(SpanStyle(color = ClusterAccentColor)) { append("%") }
}

/** 알람 색으로 value span만 바꾸고, 단위(ClusterAccentColor) span은 유지 */
private fun recolorValuesKeepUnits(source: AnnotatedString, valueColor: Color): AnnotatedString {
    if (source.spanStyles.isEmpty()) {
        return AnnotatedString(source.text, listOf(AnnotatedString.Range(SpanStyle(color = valueColor), 0, source.length)))
    }
    return buildAnnotatedString {
        append(source.text)
        source.spanStyles.forEach { range ->
            val style = range.item
            val keepUnit = style.color == ClusterAccentColor
            addStyle(
                if (keepUnit) style else style.copy(color = valueColor),
                range.start,
                range.end
            )
        }
    }
}

private fun formatLastReadAt(epochMs: Long, zone: ZoneId): String {
    if (epochMs <= 0L) return "--"
    return Instant.ofEpochMilli(epochMs).atZone(zone).format(LastReadFormatter)
}
