package com.dooderge.bmw.motorrad.cluster.tpms

import android.bluetooth.le.ScanResult
import android.util.Log
import kotlin.math.roundToInt

/**
 * LYTPMS 1.0.3 (`com.zl.dev.tire.BleServiceLY` / `TireBean.parse`) 기준 Advertise 파서.
 *
 * 오픈소스(TPMS Advanced Sysgration/Pecham) 레이아웃은 사용하지 않음.
 */
object TpmsAdvParsers {
    private const val TAG = "TpmsAdvParsers"

    /** `BaseTireBleService.BLE_ID` — Flags(0x06) + Complete UUID list `FBB0` */
    private const val BLE_ID_HEX = "0201060303B0FB"

    fun parse(result: ScanResult): AdvTireSample? {
        val sample = parseLytpms(result)
        if (sample == null) {
            val record = result.scanRecord
            val hex = record?.bytes?.let { TpmsProtocol.bytesToHexCompact(it.take(16).toByteArray()) }
            if (hex != null && (hex.startsWith("0201") || hex.contains("B0FB") || hex.contains("FBB0"))) {
                Log.d(TAG, "unparsed lytpms-like rssi=${result.rssi} hex=$hex")
            }
        }
        return sample
    }

    /**
     * Official flow:
     * 1. `scanRecord.bytes` hex starts with `0201060303B0FB`
     * 2. payload = hex.substring(14, 52) → 19 bytes → [TireBean.parse]
     * 3. CRC = [VerifyUtil.validateCRCLYTPMS](hex.substring(18))
     */
    fun parseLytpms(result: ScanResult): AdvTireSample? {
        val bytes = result.scanRecord?.bytes ?: return null
        if (bytes.size < 26) return null
        val hex = TpmsProtocol.bytesToHexCompact(bytes)
        if (!hex.startsWith(BLE_ID_HEX, ignoreCase = true)) return null
        if (hex.length < 52) return null

        val crcInput = try {
            TpmsProtocol.hexStringToBytes(hex.substring(18))
        } catch (_: Exception) {
            return null
        }
        if (!validateCrcLytpms(crcInput)) {
            Log.d(TAG, "CRC fail hex=${hex.take(52)}")
            return null
        }

        val payload = try {
            TpmsProtocol.hexStringToBytes(hex.substring(14, 52))
        } catch (_: Exception) {
            return null
        }
        if (payload.size < 19) return null

        // TireBean.parse (payload-relative indices)
        val voltage = (payload[4].toInt() and 0xFF) * 0.01f + 1.22f
        var pressureRaw = payload[5].toInt() and 0xFF
        if ((payload[10].toInt() and 0xFF) == 1) pressureRaw += 256
        val pressureKpa = pressureRaw * 3.144f
        val tempC = (payload[6].toInt() and 0xFF) - 55
        val state = payload[7].toInt() and 0xFF

        // ID = bytes2HexString([p18, p17, p16]) → display order MSB first
        val idHex = "%02X%02X%02X".format(
            payload[18].toInt() and 0xFF,
            payload[17].toInt() and 0xFF,
            payload[16].toInt() and 0xFF
        )
        val sensorId = idHex.toInt(16)
        val batteryPct = batteryPercent(voltage)

        return AdvTireSample(
            sensorId = sensorId,
            sensorIdHex = idHex,
            pressureKpaCents = TpmsProtocol.kpaToCents(pressureKpa),
            temperatureC = tempC,
            battery = batteryPct,
            isAlarm = state == 1 || voltage <= 2.1f,
            rssi = result.rssi,
            brand = TpmsSensorBrand.LYTPMS,
            macAddress = result.device?.address.orEmpty(),
            voltage = voltage,
            state = state
        )
    }

    /** Official `TireBean.getPower()` */
    private fun batteryPercent(voltage: Float): Int {
        val pct = (((voltage - 1.8) / 1.2) * 100.0).roundToInt()
        return pct.coerceIn(0, 100)
    }

    /**
     * Official `VerifyUtil.validateCRCLYTPMS`.
     * Input = advertise bytes from index 9 (hex.substring(18)).
     * Note: expected uses **signed** `bArr[8]` subtraction (Java byte → int).
     */
    fun validateCrcLytpms(bArr: ByteArray): Boolean {
        if (bArr.size < 17) return false
        val buf = ByteArray(14)
        System.arraycopy(bArr, 0, buf, 0, 7)
        System.arraycopy(bArr, 10, buf, 7, 7)
        val expected =
            (((bArr[7].toInt() and 0xFF) shl 8) or (bArr[9].toInt() and 0xFF)) - bArr[8].toInt()
        return crc16Xmodem(buf) == expected
    }

    private fun crc16Xmodem(data: ByteArray): Int {
        var crc = 0
        for (byte in data) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
            }
        }
        return crc and 0xFFFF
    }
}
