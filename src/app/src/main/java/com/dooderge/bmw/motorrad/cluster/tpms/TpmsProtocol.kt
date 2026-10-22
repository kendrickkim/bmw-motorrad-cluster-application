package com.dooderge.bmw.motorrad.cluster.tpms

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TPMS Advanced Advertise 프로토콜 헬퍼.
 * GATT Write/Notify/0xFC 프레임 없음 — 로컬 바인딩·단위 변환만.
 */
object TpmsProtocol {
    const val PSI_TO_KPA = 6.895f

    /** 로컬 알림 임계 (kPa) */
    const val LOW_PRESSURE_MIN_KPA = 150
    const val LOW_PRESSURE_MAX_KPA = 220
    const val HIGH_PRESSURE_MIN_KPA = 280
    const val HIGH_PRESSURE_MAX_KPA = 350
    const val TEMP_MIN = 70
    const val TEMP_MAX = 90

    /** UI 호환 별칭 (구 BAR×10 스케일 대신 kPa) */
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

    /**
     * Sysgration QR 6hex → sensorId (LE int, 앞에 0x00).
     * 예: "1A2B3C" → bytes 00 1A 2B 3C LE
     */
    fun hex6ToSensorId(hex6: String): Int {
        val cleaned = hex6.trim().replace(" ", "").uppercase()
        require(cleaned.length == 6 && cleaned.all { it in "0123456789ABCDEF" }) {
            "expected 6 hex chars"
        }
        val three = hexStringToBytes(cleaned)
        val padded = byteArrayOf(0x00) + three
        return ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun sensorIdToHex(id: Int): String {
        if (id < 0) return "------"
        // 하위 3바이트 LE → 6 hex (Sysgration 표시와 동일)
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(id).array()
        return "%02X%02X%02X".format(buf[1].toInt() and 0xFF, buf[2].toInt() and 0xFF, buf[3].toInt() and 0xFF)
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
}

/**
 * TPMS Advanced QR:
 * - `HHHHHH&HHHHHH` (2휠) / 4개 `&` 연결
 * - 단일 6hex Wicarlink는 앱에서도 미지원 → 수동 바인딩
 * - BLE MAC → Pecham 등 MAC hash 바인딩용
 */
sealed class TpmsQrPayload {
    data class SensorHexId(val hex: String, val sensorId: Int) : TpmsQrPayload()
    data class MultiSensorIds(val ids: List<Pair<TirePosition, Int>>) : TpmsQrPayload()
    data class BleAddress(val address: String) : TpmsQrPayload()
}

object TpmsQrParser {
    private val fourSensor =
        Regex("""([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})""")
    private val twoSensor =
        Regex("""([0-9A-Fa-f]{6})&([0-9A-Fa-f]{6})""")
    private val wicarlinkSingle =
        Regex("""^([0-9A-Fa-f]{6})$""")
    private val macColon =
        Regex("""([0-9A-Fa-f]{2}[:\-]){5}[0-9A-Fa-f]{2}""")

    fun parse(raw: String): TpmsQrPayload? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        macColon.find(text)?.value?.let { mac ->
            return TpmsQrPayload.BleAddress(mac.replace('-', ':').uppercase())
        }

        fourSensor.find(text)?.let { m ->
            val hexs = m.groupValues.subList(1, 5)
            return TpmsQrPayload.MultiSensorIds(mapHexListToSlots(hexs))
        }
        twoSensor.find(text)?.let { m ->
            val hexs = m.groupValues.subList(1, 3)
            // 모터사이클: 0=Front, 1=Rear
            val ids = hexs.mapIndexed { index, hex ->
                val slot = when (index) {
                    0 -> TirePosition.FRONT
                    else -> TirePosition.REAR
                }
                slot to TpmsProtocol.hex6ToSensorId(hex)
            }
            return TpmsQrPayload.MultiSensorIds(ids)
        }

        if (wicarlinkSingle.matches(text)) {
            // TPMS Advanced: UnsupportedWicarlinkQrCode — 단일 6hex는 수동 권장
            // 클러스터에서는 Front 후보로라도 허용
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

    /** 4휠 QR → 모터사이클 Front=FL(index0), Rear=RL(index2) */
    private fun mapHexListToSlots(hexs: List<String>): List<Pair<TirePosition, Int>> {
        if (hexs.size < 2) return emptyList()
        return listOf(
            TirePosition.FRONT to TpmsProtocol.hex6ToSensorId(hexs[0]),
            TirePosition.REAR to TpmsProtocol.hex6ToSensorId(
                hexs.getOrElse(2) { hexs[1] }
            )
        )
    }
}
