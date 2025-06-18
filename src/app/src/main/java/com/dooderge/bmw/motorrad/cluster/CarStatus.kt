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
    var coolant_temperature: MutableState<Int> = mutableStateOf(0)
) {
    fun reset() {
        button_repeat_left.value = false
        button_repeat_right.value = false
        button_repeat_center.value = false
        button_wheel_left.value = false
        button_wheel_right.value = false
        wheel_value.value = -1
        gear.value = 0
        speed.value = 0
        rpm.value = 0
        voltage.value = 0
        isIgnitionOn.value = false
        coolant_temperature.value = 0
    }

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
                coolant_temperature.value == other.coolant_temperature.value

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
                "coolant_temperature": ${coolant_temperature.value}
            }
        """.trimIndent()
    }

    fun fromJson(json: String) {
        val regex = """"(\w+)":\s*(true|false|null|\d+)""".toRegex()
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
            }
        }
    }
}