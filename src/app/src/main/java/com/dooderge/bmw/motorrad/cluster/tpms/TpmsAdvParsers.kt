package com.dooderge.bmw.motorrad.cluster.tpms

import android.bluetooth.le.ScanResult
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * TPMS Advanced Advertise 파서 (Sysgration / Pecham / Wicarlink).
 */
object TpmsAdvParsers {
    private const val TAG = "TpmsAdvParsers"
    private val SYSGRATION_ADDR = byteArrayOf(0xEA.toByte(), 0xCA.toByte())

    fun parse(result: ScanResult): AdvTireSample? {
        val sample = parsePecham(result)
            ?: parseWicarlink(result)
            ?: parseSysgration(result)
        if (sample == null) {
            val record = result.scanRecord
            val uuids = record?.serviceUuids?.joinToString { it.uuid.toString().take(8) }
            val name = record?.deviceName
            if (!uuids.isNullOrBlank() || name == "BR") {
                Log.d(
                    TAG,
                    "unparsed rssi=${result.rssi} name=$name uuids=$uuids " +
                        "mfgSize=${record?.manufacturerSpecificData?.size() ?: 0} " +
                        "flags=${record?.advertiseFlags} " +
                        "bytes=${record?.bytes?.let { TpmsProtocol.bytesToHex(it.take(32).toByteArray()) }}"
                )
            }
        }
        return sample
    }

    fun parseSysgration(result: ScanResult): AdvTireSample? {
        val record = result.scanRecord ?: return null

        // 1) manufacturerSpecificData 각 엔트리 (열린 소스: value 안에 loc+EACA+…)
        val mfg = record.manufacturerSpecificData
        if (mfg != null) {
            for (i in 0 until mfg.size()) {
                val payload = mfg.valueAt(i) ?: continue
                decodeSysgrationPayload(payload, result)?.let { return it }

                // 2) companyId 가 EA CA 인 경우 — Android가 CID를 제거한 형태
                val companyId = mfg.keyAt(i)
                val cidLo = (companyId and 0xFF).toByte()
                val cidHi = ((companyId shr 8) and 0xFF).toByte()
                if (cidLo == SYSGRATION_ADDR[0] && cidHi == SYSGRATION_ADDR[1] && payload.size >= 14) {
                    // wire: EA CA | loc | id0 id1 id2 | …  → value: loc | id…
                    decodeSysgrationPayloadAfterCidStrip(payload, result)?.let { return it }
                }
            }
        }

        // 3) raw AD 바이트에서 0xFF manufacturer 블록 직접 탐색
        val raw = record.bytes ?: return null
        return decodeSysgrationFromRawAd(raw, result)
    }

    /** payload = [loc][EA][CA][id0][id1][id2][p0..p3][t0..t3][bat][alarm] (16+) */
    private fun decodeSysgrationPayload(data: ByteArray, result: ScanResult): AdvTireSample? {
        if (data.size < 16) return null
        if (data[1] != SYSGRATION_ADDR[0] || data[2] != SYSGRATION_ADDR[1]) return null
        return buildSysgration(
            locationByte = data[0],
            idBytes = data.copyOfRange(3, 6),
            pressBytes = data.copyOfRange(6, 10),
            tempBytes = data.copyOfRange(10, 14),
            battery = data[14].toInt() and 0xFF,
            alarm = data[15] == 0x01.toByte(),
            result = result
        )
    }

    /** CID(EA CA) 제거 후: [loc][id0][id1][id2][p…][t…][bat][alarm] (14+) */
    private fun decodeSysgrationPayloadAfterCidStrip(
        data: ByteArray,
        result: ScanResult
    ): AdvTireSample? {
        if (data.size < 14) return null
        return buildSysgration(
            locationByte = data[0],
            idBytes = data.copyOfRange(1, 4),
            pressBytes = data.copyOfRange(4, 8),
            tempBytes = data.copyOfRange(8, 12),
            battery = data[12].toInt() and 0xFF,
            alarm = data[13] == 0x01.toByte(),
            result = result
        )
    }

    private fun decodeSysgrationFromRawAd(raw: ByteArray, result: ScanResult): AdvTireSample? {
        var i = 0
        while (i + 2 < raw.size) {
            val len = raw[i].toInt() and 0xFF
            if (len == 0) break
            if (i + 1 + len > raw.size) break
            val type = raw[i + 1].toInt() and 0xFF
            if (type == 0xFF && len >= 17) {
                // data: [cid0][cid1][…] length = len-1
                val block = raw.copyOfRange(i + 2, i + 1 + len)
                if (block.size >= 16 &&
                    block[0] == SYSGRATION_ADDR[0] &&
                    block[1] == SYSGRATION_ADDR[1]
                ) {
                    // block = EA CA loc id…  OR EA CA is cid and rest follows
                    // Try: full block as loc-less with EA CA at 0,1 → treat as CID+payload
                    val afterCid = block.copyOfRange(2, block.size)
                    decodeSysgrationPayloadAfterCidStrip(afterCid, result)?.let { return it }
                }
                // block starts with loc, EA CA at 1,2
                decodeSysgrationPayload(block, result)?.let { return it }
            }
            i += len + 1
        }
        return null
    }

