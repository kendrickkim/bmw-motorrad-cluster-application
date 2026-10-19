package com.dooderge.bmw.motorrad.cluster

import android.content.Context
import android.content.SharedPreferences
import com.dooderge.bmw.motorrad.cluster.ui.screens.MainScreen

class SettingPreference private constructor() {
    companion object {
        private const val PREFERENCE_NAME = "settings"
        private const val DEVICE_NAME_KEY = "deviceName"
        private const val ADDRESS_KEY = "address"
        private const val OVERLAY_X_KEY = "overlayX"
        private const val OVERLAY_Y_KEY = "overlayY"

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
        }

        fun updateIgnitionTrip(context: Context, status: CarStatus) {
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

    }
}
