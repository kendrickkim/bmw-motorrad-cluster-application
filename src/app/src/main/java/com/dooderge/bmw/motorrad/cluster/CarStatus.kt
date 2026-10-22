package com.dooderge.bmw.motorrad.cluster

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.experimental.and

data class CarStatus(
    var button_repeat_left: MutableState<Boolean> = mutableStateOf(false),
    var button_repeat_right: MutableState<Boolean> = mutableStateOf(false),
    var button_repeat_center: MutableState<Boolean> = mutableStateOf(false),
    var button_wheel_left: MutableState<Boolean> = mutableStateOf(false),
    var button_wheel_right: MutableState<Boolean> = mutableStateOf(false),
    var wheel_value: MutableState<Int> = mutableStateOf(-1),
    var gear: MutableState<Int> = mutableStateOf(0),
    var speed: MutableState<Int> = mutableStateOf(0),
    var rpm: MutableState<Int> = mutableStateOf(0),
    var voltage: MutableState<Int> = mutableStateOf(0),
    var isIgnitionOn: MutableState<Boolean> = mutableStateOf(false),
    var coolant_temperature: MutableState<Int> = mutableStateOf(0),
    var fuelRangeKm: MutableState<Int> = mutableStateOf(0),
    var odometerKm: MutableState<Int> = mutableStateOf(0),
    var horn: MutableState<Boolean> = mutableStateOf(false),
    var hazard: MutableState<Boolean> = mutableStateOf(false),
    var abs: MutableState<Boolean> = mutableStateOf(false),
    var esa: MutableState<Boolean> = mutableStateOf(false),
    var cruise: MutableState<Boolean> = mutableStateOf(false),
    var highBeam: MutableState<Boolean> = mutableStateOf(false),
    var trip: MutableState<Boolean> = mutableStateOf(false),
    var info: MutableState<Boolean> = mutableStateOf(false),
    var cruiseSet: MutableState<Boolean> = mutableStateOf(false),
    var cruiseReset: MutableState<Boolean> = mutableStateOf(false),
    var wheelDirection: MutableState<Int> = mutableStateOf(0),
    var ambientLight: MutableState<Int> = mutableStateOf(-1),
    var throttlePercent: MutableState<Int> = mutableStateOf(-1),
    var ascState: MutableState<Int> = mutableStateOf(LinFrame.ASC_UNKNOWN),
    var oilLow: MutableState<Boolean> = mutableStateOf(false),
    var averageSpeedKmh: MutableState<Int> = mutableStateOf(-1),
    var speedE9Kmh: MutableState<Int> = mutableStateOf(-1),
    var fuelFault: MutableState<Boolean> = mutableStateOf(false),
    var generalFault: MutableState<Int> = mutableStateOf(0),
    var bt_connection_status: MutableState<Boolean> = mutableStateOf(false),
    var linCapturePlaying: MutableState<Boolean> = mutableStateOf(false),
    var linCaptureLine: MutableState<Int> = mutableStateOf(0),
    var ignitionKnown: MutableState<Boolean> = mutableStateOf(false),
    var tripActive: MutableState<Boolean> = mutableStateOf(false),
    var tripStartedAtMillis: MutableState<Long> = mutableStateOf(0L),
    var tripBaseOdometerKm: MutableState<Int> = mutableStateOf(0),
    var tripBaseLatched: MutableState<Boolean> = mutableStateOf(false),
    /** TripB — 시동과 무관하게 유지되는 트립 기준 적산거리 */
    var tripBBaseOdometerKm: MutableState<Int> = mutableStateOf(0),
    var tripBBaseLatched: MutableState<Boolean> = mutableStateOf(false),
    /** TPMS — SettingPreference / TpmsService 로부터 MainActivity가 갱신 */
    var tpmsEnabled: MutableState<Boolean> = mutableStateOf(false),
    var tpmsFrontPressureRaw: MutableState<Int> = mutableStateOf(-1),
    var tpmsRearPressureRaw: MutableState<Int> = mutableStateOf(-1),
    var tpmsFrontTempC: MutableState<Int> = mutableStateOf(-1),
    var tpmsRearTempC: MutableState<Int> = mutableStateOf(-1),
    var tpmsUnitLabel: MutableState<String> = mutableStateOf("Bar"),
    var tpmsConnection: MutableState<String> = mutableStateOf("DISCONNECTED"),
    var tpmsStatusMessage: MutableState<String> = mutableStateOf("")
) {
    fun setButtons(value: Byte) {
        button_repeat_left.value = (value and 0x01) != 0.toByte()
        button_repeat_right.value = (value and 0x02) != 0.toByte()
        button_repeat_center.value = (value and 0x04) != 0.toByte()
        button_wheel_left.value = (value and 0x08) != 0.toByte()
        button_wheel_right.value = (value and 0x10) != 0.toByte()
    }

    fun isSame(other: CarStatus): Boolean {
        return button_repeat_left.value == other.button_repeat_left.value &&
                button_repeat_right.value == other.button_repeat_right.value &&
                button_repeat_center.value == other.button_repeat_center.value &&
                button_wheel_left.value == other.button_wheel_left.value &&
                button_wheel_right.value == other.button_wheel_right.value &&
                gear.value == other.gear.value &&
                speed.value == other.speed.value &&
                rpm.value == other.rpm.value &&
                voltage.value == other.voltage.value &&
                isIgnitionOn.value == other.isIgnitionOn.value &&
                coolant_temperature.value == other.coolant_temperature.value &&
                fuelRangeKm.value == other.fuelRangeKm.value &&
                odometerKm.value == other.odometerKm.value &&
                horn.value == other.horn.value &&
                hazard.value == other.hazard.value &&
                abs.value == other.abs.value &&
                esa.value == other.esa.value &&
                cruise.value == other.cruise.value &&
                highBeam.value == other.highBeam.value &&
                trip.value == other.trip.value &&
                info.value == other.info.value &&
                cruiseSet.value == other.cruiseSet.value &&
                cruiseReset.value == other.cruiseReset.value &&
                wheelDirection.value == other.wheelDirection.value &&
                ambientLight.value == other.ambientLight.value &&
                throttlePercent.value == other.throttlePercent.value &&
                ascState.value == other.ascState.value &&
                oilLow.value == other.oilLow.value &&
                averageSpeedKmh.value == other.averageSpeedKmh.value &&
                speedE9Kmh.value == other.speedE9Kmh.value &&
                fuelFault.value == other.fuelFault.value &&
                generalFault.value == other.generalFault.value &&
                ignitionKnown.value == other.ignitionKnown.value
    }

    fun copyFrom(other: CarStatus) {
        if (button_repeat_left.value != other.button_repeat_left.value)
            button_repeat_left.value = other.button_repeat_left.value

        if (button_repeat_right.value != other.button_repeat_right.value)
            button_repeat_right.value = other.button_repeat_right.value

        if (button_repeat_center.value != other.button_repeat_center.value)
            button_repeat_center.value = other.button_repeat_center.value

        if (button_wheel_left.value != other.button_wheel_left.value)
            button_wheel_left.value = other.button_wheel_left.value

        if (button_wheel_right.value != other.button_wheel_right.value)
            button_wheel_right.value = other.button_wheel_right.value

        if (wheel_value.value != other.wheel_value.value)
            wheel_value.value = other.wheel_value.value

        if (gear.value != other.gear.value)
            gear.value = other.gear.value

        if (speed.value != other.speed.value)
            speed.value = other.speed.value

        if (rpm.value != other.rpm.value)
            rpm.value = other.rpm.value

        if (voltage.value != other.voltage.value)
            voltage.value = other.voltage.value

        if (isIgnitionOn.value != other.isIgnitionOn.value)
            isIgnitionOn.value = other.isIgnitionOn.value

        if (coolant_temperature.value != other.coolant_temperature.value)
            coolant_temperature.value = other.coolant_temperature.value

        if (fuelRangeKm.value != other.fuelRangeKm.value)
            fuelRangeKm.value = other.fuelRangeKm.value

        if (odometerKm.value != other.odometerKm.value)
            odometerKm.value = other.odometerKm.value

        if (horn.value != other.horn.value) horn.value = other.horn.value
        if (hazard.value != other.hazard.value) hazard.value = other.hazard.value
        if (abs.value != other.abs.value) abs.value = other.abs.value
        if (esa.value != other.esa.value) esa.value = other.esa.value
        if (cruise.value != other.cruise.value) cruise.value = other.cruise.value
        if (highBeam.value != other.highBeam.value) highBeam.value = other.highBeam.value
        if (trip.value != other.trip.value) trip.value = other.trip.value
        if (info.value != other.info.value) info.value = other.info.value
        if (cruiseSet.value != other.cruiseSet.value) cruiseSet.value = other.cruiseSet.value
        if (cruiseReset.value != other.cruiseReset.value) cruiseReset.value = other.cruiseReset.value
        if (wheelDirection.value != other.wheelDirection.value)
            wheelDirection.value = other.wheelDirection.value
        if (ambientLight.value != other.ambientLight.value)
            ambientLight.value = other.ambientLight.value
        if (throttlePercent.value != other.throttlePercent.value)
            throttlePercent.value = other.throttlePercent.value
        if (ascState.value != other.ascState.value) ascState.value = other.ascState.value
        if (oilLow.value != other.oilLow.value) oilLow.value = other.oilLow.value
        if (averageSpeedKmh.value != other.averageSpeedKmh.value)
            averageSpeedKmh.value = other.averageSpeedKmh.value
        if (speedE9Kmh.value != other.speedE9Kmh.value) speedE9Kmh.value = other.speedE9Kmh.value
        if (fuelFault.value != other.fuelFault.value) fuelFault.value = other.fuelFault.value
        if (generalFault.value != other.generalFault.value)
            generalFault.value = other.generalFault.value
        if (ignitionKnown.value != other.ignitionKnown.value)
            ignitionKnown.value = other.ignitionKnown.value
    }

    fun toJson(): String {
        return """
            {
                "button_repeat_left": ${button_repeat_left.value},
                "button_repeat_right": ${button_repeat_right.value},
                "button_repeat_center": ${button_repeat_center.value},
                "button_wheel_left": ${button_wheel_left.value},
                "button_wheel_right": ${button_wheel_right.value},
                "wheel_value": ${wheel_value.value.toInt()},
                "gear": ${gear.value},
                "speed": ${speed.value},
                "rpm": ${rpm.value},
                "voltage": ${voltage.value},
                "isIgnitionOn": ${isIgnitionOn.value},
                "coolant_temperature": ${coolant_temperature.value},
                "fuelRangeKm": ${fuelRangeKm.value},
                "odometerKm": ${odometerKm.value},
                "horn": ${horn.value},
                "hazard": ${hazard.value},
                "abs": ${abs.value},
                "esa": ${esa.value},
                "cruise": ${cruise.value},
                "highBeam": ${highBeam.value},
                "trip": ${trip.value},
                "info": ${info.value},
                "cruiseSet": ${cruiseSet.value},
                "cruiseReset": ${cruiseReset.value},
                "wheelDirection": ${wheelDirection.value},
                "ambientLight": ${ambientLight.value},
                "throttlePercent": ${throttlePercent.value},
                "ascState": ${ascState.value},
                "oilLow": ${oilLow.value},
                "averageSpeedKmh": ${averageSpeedKmh.value},
                "speedE9Kmh": ${speedE9Kmh.value},
                "fuelFault": ${fuelFault.value},
                "generalFault": ${generalFault.value},
                "ignitionKnown": ${ignitionKnown.value}
            }
        """.trimIndent()
    }

    fun fromJson(json: String) {
        val regex = """"(\w+)":\s*(true|false|null|-?\d+)""".toRegex()
        regex.findAll(json).forEach { matchResult ->
            when (matchResult.groupValues[1]) {
                "button_repeat_left" -> button_repeat_left.value =
                    matchResult.groupValues[2].toBoolean()

                "button_repeat_right" -> button_repeat_right.value =
                    matchResult.groupValues[2].toBoolean()

                "button_repeat_center" -> button_repeat_center.value =
                    matchResult.groupValues[2].toBoolean()

                "button_wheel_left" -> button_wheel_left.value =
                    matchResult.groupValues[2].toBoolean()

                "button_wheel_right" -> button_wheel_right.value =
                    matchResult.groupValues[2].toBoolean()

                "wheel_value" -> wheel_value.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0x00

                "gear" -> gear.value = matchResult.groupValues[2].toIntOrNull() ?: 0
                "speed" -> speed.value = matchResult.groupValues[2].toIntOrNull() ?: 0
                "rpm" -> rpm.value = matchResult.groupValues[2].toIntOrNull() ?: 0
                "voltage" -> voltage.value = matchResult.groupValues[2].toIntOrNull() ?: 0
                "isIgnitionOn" -> isIgnitionOn.value = matchResult.groupValues[2].toBoolean()
                "coolant_temperature" -> coolant_temperature.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0

                "fuelRangeKm" -> fuelRangeKm.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0

                "odometerKm" -> odometerKm.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0

                "horn" -> horn.value = matchResult.groupValues[2].toBoolean()
                "hazard" -> hazard.value = matchResult.groupValues[2].toBoolean()
                "abs" -> abs.value = matchResult.groupValues[2].toBoolean()
                "esa" -> esa.value = matchResult.groupValues[2].toBoolean()
                "cruise" -> cruise.value = matchResult.groupValues[2].toBoolean()
                "highBeam" -> highBeam.value = matchResult.groupValues[2].toBoolean()
                "trip" -> trip.value = matchResult.groupValues[2].toBoolean()
                "info" -> info.value = matchResult.groupValues[2].toBoolean()
                "cruiseSet" -> cruiseSet.value = matchResult.groupValues[2].toBoolean()
                "cruiseReset" -> cruiseReset.value = matchResult.groupValues[2].toBoolean()
                "wheelDirection" -> wheelDirection.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0
                "ambientLight" -> ambientLight.value =
                    matchResult.groupValues[2].toIntOrNull() ?: -1
                "throttlePercent" -> throttlePercent.value =
                    matchResult.groupValues[2].toIntOrNull() ?: -1
                "ascState" -> ascState.value =
                    matchResult.groupValues[2].toIntOrNull() ?: LinFrame.ASC_UNKNOWN
                "oilLow" -> oilLow.value = matchResult.groupValues[2].toBoolean()
                "averageSpeedKmh" -> averageSpeedKmh.value =
                    matchResult.groupValues[2].toIntOrNull() ?: -1
                "speedE9Kmh" -> speedE9Kmh.value =
                    matchResult.groupValues[2].toIntOrNull() ?: -1
                "fuelFault" -> fuelFault.value = matchResult.groupValues[2].toBoolean()
                "generalFault" -> generalFault.value =
                    matchResult.groupValues[2].toIntOrNull() ?: 0
                "ignitionKnown" -> ignitionKnown.value = matchResult.groupValues[2].toBoolean()
            }
        }
    }

    fun setBtStatus(status: Boolean) {
        if (bt_connection_status.value != status)
            bt_connection_status.value = status
    }

}