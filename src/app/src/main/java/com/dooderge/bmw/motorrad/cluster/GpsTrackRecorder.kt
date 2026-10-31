package com.dooderge.bmw.motorrad.cluster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 차량 속도 10 km/h 이상일 때 5초마다 GPS(시간·위도·경도·고도·속도·RPM)를 CSV로 기록.
 * 시각·파일 일자는 KST(Asia/Seoul) 기준.
 */
class GpsTrackRecorder(
    private val context: Context,
    private val speedKmH: () -> Int,
    private val rpm: () -> Int,
) {
    private val logger = GpsTrackLogger(context)
    private val handler = Handler(Looper.getMainLooper())
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var running = false

    private val locationListener = LocationListener { location ->
        lastLocation = location
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            tryRecord()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        if (!hasLocationPermission()) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = manager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return
        try {
            manager.requestLocationUpdates(provider, 1000L, 0f, locationListener)
            lastLocation = manager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            return
        }
        running = true
        handler.post(tickRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
        locationManager?.removeUpdates(locationListener)
        locationManager = null
        lastLocation = null
        logger.release()
    }

    private fun tryRecord() {
        val speed = speedKmH()
        if (speed < MIN_SPEED_KMH) return
        val location = lastLocation ?: return
        val altitude = if (location.hasAltitude()) location.altitude else 0.0
        logger.append(
            time = ZonedDateTime.now(KST),
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = altitude,
            speedKmh = speed,
            rpm = rpm()
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
        private const val MIN_SPEED_KMH = 10
        private const val INTERVAL_MS = 5_000L
    }
}
