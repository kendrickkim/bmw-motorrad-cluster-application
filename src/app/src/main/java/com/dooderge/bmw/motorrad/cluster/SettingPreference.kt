package com.dooderge.bmw.motorrad.cluster

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsPressureUnit
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsProtocol

class SettingPreference private constructor() {
    companion object {
        private const val PREFERENCE_NAME = "settings"
        private const val DEVICE_NAME_KEY = "deviceName"
        private const val ADDRESS_KEY = "address"
        private const val OVERLAY_X_KEY = "overlayX"
        private const val OVERLAY_Y_KEY = "overlayY"
        private const val LARGE_OVERLAY_KEY = "largeOverlay"
        private const val TPMS_ENABLED_KEY = "tpmsEnabled"
        private const val TPMS_DEVICE_NAME_KEY = "tpmsDeviceName"
        private const val TPMS_ADDRESS_KEY = "tpmsAddress"
        private const val TPMS_FRONT_NAME_KEY = "tpmsFrontName"
        private const val TPMS_FRONT_ADDRESS_KEY = "tpmsFrontAddress"
        private const val TPMS_FRONT_SENSOR_ID_KEY = "tpmsFrontSensorId"
        private const val TPMS_REAR_NAME_KEY = "tpmsRearName"
        private const val TPMS_REAR_ADDRESS_KEY = "tpmsRearAddress"
        private const val TPMS_REAR_SENSOR_ID_KEY = "tpmsRearSensorId"
        private const val TPMS_UNIT_KEY = "tpmsUnit"
        private const val TPMS_PRESSURE_MIN_KEY = "tpmsPressureMin"
        private const val TPMS_PRESSURE_MAX_KEY = "tpmsPressureMax"
        private const val TPMS_TEMP_MAX_KEY = "tpmsTemperatureMax"
        private const val TPMS_LAST_FRONT_PRESSURE_KEY = "tpmsLastFrontPressure"
        private const val TPMS_LAST_REAR_PRESSURE_KEY = "tpmsLastRearPressure"
        private const val TPMS_LAST_FRONT_TEMP_KEY = "tpmsLastFrontTemp"
        private const val TPMS_LAST_REAR_TEMP_KEY = "tpmsLastRearTemp"
        private const val TPMS_LAST_FRONT_BATTERY_KEY = "tpmsLastFrontBattery"
        private const val TPMS_LAST_REAR_BATTERY_KEY = "tpmsLastRearBattery"
        private const val TPMS_LAST_READ_AT_KEY = "tpmsLastReadAt"
        private const val LAST_ODOMETER_KM_KEY = "lastOdometerKm"
        private const val LEAN_ANGLE_ZERO_KEY = "leanAngleZeroOffset"
        private const val LEAN_ANGLE_CALIBRATED_KEY = "leanAngleCalibrated"

        /** settings SharedPreferences 단일 진입점 */
        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        fun saveSettings(context: Context, deviceName: String, address: String) {
            val sharedPreferences: SharedPreferences =
                prefs(context)
            val editor = sharedPreferences.edit()
            editor.putString(DEVICE_NAME_KEY, deviceName)
            editor.putString(ADDRESS_KEY, address)
            editor.apply()
        }

        fun getDeviceName(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                prefs(context)
            return sharedPreferences.getString(DEVICE_NAME_KEY, null)
        }

        fun getAddress(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                prefs(context)
            return sharedPreferences.getString(ADDRESS_KEY, null)
        }

        fun getOverlayPosition(context: Context): Pair<Int, Int> {
            val pref = prefs(context)
            val x = pref.getInt(OVERLAY_X_KEY, 0)
            val y = pref.getInt(OVERLAY_Y_KEY, 0)
            return Pair(x, y)
        }

        fun setOverlayPosition(context: Context, x: Int, y: Int) {
            val pref = prefs(context)
            pref.edit().putInt(OVERLAY_X_KEY, x).putInt(OVERLAY_Y_KEY, y).apply()
        }

        fun isLargeOverlay(context: Context): Boolean {
            return prefs(context)
                .getBoolean(LARGE_OVERLAY_KEY, false)
        }

        fun setLargeOverlay(context: Context, enabled: Boolean) {
            prefs(context)
                .edit().putBoolean(LARGE_OVERLAY_KEY, enabled).apply()
        }

        fun setWheelRpmToSpeedFactor(context: Context, factor: Float) {
            val pref = prefs(context)
            pref.edit().putFloat("wheelRpmToSpeedFactor", factor).apply()
        }

        fun getWheelRpmToSpeedFactor(context: Context): Float {
            val pref = prefs(context)
            return pref.getFloat("wheelRpmToSpeedFactor", BleService.WHEEL_RPM_TO_SPEED)
        }

        fun setLinTestEnabled(context: Context, enabled: Boolean) {
            val pref = prefs(context)
            pref.edit().putBoolean("linTestEnabled", enabled).apply()
        }

        fun isLinTestEnabled(context: Context): Boolean {
            val pref = prefs(context)
            return pref.getBoolean("linTestEnabled", false)
        }

        fun setLinLogging(context: Context, enabled: Boolean) {
            val pref = prefs(context)
            pref.edit().putBoolean("linLogging", enabled).apply()
        }

        fun isLinLogging(context: Context): Boolean {
            val pref = prefs(context)
            return pref.getBoolean("linLogging", true)
        }

        fun setGpsLogging(context: Context, enabled: Boolean) {
            val pref = prefs(context)
            pref.edit().putBoolean("gpsLogging", enabled).apply()
        }

        fun isGpsLogging(context: Context): Boolean {
            val pref = prefs(context)
            return pref.getBoolean("gpsLogging", true)
        }

        fun isLeanAngleCalibrated(context: Context): Boolean {
            val pref = prefs(context)
            return pref.getBoolean(LEAN_ANGLE_CALIBRATED_KEY, false)
        }

        fun getLeanAngleZeroOffset(context: Context): Float {
            val pref = prefs(context)
            return pref.getFloat(LEAN_ANGLE_ZERO_KEY, 0f)
        }

        fun setLeanAngleZeroOffset(context: Context, offsetDegrees: Float) {
            val pref = prefs(context)
            pref.edit()
                .putFloat(LEAN_ANGLE_ZERO_KEY, offsetDegrees)
                .putBoolean(LEAN_ANGLE_CALIBRATED_KEY, true)
                .apply()
        }

        fun restoreIgnitionTrip(context: Context, status: CarStatus) {
            val pref = prefs(context)
            status.tripActive.value = pref.getBoolean("tripActive", false)
            status.tripStartedAtMillis.value = pref.getLong("tripStartedAt", 0L)
            status.tripBaseOdometerKm.value = pref.getInt("tripBaseKm", 0)
            status.tripBaseLatched.value = pref.getBoolean("tripBaseLatched", false)
            status.tripABaseOdometerKm.value = pref.getInt("tripABaseKm", 0)
            status.tripABaseLatched.value = pref.getBoolean("tripABaseLatched", false)
            status.tripBBaseOdometerKm.value = pref.getInt("tripBBaseKm", 0)
            status.tripBBaseLatched.value = pref.getBoolean("tripBBaseLatched", false)
            restoreLastOdometer(context, status)
        }

        /** 한 번이라도 수신된 총 주행거리. 0 이하는 무시. */
        fun noteOdometerKm(context: Context, km: Int) {
            if (km <= 0) return
            val pref = prefs(context)
            if (pref.getInt(LAST_ODOMETER_KM_KEY, 0) == km) return
            pref.edit().putInt(LAST_ODOMETER_KM_KEY, km).apply()
        }

        fun getLastOdometerKm(context: Context): Int =
            prefs(context)
                .getInt(LAST_ODOMETER_KM_KEY, 0)

        /** 라이브 값이 없을 때 저장된 총 주행거리를 채운다. */
        fun restoreLastOdometer(context: Context, status: CarStatus) {
            if (status.odometerKm.value > 0) return
            val saved = getLastOdometerKm(context)
            if (saved > 0) status.odometerKm.value = saved
        }

        fun updateIgnitionTrip(context: Context, status: CarStatus) {
            noteOdometerKm(context, status.odometerKm.value)
            latchTripComputersIfNeeded(context, status)

            if (!status.ignitionKnown.value) return
            if (status.isIgnitionOn.value) {
                if (!status.tripActive.value) {
                    status.tripActive.value = true
                    status.tripStartedAtMillis.value = System.currentTimeMillis()
                    if (status.odometerKm.value > 0) {
                        status.tripBaseOdometerKm.value = status.odometerKm.value
                        status.tripBaseLatched.value = true
                    } else {
                        status.tripBaseOdometerKm.value = 0
                        status.tripBaseLatched.value = false
                    }
                    saveIgnitionTrip(context, status)
                } else if (!status.tripBaseLatched.value && status.odometerKm.value > 0) {
                    status.tripBaseOdometerKm.value = status.odometerKm.value
                    status.tripBaseLatched.value = true
                    saveIgnitionTrip(context, status)
                }
            } else if (status.tripActive.value) {
                status.tripActive.value = false
                status.tripStartedAtMillis.value = 0L
                status.tripBaseOdometerKm.value = 0
                status.tripBaseLatched.value = false
                saveIgnitionTrip(context, status)
            }
        }

        /** TripA/B: 최초 유효 오도메터가 오면 기준을 잡는다. */
        fun latchTripComputersIfNeeded(context: Context, status: CarStatus) {
            val odo = effectiveOdometerKm(context, status)
            if (odo <= 0) return
            val changedA = latchTripBaseIfNeeded(
                status.tripABaseOdometerKm,
                status.tripABaseLatched,
                odo
            )
            val changedB = latchTripBaseIfNeeded(
                status.tripBBaseOdometerKm,
                status.tripBBaseLatched,
                odo
            )
            if (changedA || changedB) saveTripComputers(context, status)
        }

        /** 아직 latch 되지 않은 Trip 기준을 odo로 고정. 변경 시 true. */
        private fun latchTripBaseIfNeeded(
            baseKm: MutableState<Int>,
            latched: MutableState<Boolean>,
            odo: Int,
        ): Boolean {
            if (latched.value) return false
            baseKm.value = odo
            latched.value = true
            return true
        }

        fun effectiveOdometerKm(context: Context, status: CarStatus): Int {
            val live = status.odometerKm.value
            return if (live > 0) live else getLastOdometerKm(context)
        }

        fun tripComputerKm(context: Context, status: CarStatus, trip: Char): Int {
            val odo = effectiveOdometerKm(context, status)
            val (base, latched) = when (trip) {
                'A' -> status.tripABaseOdometerKm.value to status.tripABaseLatched.value
                'B' -> status.tripBBaseOdometerKm.value to status.tripBBaseLatched.value
                else -> return 0
            }
            if (!latched || odo <= 0) return 0
            return (odo - base).coerceAtLeast(0)
        }

        private fun saveIgnitionTrip(context: Context, status: CarStatus) {
            prefs(context)
                .edit()
                .putBoolean("tripActive", status.tripActive.value)
                .putLong("tripStartedAt", status.tripStartedAtMillis.value)
                .putInt("tripBaseKm", status.tripBaseOdometerKm.value)
                .putBoolean("tripBaseLatched", status.tripBaseLatched.value)
                .apply()
        }

        private fun saveTripComputers(context: Context, status: CarStatus) {
            prefs(context)
                .edit()
                .putInt("tripABaseKm", status.tripABaseOdometerKm.value)
                .putBoolean("tripABaseLatched", status.tripABaseLatched.value)
                .putInt("tripBBaseKm", status.tripBBaseOdometerKm.value)
                .putBoolean("tripBBaseLatched", status.tripBBaseLatched.value)
                .apply()
        }

        /** TripA/B 수동 리셋 — 현재 오도메터를 새 기준으로 저장 */
        fun resetTripComputer(context: Context, status: CarStatus, trip: Char) {
            val odo = effectiveOdometerKm(context, status)
            when (trip) {
                'A' -> {
                    status.tripABaseOdometerKm.value = odo
                    status.tripABaseLatched.value = odo > 0
                }
                'B' -> {
                    status.tripBBaseOdometerKm.value = odo
                    status.tripBBaseLatched.value = odo > 0
                }
                else -> return
            }
            saveTripComputers(context, status)
        }

        fun setTpmsEnabled(context: Context, enabled: Boolean) {
            prefs(context)
                .edit().putBoolean(TPMS_ENABLED_KEY, enabled).apply()
        }

        fun isTpmsEnabled(context: Context): Boolean {
            return prefs(context)
                .getBoolean(TPMS_ENABLED_KEY, false)
        }

        /** 앞/뒤 휠 센서(이름·주소·ID)를 한 경로로 저장. */
        private fun setTpmsWheelSensor(
            context: Context,
            front: Boolean,
            name: String,
            address: String,
            sensorId: Int? = null,
        ) {
            val edit = prefs(context).edit()
            if (front) {
                edit.putString(TPMS_FRONT_NAME_KEY, name)
                    .putString(TPMS_FRONT_ADDRESS_KEY, address)
                if (sensorId != null) edit.putInt(TPMS_FRONT_SENSOR_ID_KEY, sensorId)
            } else {
                edit.putString(TPMS_REAR_NAME_KEY, name)
                    .putString(TPMS_REAR_ADDRESS_KEY, address)
                if (sensorId != null) edit.putInt(TPMS_REAR_SENSOR_ID_KEY, sensorId)
            }
            edit.apply()
        }

        fun setTpmsFrontSensor(
            context: Context,
            name: String,
            address: String,
            sensorId: Int
        ) = setTpmsWheelSensor(context, front = true, name, address, sensorId)

        fun setTpmsRearSensor(
            context: Context,
            name: String,
            address: String,
            sensorId: Int
        ) = setTpmsWheelSensor(context, front = false, name, address, sensorId)

        fun getTpmsFrontSensorId(context: Context): Int =
            prefs(context).getInt(TPMS_FRONT_SENSOR_ID_KEY, Int.MIN_VALUE)

        fun getTpmsRearSensorId(context: Context): Int =
            prefs(context).getInt(TPMS_REAR_SENSOR_ID_KEY, Int.MIN_VALUE)

        /** Pecham MAC hash 등 음수 ID 가능 — contains 로 등록 여부 판단 */
        fun hasTpmsFrontSensor(context: Context): Boolean =
            prefs(context).contains(TPMS_FRONT_SENSOR_ID_KEY)

        fun hasTpmsRearSensor(context: Context): Boolean =
            prefs(context).contains(TPMS_REAR_SENSOR_ID_KEY)

        fun getTpmsFrontName(context: Context): String? {
            val pref = prefs(context)
            return pref.getString(TPMS_FRONT_NAME_KEY, null)
                ?: pref.getString(TPMS_DEVICE_NAME_KEY, null)
        }

        fun getTpmsFrontAddress(context: Context): String? {
            val pref = prefs(context)
            return pref.getString(TPMS_FRONT_ADDRESS_KEY, null)
                ?: pref.getString(TPMS_ADDRESS_KEY, null)
        }

        fun getTpmsRearName(context: Context): String? {
            return prefs(context)
                .getString(TPMS_REAR_NAME_KEY, null)
        }

        fun getTpmsRearAddress(context: Context): String? {
            return prefs(context)
                .getString(TPMS_REAR_ADDRESS_KEY, null)
        }

        /** @deprecated Use front/rear setters. Kept for migration. */
        fun setTpmsDevice(context: Context, name: String, address: String) {
            setTpmsWheelSensor(context, front = true, name, address, sensorId = null)
        }

        fun getTpmsDeviceName(context: Context): String? = getTpmsFrontName(context)

        fun getTpmsAddress(context: Context): String? = getTpmsFrontAddress(context)

        fun setTpmsUnit(context: Context, unit: TpmsPressureUnit) {
            prefs(context)
                .edit().putString(TPMS_UNIT_KEY, unit.label).apply()
        }

        fun getTpmsUnit(context: Context): TpmsPressureUnit {
            val label = prefs(context)
                .getString(TPMS_UNIT_KEY, TpmsPressureUnit.BAR.label)
            return TpmsPressureUnit.fromLabel(label ?: TpmsPressureUnit.BAR.label)
        }

        fun setTpmsPressureMin(context: Context, value: Int) {
            val v = value.coerceIn(
                TpmsProtocol.LOW_PRESSURE_MIN_KPA,
                TpmsProtocol.LOW_PRESSURE_MAX_KPA
            )
            prefs(context)
                .edit().putInt(TPMS_PRESSURE_MIN_KEY, v).apply()
        }

        fun getTpmsPressureMin(context: Context): Int {
            return prefs(context)
                .getInt(TPMS_PRESSURE_MIN_KEY, 180)
        }

        fun setTpmsPressureMax(context: Context, value: Int) {
            val v = value.coerceIn(
                TpmsProtocol.HIGH_PRESSURE_MIN_KPA,
                TpmsProtocol.HIGH_PRESSURE_MAX_KPA
            )
            prefs(context)
                .edit().putInt(TPMS_PRESSURE_MAX_KEY, v).apply()
        }

        fun getTpmsPressureMax(context: Context): Int {
            return prefs(context)
                .getInt(TPMS_PRESSURE_MAX_KEY, 320)
        }

        fun setTpmsTemperatureMax(context: Context, value: Int) {
            val v = value.coerceIn(TpmsProtocol.TEMP_MIN, TpmsProtocol.TEMP_MAX)
            prefs(context)
                .edit().putInt(TPMS_TEMP_MAX_KEY, v).apply()
        }

        fun getTpmsTemperatureMax(context: Context): Int {
            return prefs(context)
                .getInt(TPMS_TEMP_MAX_KEY, 90)
        }

        fun hasTpmsSensorsConfigured(context: Context): Boolean =
            hasTpmsFrontSensor(context) || hasTpmsRearSensor(context)

        fun saveTpmsLastReading(
            context: Context,
            frontPressureRaw: Int,
            rearPressureRaw: Int,
            frontTempC: Int,
            rearTempC: Int,
            frontBatteryPct: Int = -1,
            rearBatteryPct: Int = -1
        ) {
            prefs(context)
                .edit()
                .putInt(TPMS_LAST_FRONT_PRESSURE_KEY, frontPressureRaw)
                .putInt(TPMS_LAST_REAR_PRESSURE_KEY, rearPressureRaw)
                .putInt(TPMS_LAST_FRONT_TEMP_KEY, frontTempC)
                .putInt(TPMS_LAST_REAR_TEMP_KEY, rearTempC)
                .putInt(TPMS_LAST_FRONT_BATTERY_KEY, frontBatteryPct)
                .putInt(TPMS_LAST_REAR_BATTERY_KEY, rearBatteryPct)
                .putLong(TPMS_LAST_READ_AT_KEY, System.currentTimeMillis())
                .apply()
        }

        private fun prefInt(context: Context, key: String, default: Int = -1): Int =
            prefs(context).getInt(key, default)

        fun getTpmsLastFrontPressure(context: Context): Int =
            prefInt(context, TPMS_LAST_FRONT_PRESSURE_KEY)

        fun getTpmsLastRearPressure(context: Context): Int =
            prefInt(context, TPMS_LAST_REAR_PRESSURE_KEY)

        fun getTpmsLastFrontTemp(context: Context): Int =
            prefInt(context, TPMS_LAST_FRONT_TEMP_KEY)

        fun getTpmsLastRearTemp(context: Context): Int =
            prefInt(context, TPMS_LAST_REAR_TEMP_KEY)

        fun getTpmsLastFrontBattery(context: Context): Int =
            prefInt(context, TPMS_LAST_FRONT_BATTERY_KEY)

        fun getTpmsLastRearBattery(context: Context): Int =
            prefInt(context, TPMS_LAST_REAR_BATTERY_KEY)

        fun getTpmsLastReadAt(context: Context): Long =
            prefs(context).getLong(TPMS_LAST_READ_AT_KEY, 0L)

        /**
         * TPMS Settings 화면 Save — MainActivity가 읽는 설정을 한 번에 저장.
         */
        fun saveTpmsSettings(
            context: Context,
            enabled: Boolean,
            frontName: String,
            frontAddress: String,
            frontSensorId: Int,
            rearName: String,
            rearAddress: String,
            rearSensorId: Int,
            unit: TpmsPressureUnit,
            pressureMin: Int,
            pressureMax: Int,
            temperatureMax: Int
        ) {
            val low = pressureMin.coerceIn(
                TpmsProtocol.LOW_PRESSURE_MIN_KPA,
                TpmsProtocol.LOW_PRESSURE_MAX_KPA
            )
            val high = pressureMax.coerceIn(
                TpmsProtocol.HIGH_PRESSURE_MIN_KPA,
                TpmsProtocol.HIGH_PRESSURE_MAX_KPA
            )
            val temp = temperatureMax.coerceIn(TpmsProtocol.TEMP_MIN, TpmsProtocol.TEMP_MAX)
            prefs(context)
                .edit()
                .putBoolean(TPMS_ENABLED_KEY, enabled)
                .putString(TPMS_FRONT_NAME_KEY, frontName)
                .putString(TPMS_FRONT_ADDRESS_KEY, frontAddress)
                .putInt(TPMS_FRONT_SENSOR_ID_KEY, frontSensorId)
                .putString(TPMS_REAR_NAME_KEY, rearName)
                .putString(TPMS_REAR_ADDRESS_KEY, rearAddress)
                .putInt(TPMS_REAR_SENSOR_ID_KEY, rearSensorId)
                .putString(TPMS_UNIT_KEY, unit.label)
                .putInt(TPMS_PRESSURE_MIN_KEY, low)
                .putInt(TPMS_PRESSURE_MAX_KEY, high)
                .putInt(TPMS_TEMP_MAX_KEY, temp)
                .apply()
        }
    }
}
