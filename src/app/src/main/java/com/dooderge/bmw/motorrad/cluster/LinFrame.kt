package com.dooderge.bmw.motorrad.cluster

/**
 * Project 2 notify: 7 bytes, Message ID + Data-0..Data-5.
 * Field meanings follow bmw-motorrad-cluster-linbus lin_analysis.
 */
object LinFrame {
    const val LENGTH = 7

    private const val ID_HANDLE = 0x14
    private const val ID_SPEED = 0x20
    private const val ID_ODOMETER = 0x2B
    private const val ID_ENGINE = 0xE2
    private const val ID_VOLTAGE = 0xE9

    private const val REAR_RPM_TO_KMH = 0.14f

    const val ASC_UNKNOWN = -1
    const val ASC_INTERVENTION = 1
    const val ASC_ERROR = 2
    const val ASC_SELF_DIAGNOSIS = 3
    const val ASC_DEACTIVATED = 4

    const val FAULT_FLASHING_YELLOW = 1
    const val FAULT_STEADY_YELLOW = 2
    const val FAULT_FLASHING_RED = 4
    const val FAULT_STEADY_RED = 8

    fun apply(frame: ByteArray, status: CarStatus): Boolean {
        if (frame.size < LENGTH) return false
        val id = frame[0].u8()
        val d0 = frame[1].u8()
        val d1 = frame[2].u8()
        val d2 = frame[3].u8()
        val d3 = frame[4].u8()
        val d4 = frame[5].u8()
        val d5 = frame[6].u8()
        return when (id) {
            ID_HANDLE -> {
                applyHandle(d1, d2, d3, d5, status)
                true
            }
            ID_SPEED -> {
                applySpeed(d1, d2, d3, d4, d5, status)
                true
            }
            ID_ODOMETER -> {
                if (d1 == 0x25) {
                    status.odometerKm.value = (d4 shl 16) or (d3 shl 8) or d2
                }
                true
            }
            ID_ENGINE -> {
                applyEngine(d0, d1, d2, d3, d4, status)
                true
            }
            ID_VOLTAGE -> {
                applyVoltage(d0, d1, d2, d3, d4, status)
                true
            }
            else -> false
        }
    }

    private fun applyHandle(
        handle: Int,
        handle2: Int,
        wheelInOut: Int,
        rotation: Int,
        status: CarStatus
    ) {
        status.esa.value = handle.bit(0)
        status.abs.value = handle.bit(1)
        status.hazard.value = handle.bit(2)
        status.cruise.value = handle.bit(3)
        status.button_repeat_left.value = handle == 0x10
        status.button_repeat_right.value = handle == 0x20
        status.button_repeat_center.value = handle == 0x40
        status.horn.value = handle.bit(7)

        status.highBeam.value = handle2.bit(1)
        status.cruiseReset.value = handle2.bit(2)
        status.cruiseSet.value = handle2.bit(3)
        status.info.value = handle2.bit(4)
        status.trip.value = handle2.bit(5)

        status.button_wheel_left.value = wheelInOut == 0xFD
        status.button_wheel_right.value = wheelInOut == 0xFE
        status.wheelDirection.value = wheelDirection(status.wheel_value.value, rotation)
        status.wheel_value.value = rotation
    }

    private fun wheelDirection(previous: Int, rotation: Int): Int {
        if (previous < 0 || previous == rotation) return 0
        var up = previous < rotation
        if (previous < rotation && rotation - previous > 200) up = false
        if (previous > rotation && previous - rotation > 200) up = true
        return if (up) 1 else -1
    }

    private fun applySpeed(
        ignition: Int,
        speedLow: Int,
        speedHighAndRange: Int,
        rangeRest: Int,
        ambient: Int,
        status: CarStatus
    ) {
        val ignitionNibble = (ignition ushr 4) and 0x0F
        when (ignitionNibble) {
            in 0..3 -> {
                status.ignitionKnown.value = true
                status.isIgnitionOn.value = false
                status.speed.value = 0
                status.rpm.value = 0
            }
            in 4..7 -> {
                status.ignitionKnown.value = true
                status.isIgnitionOn.value = true
            }
        }

        val speedAccurate = speedLow != 0xFF && (speedHighAndRange and 0x0F) != 0x0F
        if (status.isIgnitionOn.value && speedAccurate) {
            val rearWheelRpm = speedLow or ((speedHighAndRange and 0x0F) shl 8)
            status.speed.value = (rearWheelRpm * REAR_RPM_TO_KMH).toInt()
        }

        val rangeAccurate = ((speedHighAndRange ushr 4) and 0x0F) != 0x0F && rangeRest != 0xFF
        if (rangeAccurate) {
            val lowDigit = (speedHighAndRange ushr 4) and 0x0F
            val mid = rangeRest and 0x0F
            val high = (rangeRest ushr 4) and 0x0F
            status.fuelRangeKm.value = lowDigit + (mid * 16) + (high * 256)
        }

        val ambientNibble = ambient and 0x0F
        if (ambientNibble != 0x0F) status.ambientLight.value = ambientNibble
    }

