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

@Composable
fun SectionTyrePressure(
    carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier
) {
    val unit = TpmsPressureUnit.fromLabel(carStatus.tpmsUnitLabel.value)
    val frontP = TpmsProtocol.formatPressure(carStatus.tpmsFrontPressureRaw.value, unit)
    val rearP = TpmsProtocol.formatPressure(carStatus.tpmsRearPressureRaw.value, unit)
    val frontT = formatTemp(carStatus.tpmsFrontTempC.value)
    val rearT = formatTemp(carStatus.tpmsRearTempC.value)

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
                temperature = frontT,
                modifier = Modifier.weight(1f)
            )
            WheelPressureColumn(
                pressure = rearP,
                temperature = rearT,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelPressureColumn(
    pressure: String,
    temperature: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        ClusterText(
            text = pressure,
            fontSize = 78f,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xffaaaaaa)
        )
        ClusterText(
            text = temperature,
            fontSize = 54f,
            fontWeight = FontWeight.Bold,
            color = Color(0xff888888)
        )
    }
}

private fun formatTemp(celsius: Int): String =
    if (celsius < 0) "-- °C" else "$celsius °C"
