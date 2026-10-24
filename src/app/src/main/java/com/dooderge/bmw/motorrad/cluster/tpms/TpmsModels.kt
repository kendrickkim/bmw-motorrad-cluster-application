package com.dooderge.bmw.motorrad.cluster.tpms

enum class TpmsPressureUnit(val label: String) {
    BAR("Bar"),
    KPA("kPa"),
    PSI("PSI");

    companion object {
        fun fromLabel(label: String): TpmsPressureUnit =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: BAR
    }
}

/** 클러스터 모터사이클: Front / Rear */
enum class TirePosition(val label: String) {
    FRONT("Front"),
    REAR("Rear");

    companion object {
        fun fromLabel(label: String): TirePosition? =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: when (label.lowercase()) {
                    "front" -> FRONT
                    "rear" -> REAR
                    else -> null
                }
    }
}

/** Sysgration advertise 위치 바이트 → 클러스터 슬롯 */
enum class AdvWheelLocation(val wire: Int) {
    FRONT_LEFT(0x80),
    FRONT_RIGHT(0x81),
    REAR_LEFT(0x82),
    REAR_RIGHT(0x83);

    fun toClusterSlot(): TirePosition = when (this) {
        FRONT_LEFT, FRONT_RIGHT -> TirePosition.FRONT
        REAR_LEFT, REAR_RIGHT -> TirePosition.REAR
    }

    companion object {
        fun fromWire(b: Int): AdvWheelLocation? =
            entries.firstOrNull { it.wire == (b and 0xFF) }
    }
}

enum class TpmsSensorBrand {
    SYSGRATION,
    PECHAM,
    WICARLINK,
    UNKNOWN
}

enum class TpmsConnectionState {
    DISCONNECTED,
    SCANNING,
    LISTENING,
    ERROR
}

/**
 * @param pressureKpaCents 압력 (kPa × 100). 예: 25000 → 250.00 kPa ≈ 2.50 Bar. -1 = 없음
 */
data class TireReading(
    val position: TirePosition,
    val pressureKpaCents: Int = -1,
    val temperatureC: Int = -1,
    val battery: Int = -1,
    val sensorId: Int = -1,
    val sensorIdHex: String = "",
    val brand: TpmsSensorBrand = TpmsSensorBrand.UNKNOWN,
    val isAlarm: Boolean = false,
    val rssi: Int = 0,
    val updatedAtMillis: Long = 0L
) {
    val hasPressure: Boolean get() = pressureKpaCents >= 0

    /** 하위 호환: CarStatus / broadcast 가 쓰던 raw 이름 */
    val pressureRaw: Int get() = pressureKpaCents

    fun pressureWithUnit(unit: TpmsPressureUnit = TpmsPressureUnit.BAR): String {
        if (!hasPressure) return "-- ${unit.label}"
        val kpa = pressureKpaCents / 100.0
        return when (unit) {
            TpmsPressureUnit.BAR -> String.format("%.2f Bar", kpa / 100.0)
            TpmsPressureUnit.KPA -> String.format("%.1f kPa", kpa)
            TpmsPressureUnit.PSI -> String.format("%.1f PSI", kpa / 6.895)
        }
    }
}

data class WheelDeviceInfo(
    val name: String = "",
    val address: String = "",
    /**
     * Sysgration/Wicarlink/Pecham(hash) 센서 ID.
     * Pecham 은 MAC.hashCode() 로 **음수 가능**. 미등록은 Int.MIN_VALUE.
     */
    val sensorId: Int = Int.MIN_VALUE,
    val connection: TpmsConnectionState = TpmsConnectionState.DISCONNECTED
) {
    val isBound: Boolean get() = sensorId != Int.MIN_VALUE || address.isNotBlank()

    fun labelOrPlaceholder(): String = when {
        sensorId != Int.MIN_VALUE && name.isNotBlank() ->
            "$name · ${TpmsProtocol.sensorIdToHex(sensorId)}"
        sensorId != Int.MIN_VALUE -> TpmsProtocol.sensorIdToHex(sensorId)
        name.isNotBlank() && address.isNotBlank() -> "$name ($address)"
        address.isNotBlank() -> address
        else -> "Not selected"
    }
}

data class TpmsDeviceState(
    val frontDevice: WheelDeviceInfo = WheelDeviceInfo(),
    val rearDevice: WheelDeviceInfo = WheelDeviceInfo(),
    val statusMessage: String = "",
    val front: TireReading = TireReading(TirePosition.FRONT),
    val rear: TireReading = TireReading(TirePosition.REAR),
    /** 임계값도 kPa 단위 (로컬 알림용, 센서 Write 없음) */
    val pressureMinKpa: Int = 180,
    val pressureMaxKpa: Int = 320,
    val temperatureMax: Int = 90,
    val displayUnit: TpmsPressureUnit = TpmsPressureUnit.BAR,
    val scannedDevices: List<ScannedTpmsDevice> = emptyList()
) {
    @Deprecated("Use pressureMinKpa", ReplaceWith("pressureMinKpa"))
    val pressureMin: Int get() = pressureMinKpa

    @Deprecated("Use pressureMaxKpa", ReplaceWith("pressureMaxKpa"))
    val pressureMax: Int get() = pressureMaxKpa

    val connection: TpmsConnectionState
        get() {
            val states = listOf(frontDevice.connection, rearDevice.connection)
            return when {
                states.any { it == TpmsConnectionState.SCANNING } -> TpmsConnectionState.SCANNING
                states.any { it == TpmsConnectionState.LISTENING } -> TpmsConnectionState.LISTENING
                states.any { it == TpmsConnectionState.ERROR } -> TpmsConnectionState.ERROR
                else -> TpmsConnectionState.DISCONNECTED
            }
        }
}

data class ScannedTpmsDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val sensorId: Int,
    val sensorIdHex: String,
    val brand: TpmsSensorBrand,
    val suggestedSlot: TirePosition? = null,
    val pressureKpaCents: Int = -1,
    val temperatureC: Int = -1
)

/** Advertise 파싱 결과 (바인딩 전) */
data class AdvTireSample(
    val sensorId: Int,
    val sensorIdHex: String,
    val pressureKpaCents: Int,
    val temperatureC: Int,
    val battery: Int,
    val isAlarm: Boolean,
    val rssi: Int,
    val brand: TpmsSensorBrand,
    val macAddress: String = "",
    val advLocation: AdvWheelLocation? = null
) {
    fun suggestedSlot(): TirePosition? = advLocation?.toClusterSlot()
}