    private fun applyEngine(
        rpmLow: Int,
        rpmHighAndGear: Int,
        throttle: Int,
        temperature: Int,
        ascAndOil: Int,
        status: CarStatus
    ) {
        val rpmNibble = rpmHighAndGear and 0x0F
        if (status.isIgnitionOn.value && rpmLow != 0xFF && rpmNibble != 0x0F) {
            val rpm = (rpmLow + (rpmNibble * 255)) * 5
            status.rpm.value = rpm
        }

        when ((rpmHighAndGear ushr 4) and 0x0F) {
            0x1 -> status.gear.value = 1
            0x2 -> status.gear.value = 0
            0x4 -> status.gear.value = 2
            0x7 -> status.gear.value = 3
            0x8 -> status.gear.value = 4
            0xB -> status.gear.value = 5
            0xD -> status.gear.value = 6
        }

        if (temperature != 0xFF) {
            val celsius = (temperature * 0.75) - 25.0
            status.coolant_temperature.value = (celsius * 10.0).toInt()
        }

        if (throttle != 0xFF) {
            val percent = ((throttle - 36) * 100) / (236 - 36)
            status.throttlePercent.value = percent.coerceIn(0, 100)
        }

        val ascNibble = (ascAndOil ushr 4) and 0x0F
        if (ascNibble != 0x0F) {
            status.ascState.value = when (ascNibble) {
                0x1, 0x9 -> ASC_INTERVENTION
                0x2, 0x5, 0x6, 0x7, 0xA, 0xD, 0xE -> ASC_ERROR
                0x3, 0xB -> ASC_SELF_DIAGNOSIS
                0x8 -> ASC_DEACTIVATED
                else -> ASC_UNKNOWN
            }
        }

        val oilNibble = ascAndOil and 0x0F
        if (oilNibble != 0x0F) {
            status.oilLow.value = oilNibble == 0x2 || oilNibble == 0x6 ||
                oilNibble == 0xA || oilNibble == 0xE
        }
    }

    private fun applyVoltage(
        averageLow: Int,
        averageHigh: Int,
        speedByte: Int,
        voltage: Int,
        faults: Int,
        status: CarStatus
    ) {
        val averageNibble = averageHigh and 0x0F
        if (averageLow != 0xFF && averageNibble != 0x0F) {
            val average = ((averageLow ushr 4) and 0x0F) * 2.0 +
                (averageLow and 0x0F) * 0.125 +
                averageNibble * 32.0
            status.averageSpeedKmh.value = average.toInt()
        }

        if (speedByte != 0xFF) status.speedE9Kmh.value = speedByte * 2
        if (voltage != 0xFF) status.voltage.value = voltage

        val fuelNibble = (faults ushr 4) and 0x0F
        if (fuelNibble != 0x0F) {
            status.fuelFault.value = fuelNibble == 0x2 || fuelNibble == 0x6 ||
                fuelNibble == 0xA || fuelNibble == 0xE
        }

        val generalNibble = faults and 0x0F
        if (generalNibble != 0x0F) {
            var flags = 0
            if (generalNibble == 0x1 || generalNibble == 0x5 || generalNibble == 0xD) {
                flags = flags or FAULT_FLASHING_YELLOW
            }
            if (generalNibble == 0x2 || generalNibble == 0x6 ||
                generalNibble == 0xA || generalNibble == 0xE
            ) {
                flags = flags or FAULT_STEADY_YELLOW
            }
            if (generalNibble == 0x4 || generalNibble == 0x5 || generalNibble == 0x6 ||
                generalNibble == 0x7 || generalNibble == 0x9
            ) {
                flags = flags or FAULT_FLASHING_RED
            }
            if (generalNibble == 0x8 || generalNibble == 0x9 ||
                generalNibble == 0xA || generalNibble == 0xB
            ) {
                flags = flags or FAULT_STEADY_RED
            }
            status.generalFault.value = flags
        }
    }
}

private fun Byte.u8(): Int = toInt() and 0xFF

private fun Int.bit(index: Int): Boolean = ((this ushr index) and 1) == 1