    private fun buildSysgration(
        locationByte: Byte,
        idBytes: ByteArray,
        pressBytes: ByteArray,
        tempBytes: ByteArray,
        battery: Int,
        alarm: Boolean,
        result: ScanResult
    ): AdvTireSample? {
        val location = AdvWheelLocation.fromWire(locationByte.toInt() and 0xFF) ?: return null
        val padded = byteArrayOf(0x00) + idBytes
        val sensorId = ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN).int
        val pressureKpa = ByteBuffer.wrap(pressBytes).order(ByteOrder.LITTLE_ENDIAN).int / 1000f
        val tempC = ByteBuffer.wrap(tempBytes).order(ByteOrder.LITTLE_ENDIAN).int / 100f
        return AdvTireSample(
            sensorId = sensorId,
            sensorIdHex = TpmsProtocol.sensorIdToHex(sensorId),
            pressureKpaCents = TpmsProtocol.kpaToCents(pressureKpa),
            temperatureC = tempC.roundToInt(),
            battery = battery,
            isAlarm = alarm,
            rssi = result.rssi,
            brand = TpmsSensorBrand.SYSGRATION,
            macAddress = result.device?.address.orEmpty(),
            advLocation = location
        )
    }

    fun parsePecham(result: ScanResult): AdvTireSample? {
        val record = result.scanRecord ?: return null
        val bytes = record.bytes ?: return null
        val name = record.deviceName
        val nameOk = name == "BR" || bytesContainShortNameBr(bytes)
        if (!nameOk) return null
        val uuids = record.serviceUuids
        val uuidOk = uuids?.contains(TpmsBleUuids.SERVICE_27A5) == true ||
            bytesContainUuid27a5(bytes)
        if (!uuidOk) return null
        if (bytes.size < 17) return null

        val data = bytes.copyOfRange(10, 17)
        val pressurePsi = (
            ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
            ).minus(145).div(10f)
        val pressureKpa = pressurePsi * TpmsProtocol.PSI_TO_KPA
        val battery = data[1].toInt() and 0xFF
        val tempC = data[2].toInt()
        val mac = result.device?.address.orEmpty()
        val sensorId = mac.hashCode()

        return AdvTireSample(
            sensorId = sensorId,
            sensorIdHex = "%08X".format(sensorId),
            pressureKpaCents = TpmsProtocol.kpaToCents(pressureKpa),
            temperatureC = tempC,
            battery = battery,
            isAlarm = battery < 26,
            rssi = result.rssi,
            brand = TpmsSensorBrand.PECHAM,
            macAddress = mac,
            advLocation = null
        )
    }

    fun parseWicarlink(result: ScanResult): AdvTireSample? {
        val record = result.scanRecord ?: return null
        val uuids = record.serviceUuids
        val hasFbb0 = uuids?.contains(TpmsBleUuids.SERVICE_FBB0) == true
        if (!hasFbb0 && record.advertiseFlags != 0x06) return null
        if (!hasFbb0) return null
        val bytes = record.bytes ?: return null
        if (bytes.size < 26) return null
        // flags 가 다르거나 CRC 실패해도 Sysgration 으로 fallthrough 하도록
        // Wicarlink 는 flags+CRC 모두 만족할 때만
        if (record.advertiseFlags != 0x06) return null
        if (!validateWicarlinkCrc(bytes)) return null

        var pressureRaw = bytes[12].toInt() and 0xFF
        if ((bytes[17].toInt() and 0xFF) == 1) pressureRaw += 256
        val pressureKpa = pressureRaw * 3.144f
        val voltage = (bytes[11].toInt() and 0xFF) * 0.01f + 1.22f
        val tempC = (bytes[13].toInt() and 0xFF) - 55
        val sensorId = (bytes[23].toInt() and 0xFF) or
            ((bytes[24].toInt() and 0xFF) shl 8) or
            ((bytes[25].toInt() and 0xFF) shl 16)

        return AdvTireSample(
            sensorId = sensorId,
            sensorIdHex = TpmsProtocol.sensorIdToHex(sensorId and 0xFFFFFF),
            pressureKpaCents = TpmsProtocol.kpaToCents(pressureKpa),
            temperatureC = tempC,
            battery = (voltage * 10f).roundToInt(),
            isAlarm = voltage <= 2.1f,
            rssi = result.rssi,
            brand = TpmsSensorBrand.WICARLINK,
            macAddress = result.device?.address.orEmpty(),
            advLocation = null
        )
    }

    private fun bytesContainShortNameBr(bytes: ByteArray): Boolean {
        // AD: len=3 type=0x08 'B''R' → 03 08 42 52
        var i = 0
        while (i + 3 < bytes.size) {
            val len = bytes[i].toInt() and 0xFF
            if (len == 0) break
            if (i + 1 + len > bytes.size) break
            val type = bytes[i + 1].toInt() and 0xFF
            if ((type == 0x08 || type == 0x09) && len >= 3) {
                if (bytes[i + 2] == 'B'.code.toByte() && bytes[i + 3] == 'R'.code.toByte()) {
                    return true
                }
            }
            i += len + 1
        }
        return false
    }

    private fun bytesContainUuid27a5(bytes: ByteArray): Boolean {
        // 03 03 A5 27 (complete list 16-bit UUID, LE)
        var i = 0
        while (i + 3 < bytes.size) {
            val len = bytes[i].toInt() and 0xFF
            if (len == 0) break
            if (i + 1 + len > bytes.size) break
            val type = bytes[i + 1].toInt() and 0xFF
            if (type == 0x02 || type == 0x03) {
                for (j in 0 until (len - 1) / 2) {
                    val lo = bytes[i + 2 + j * 2].toInt() and 0xFF
                    val hi = bytes[i + 3 + j * 2].toInt() and 0xFF
                    if (lo == 0xA5 && hi == 0x27) return true
                }
            }
            i += len + 1
        }
        return false
    }

    private fun validateWicarlinkCrc(bytes: ByteArray): Boolean {
        val buf = ByteArray(14)
        System.arraycopy(bytes, 9, buf, 0, 7)
        System.arraycopy(bytes, 19, buf, 7, 7)
        val expected =
            (((bytes[16].toInt() and 0xFF) shl 8) or (bytes[18].toInt() and 0xFF)) -
                (bytes[17].toInt() and 0xFF)
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
