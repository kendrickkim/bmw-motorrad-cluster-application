package com.dooderge.bmw.motorrad.cluster.tpms

/**
 * LYTPMS Advertise 프로토콜 헬퍼.
 * GATT Write/Notify 없음 — 로컬 바인딩·단위 변환만.
 *
 * 센서 ID는 공식 앱과 동일하게 **6자리 hex 문자열**(예: `CCBBAA`)을
 * big-endian int 로 취급한다. (Sysgration LE 패딩 방식 사용 금지)
 */
object TpmsProtocol {
    /** Official `TireBean.getPressPsi()` divisor */
    const val PSI_TO_KPA = 6.89476f

    const val LOW_PRESSURE_MIN_KPA = 150
    const val LOW_PRESSURE_MAX_KPA = 220
    const val HIGH_PRESSURE_MIN_KPA = 280
    const val HIGH_PRESSURE_MAX_KPA = 350
    const val TEMP_MIN = 70
    const val TEMP_MAX = 90

    const val LOW_PRESSURE_MIN = LOW_PRESSURE_MIN_KPA
    const val LOW_PRESSURE_MAX = LOW_PRESSURE_MAX_KPA
    const val HIGH_PRESSURE_MIN = HIGH_PRESSURE_MIN_KPA
    const val HIGH_PRESSURE_MAX = HIGH_PRESSURE_MAX_KPA

    fun kpaToCents(kpa: Float): Int = (kpa * 100f).toInt().coerceAtLeast(0)

    fun centsToKpa(cents: Int): Float = cents / 100f

    fun formatPressure(cents: Int, unit: TpmsPressureUnit): String {
        if (cents < 0) return "-- ${unit.label}"
        val kpa = centsToKpa(cents)
        return when (unit) {
            TpmsPressureUnit.BAR -> String.format("%.2f Bar", kpa / 100f)
            TpmsPressureUnit.KPA -> String.format("%.1f kPa", kpa)
            TpmsPressureUnit.PSI -> String.format("%.1f PSI", kpa / PSI_TO_KPA)
        }
    }

    /** LYTPMS 6hex ID → int (공식 앱 `TireBean.ID` 문자열과 동일) */
    fun hex6ToSensorId(hex6: String): Int {
        val cleaned = hex6.trim().replace(" ", "").uppercase()
        require(cleaned.length == 6 && cleaned.all { it in "0123456789ABCDEF" }) {
            "expected 6 hex chars"
        }
        return cleaned.toInt(16)
    }

    fun sensorIdToHex(id: Int): String {
        if (id < 0) return "------"
        return "%06X".format(id and 0xFFFFFF)
    }

    fun hexStringToBytes(hex: String): ByteArray {
        val cleaned = hex.trim()
            .replace(" ", "")
            .replace(":", "")
            .replace("-", "")
            .uppercase()
        require(cleaned.length % 2 == 0 && cleaned.isNotEmpty()) { "hex length must be even" }
        require(cleaned.all { it in "0123456789ABCDEF" }) { "invalid hex" }
        val out = ByteArray(cleaned.length / 2)
        for (i in out.indices) {
            val hi = "0123456789ABCDEF".indexOf(cleaned[i * 2])
            val lo = "0123456789ABCDEF".indexOf(cleaned[i * 2 + 1])
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    fun bytesToHex(data: ByteArray): String =
        data.joinToString(" ") { "%02X".format(it) }

    fun bytesToHexCompact(data: ByteArray): String =
        data.joinToString("") { "%02X".format(it) }
}

/**
 * LYTPMS QR / 수동 입력:
 * - 단일 6hex 센서 ID (공식 앱과 동일)
 * - `HHHHHH&HHHHHH` (Front&Rear)
 */
sealed class TpmsQrPayload {
    data class SensorHexId(val hex: String, val sensorId: Int) : TpmsQrPayload()
    data class MultiSensorIds(val ids: List<Pair<TirePosition, Int>>) : TpmsQrPayload()
}

object TpmsQrParser {
    private val twoSensor =
        Regex("""([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})""")
    private val fourSensor =
        Regex("""([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})""")
    private val singleHex =
        Regex("""^([0-9A-Fa-f]{6})$""")

    fun parse(raw: String): TpmsQrPayload? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        fourSensor.find(text)?.let { m ->
            val hexs = m.groupValues.subList(1, 5)
            return TpmsQrPayload.MultiSensorIds(
                listOf(
                    TirePosition.FRONT to TpmsProtocol.hex6ToSensorId(hexs[0]),
                    TirePosition.REAR to TpmsProtocol.hex6ToSensorId(hexs.getOrElse(2) { hexs[1] })
                )
            )
        }
        twoSensor.find(text)?.let { m ->
            val hexs = m.groupValues.subList(1, 3)
            return TpmsQrPayload.MultiSensorIds(
                listOf(
                    TirePosition.FRONT to TpmsProtocol.hex6ToSensorId(hexs[0]),
                    TirePosition.REAR to TpmsProtocol.hex6ToSensorId(hexs[1])
                )
            )
        }
        if (singleHex.matches(text)) {
            return TpmsQrPayload.SensorHexId(
                text.uppercase(),
                TpmsProtocol.hex6ToSensorId(text)
            )
        }
        val cleaned = text.replace(Regex("""[^0-9A-Fa-f]"""), "")
        if (cleaned.length == 6) {
            return TpmsQrPayload.SensorHexId(
                cleaned.uppercase(),
                TpmsProtocol.hex6ToSensorId(cleaned)
            )
        }
        return null
    }
}
