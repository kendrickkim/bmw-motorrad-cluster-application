package com.dooderge.bmw.motorrad.cluster

import android.content.Context
import android.content.SharedPreferences
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

        fun saveSettings(context: Context, deviceName: String, address: String) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(DEVICE_NAME_KEY, deviceName)
            editor.putString(ADDRESS_KEY, address)
            editor.apply()
        }

        fun getDeviceName(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(DEVICE_NAME_KEY, null)
        }

        fun getAddress(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(ADDRESS_KEY, null)
        }

        fun getOverlayPosition(context: Context): Pair<Int, Int> {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val x = pref.getInt(OVERLAY_X_KEY, 0)
            val y = pref.getInt(OVERLAY_Y_KEY, 0)
            return Pair(x, y)
        }

        fun setOverlayPosition(context: Context, x: Int, y: Int) {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            pref.edit().putInt(OVERLAY_X_KEY, x).putInt(OVERLAY_Y_KEY, y).apply()
        }

        fun isLargeOverlay(context: Context): Boolean {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getBoolean(LARGE_OVERLAY_KEY, false)
        }

        fun setLargeOverlay(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(LARGE_OVERLAY_KEY, enabled).apply()
        }

        fun setWheelRpmToSpeedFactor(context: Context, factor: Float) {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            pref.edit().putFloat("wheelRpmToSpeedFactor", factor).apply()
        }

        fun getWheelRpmToSpeedFactor(context: Context): Float {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return pref.getFloat("wheelRpmToSpeedFactor", BleService.WHEEL_RPM_TO_SPEED)
        }

        fun setLinTestEnabled(context: Context, enabled: Boolean) {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            pref.edit().putBoolean("linTestEnabled", enabled).apply()
        }

        fun isLinTestEnabled(context: Context): Boolean {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return pref.getBoolean("linTestEnabled", false)
        }

        fun setLinLogging(context: Context, enabled: Boolean) {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            pref.edit().putBoolean("linLogging", enabled).apply()
        }

        fun isLinLogging(context: Context): Boolean {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return pref.getBoolean("linLogging", false)
        }

        fun restoreIgnitionTrip(context: Context, status: CarStatus) {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            status.tripActive.value = pref.getBoolean("tripActive", false)
            status.tripStartedAtMillis.value = pref.getLong("tripStartedAt", 0L)
            status.tripBaseOdometerKm.value = pref.getInt("tripBaseKm", 0)
            status.tripBaseLatched.value = pref.getBoolean("tripBaseLatched", false)
            status.tripBBaseOdometerKm.value = pref.getInt("tripBBaseKm", 0)
            status.tripBBaseLatched.value = pref.getBoolean("tripBBaseLatched", false)
        }

        fun updateIgnitionTrip(context: Context, status: CarStatus) {
            // TripB: 최초 유효 적산거리가 오면 기준을 잡고 시동과 무관하게 유지
            if (!status.tripBBaseLatched.value && status.odometerKm.value > 0) {
                status.tripBBaseOdometerKm.value = status.odometerKm.value
                status.tripBBaseLatched.value = true
                saveTripB(context, status)
            }

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

        private fun saveIgnitionTrip(context: Context, status: CarStatus) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("tripActive", status.tripActive.value)
                .putLong("tripStartedAt", status.tripStartedAtMillis.value)
                .putInt("tripBaseKm", status.tripBaseOdometerKm.value)
                .putBoolean("tripBaseLatched", status.tripBaseLatched.value)
                .apply()
        }

        private fun saveTripB(context: Context, status: CarStatus) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("tripBBaseKm", status.tripBBaseOdometerKm.value)
                .putBoolean("tripBBaseLatched", status.tripBBaseLatched.value)
                .apply()
        }

        /** TripB 수동 리셋 (적산거리 기준으로 다시 시작) */
        fun resetTripB(context: Context, status: CarStatus) {
            status.tripBBaseOdometerKm.value = status.odometerKm.value
            status.tripBBaseLatched.value = status.odometerKm.value > 0
            saveTripB(context, status)
        }

        fun setTpmsEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(TPMS_ENABLED_KEY, enabled).apply()
        }

        fun isTpmsEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getBoolean(TPMS_ENABLED_KEY, false)
        }

        fun setTpmsFrontDevice(context: Context, name: String, address: String) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TPMS_FRONT_NAME_KEY, name)
                .putString(TPMS_FRONT_ADDRESS_KEY, address)
                .apply()
        }

        fun setTpmsRearDevice(context: Context, name: String, address: String) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TPMS_REAR_NAME_KEY, name)
                .putString(TPMS_REAR_ADDRESS_KEY, address)
                .apply()
        }

        fun setTpmsFrontSensor(
            context: Context,
            name: String,
            address: String,
            sensorId: Int
        ) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TPMS_FRONT_NAME_KEY, name)
                .putString(TPMS_FRONT_ADDRESS_KEY, address)
                .putInt(TPMS_FRONT_SENSOR_ID_KEY, sensorId)
                .apply()
        }

        fun setTpmsRearSensor(
            context: Context,
            name: String,
            address: String,
            sensorId: Int
        ) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TPMS_REAR_NAME_KEY, name)
                .putString(TPMS_REAR_ADDRESS_KEY, address)
                .putInt(TPMS_REAR_SENSOR_ID_KEY, sensorId)
                .apply()
        }

        fun getTpmsFrontSensorId(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_FRONT_SENSOR_ID_KEY, Int.MIN_VALUE)

        fun getTpmsRearSensorId(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_REAR_SENSOR_ID_KEY, Int.MIN_VALUE)

        /** Pecham MAC hash 등 음수 ID 가능 — contains 로 등록 여부 판단 */
        fun hasTpmsFrontSensor(context: Context): Boolean =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .contains(TPMS_FRONT_SENSOR_ID_KEY)

        fun hasTpmsRearSensor(context: Context): Boolean =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .contains(TPMS_REAR_SENSOR_ID_KEY)

        fun getTpmsFrontName(context: Context): String? {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return pref.getString(TPMS_FRONT_NAME_KEY, null)
                ?: pref.getString(TPMS_DEVICE_NAME_KEY, null)
        }

        fun getTpmsFrontAddress(context: Context): String? {
            val pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            return pref.getString(TPMS_FRONT_ADDRESS_KEY, null)
                ?: pref.getString(TPMS_ADDRESS_KEY, null)
        }

        fun getTpmsRearName(context: Context): String? {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(TPMS_REAR_NAME_KEY, null)
        }

        fun getTpmsRearAddress(context: Context): String? {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(TPMS_REAR_ADDRESS_KEY, null)
        }

        /** @deprecated Use front/rear setters. Kept for migration. */
        fun setTpmsDevice(context: Context, name: String, address: String) {
            setTpmsFrontDevice(context, name, address)
        }

        fun getTpmsDeviceName(context: Context): String? = getTpmsFrontName(context)

        fun getTpmsAddress(context: Context): String? = getTpmsFrontAddress(context)

        fun setTpmsUnit(context: Context, unit: TpmsPressureUnit) {
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putString(TPMS_UNIT_KEY, unit.label).apply()
        }

        fun getTpmsUnit(context: Context): TpmsPressureUnit {
            val label = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(TPMS_UNIT_KEY, TpmsPressureUnit.BAR.label)
            return TpmsPressureUnit.fromLabel(label ?: TpmsPressureUnit.BAR.label)
        }

        fun setTpmsPressureMin(context: Context, value: Int) {
            val v = value.coerceIn(
                TpmsProtocol.LOW_PRESSURE_MIN_KPA,
                TpmsProtocol.LOW_PRESSURE_MAX_KPA
            )
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putInt(TPMS_PRESSURE_MIN_KEY, v).apply()
        }

        fun getTpmsPressureMin(context: Context): Int {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_PRESSURE_MIN_KEY, 180)
        }

        fun setTpmsPressureMax(context: Context, value: Int) {
            val v = value.coerceIn(
                TpmsProtocol.HIGH_PRESSURE_MIN_KPA,
                TpmsProtocol.HIGH_PRESSURE_MAX_KPA
            )
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putInt(TPMS_PRESSURE_MAX_KEY, v).apply()
        }

        fun getTpmsPressureMax(context: Context): Int {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_PRESSURE_MAX_KEY, 320)
        }

        fun setTpmsTemperatureMax(context: Context, value: Int) {
            val v = value.coerceIn(TpmsProtocol.TEMP_MIN, TpmsProtocol.TEMP_MAX)
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit().putInt(TPMS_TEMP_MAX_KEY, v).apply()
        }

        fun getTpmsTemperatureMax(context: Context): Int {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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

        fun getTpmsLastFrontPressure(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_FRONT_PRESSURE_KEY, -1)

        fun getTpmsLastRearPressure(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_REAR_PRESSURE_KEY, -1)

        fun getTpmsLastFrontTemp(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_FRONT_TEMP_KEY, -1)

        fun getTpmsLastRearTemp(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_REAR_TEMP_KEY, -1)

        fun getTpmsLastFrontBattery(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_FRONT_BATTERY_KEY, -1)

        fun getTpmsLastRearBattery(context: Context): Int =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getInt(TPMS_LAST_REAR_BATTERY_KEY, -1)

        fun getTpmsLastReadAt(context: Context): Long =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getLong(TPMS_LAST_READ_AT_KEY, 0L)

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
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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
