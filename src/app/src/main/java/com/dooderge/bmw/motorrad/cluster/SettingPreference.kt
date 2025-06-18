package com.dooderge.bmw.motorrad.cluster

import android.content.Context
import android.content.SharedPreferences

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

    }
}
