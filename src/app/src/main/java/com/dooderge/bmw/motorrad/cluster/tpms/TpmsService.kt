package com.dooderge.bmw.motorrad.cluster.tpms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.SettingPreference

/**
 * LYTPMS Advertise 스캔 서비스.
 * 공식 앱(`BleServiceLY`)과 같이 GATT Connect 없이 Front/Rear 압력 수신.
 */
class TpmsService : Service() {
    companion object {
        const val ACTION_STATE =
            "com.dooderge.bmw.motorrad.cluster.TPMS_STATE_CHANGED"
        const val ACTION_SCAN =
            "com.dooderge.bmw.motorrad.cluster.TPMS_SCAN"
        const val ACTION_STOP_SCAN =
            "com.dooderge.bmw.motorrad.cluster.TPMS_STOP_SCAN"
        const val ACTION_REFRESH =
            "com.dooderge.bmw.motorrad.cluster.TPMS_REFRESH"
        const val ACTION_START_POLLING =
            "com.dooderge.bmw.motorrad.cluster.TPMS_START_POLLING"
        const val ACTION_STOP_POLLING =
            "com.dooderge.bmw.motorrad.cluster.TPMS_STOP_POLLING"
        const val ACTION_CONNECT =
            "com.dooderge.bmw.motorrad.cluster.TPMS_CONNECT"
        const val ACTION_DISCONNECT =
            "com.dooderge.bmw.motorrad.cluster.TPMS_DISCONNECT"

        const val EXTRA_WHEEL = "wheel"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_NAME = "name"
        const val EXTRA_CONNECTION = "connection"
        const val EXTRA_STATUS = "status"
        const val EXTRA_FRONT_PRESSURE = "frontPressure"
        const val EXTRA_REAR_PRESSURE = "rearPressure"
        const val EXTRA_FRONT_TEMP = "frontTemp"
        const val EXTRA_REAR_TEMP = "rearTemp"
        const val EXTRA_FRONT_BATTERY = "frontBattery"
        const val EXTRA_REAR_BATTERY = "rearBattery"
        const val EXTRA_UNIT = "unit"
        const val EXTRA_KEEP_CONNECTED = "keepConnected"

        private const val CHANNEL_ID = "tpms_channel"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "TpmsService"

        fun start(context: Context) {
            val intent = Intent(context, TpmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startPolling(context: Context) {
            val intent = Intent(context, TpmsService::class.java).apply {
                action = ACTION_START_POLLING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun refreshNow(context: Context) {
            val intent = Intent(context, TpmsService::class.java).apply {
                action = ACTION_REFRESH
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): TpmsService = this@TpmsService
    }

    private val binder = LocalBinder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanner: TpmsAdvScanner? = null
    private var pollingEnabled = false
    private var settingsUiOpen = false

    @Volatile
    var latestState: TpmsDeviceState = TpmsDeviceState()
        private set

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        ensureNotification()
        scanner = TpmsAdvScanner(
            context = this,
            onSample = { sample -> applySample(sample) },
            onScanList = { list ->
                latestState = latestState.copy(scannedDevices = list)
                broadcastState(latestState)
            },
            onState = { conn, msg ->
                val listening = conn == TpmsConnectionState.LISTENING ||
                    conn == TpmsConnectionState.SCANNING
                latestState = latestState.copy(
                    frontDevice = latestState.frontDevice.copy(
                        connection = if (listening) conn else latestState.frontDevice.connection
                    ),
                    rearDevice = latestState.rearDevice.copy(
                        connection = if (listening) conn else latestState.rearDevice.connection
                    ),
                    statusMessage = msg
                )
                broadcastState(latestState)
                updateNotification(latestState)
            }
        )
        seedFromPrefs()
        restoreCachedReadings()
        if (SettingPreference.isTpmsEnabled(this) &&
            SettingPreference.hasTpmsSensorsConfigured(this)
        ) {
            startPollingSchedule()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN -> {
                settingsUiOpen = true
                scanner?.clearScanList()
                // 설정 화면: 연속 스캔 (사이클 휴지 없음)
                scanner?.startScan()
            }
            ACTION_STOP_SCAN -> {
                settingsUiOpen = false
                if (pollingEnabled) {
                    scanner?.startCycling()
                } else {
                    scanner?.stopScan()
                }
            }
            ACTION_DISCONNECT, ACTION_STOP_POLLING -> {
                settingsUiOpen = false
                stopPollingSchedule()
                scanner?.stopScan()
                SettingPreference.setTpmsEnabled(this, false)
                publishStatus("TPMS stopped")
            }
            ACTION_CONNECT, ACTION_START_POLLING -> {
                SettingPreference.setTpmsEnabled(this, true)
                seedFromPrefs()
                startPollingSchedule()
            }
            ACTION_REFRESH -> {
                if (settingsUiOpen) {
                    scanner?.startScan()
                } else if (pollingEnabled) {
                    scanner?.startCycling()
                } else {
                    scanner?.startScan()
                }
            }
            null -> Unit
        }
        return START_STICKY
    }

    fun bindSensor(position: TirePosition, sample: ScannedTpmsDevice) {
        when (position) {
            TirePosition.FRONT -> {
                SettingPreference.setTpmsFrontSensor(
                    this,
                    name = "LYTPMS ${sample.sensorIdHex}",
                    address = sample.sensorIdHex,
                    sensorId = sample.sensorId
                )
            }
            TirePosition.REAR -> {
                SettingPreference.setTpmsRearSensor(
                    this,
                    name = "LYTPMS ${sample.sensorIdHex}",
                    address = sample.sensorIdHex,
                    sensorId = sample.sensorId
                )
            }
        }
        seedFromPrefs()
        applySample(
            AdvTireSample(
                sensorId = sample.sensorId,
                sensorIdHex = sample.sensorIdHex,
                pressureKpaCents = sample.pressureKpaCents,
                temperatureC = sample.temperatureC,
                battery = -1,
                isAlarm = false,
                rssi = sample.rssi,
                brand = sample.brand,
                macAddress = sample.address
            )
        )
    }

    fun bindSensorId(position: TirePosition, sensorId: Int, label: String = "") {
        val hex = TpmsProtocol.sensorIdToHex(sensorId)
        when (position) {
            TirePosition.FRONT ->
                SettingPreference.setTpmsFrontSensor(
                    this,
                    label.ifBlank { "LYTPMS $hex" },
                    hex,
                    sensorId
                )
            TirePosition.REAR ->
                SettingPreference.setTpmsRearSensor(
                    this,
                    label.ifBlank { "LYTPMS $hex" },
                    hex,
                    sensorId
                )
        }
        seedFromPrefs()
        settingsUiOpen = true
        scanner?.startScan()
    }

    override fun onDestroy() {
        stopPollingSchedule()
        scanner?.release()
        scanner = null
        super.onDestroy()
    }

    private fun startPollingSchedule() {
        if (!SettingPreference.hasTpmsSensorsConfigured(this)) {
            Log.i(TAG, "polling skipped — no sensors")
            return
        }
        pollingEnabled = true
        if (settingsUiOpen) {
            scanner?.startScan()
        } else {
            scanner?.startCycling()
        }
        Log.i(TAG, "LYTPMS continuous scan started")
    }

    private fun stopPollingSchedule() {
        pollingEnabled = false
    }

    private fun applySample(sample: AdvTireSample) {
        val hasFront = SettingPreference.hasTpmsFrontSensor(this)
        val hasRear = SettingPreference.hasTpmsRearSensor(this)
        val frontId = SettingPreference.getTpmsFrontSensorId(this)
        val rearId = SettingPreference.getTpmsRearSensorId(this)
        val frontHex = SettingPreference.getTpmsFrontAddress(this)
            ?.replace(":", "")?.uppercase()
        val rearHex = SettingPreference.getTpmsRearAddress(this)
            ?.replace(":", "")?.uppercase()
        var matched = false

        fun idMatches(boundId: Int, boundHex: String?): Boolean {
            // 공식 앱: TireData.id.equals(TireBean.ID) — 6hex 문자열 비교가 기준
            val sampleHex = sample.sensorIdHex.uppercase()
            if (!boundHex.isNullOrBlank()) {
                val normalized = boundHex.replace(":", "").uppercase()
                if (normalized.length == 6 && sampleHex.equals(normalized, ignoreCase = true)) {
                    return true
                }
            }
            if (boundId != Int.MIN_VALUE && sample.sensorId == boundId) return true
            // 이전 Sysgration LE 인코딩으로 저장된 ID 호환
            if (boundId != Int.MIN_VALUE) {
                val legacyHex = legacySysgrationHex(boundId)
                if (legacyHex.equals(sampleHex, ignoreCase = true)) return true
            }
            return false
        }

        if (hasFront && idMatches(frontId, frontHex)) {
            matched = true
            latestState = latestState.copy(
                front = toReading(TirePosition.FRONT, sample),
                frontDevice = latestState.frontDevice.copy(
                    connection = TpmsConnectionState.LISTENING,
                    sensorId = sample.sensorId,
                    name = "LYTPMS",
                    address = sample.sensorIdHex
                ),
                statusMessage = "Front ${sample.sensorIdHex} received"
            )
        }
        if (hasRear && idMatches(rearId, rearHex)) {
            matched = true
            latestState = latestState.copy(
                rear = toReading(TirePosition.REAR, sample),
                rearDevice = latestState.rearDevice.copy(
                    connection = TpmsConnectionState.LISTENING,
                    sensorId = sample.sensorId,
                    name = "LYTPMS",
                    address = sample.sensorIdHex
                ),
                statusMessage = "Rear ${sample.sensorIdHex} received"
            )
        }

        if (matched) {
            persistLatestReadings()
            broadcastState(latestState)
            updateNotification(latestState)
        } else {
            Log.d(
                TAG,
                "sample not bound: id=${sample.sensorIdHex} " +
                    "frontBound=$hasFront/${TpmsProtocol.sensorIdToHex(frontId)} " +
                    "rearBound=$hasRear/${TpmsProtocol.sensorIdToHex(rearId)}"
            )
        }
    }

    /** 구버전 Sysgration LE hex6ToSensorId 로 저장된 값 → 표시 hex */
    private fun legacySysgrationHex(id: Int): String {
        val buf = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putInt(id)
            .array()
        return "%02X%02X%02X".format(
            buf[1].toInt() and 0xFF,
            buf[2].toInt() and 0xFF,
            buf[3].toInt() and 0xFF
        )
    }

    private fun toReading(position: TirePosition, sample: AdvTireSample) = TireReading(
        position = position,
        pressureKpaCents = sample.pressureKpaCents,
        temperatureC = sample.temperatureC,
        battery = sample.battery,
        sensorId = sample.sensorId,
        sensorIdHex = sample.sensorIdHex,
        brand = sample.brand,
        isAlarm = sample.isAlarm,
        rssi = sample.rssi,
        updatedAtMillis = System.currentTimeMillis()
    )

    private fun persistLatestReadings() {
        SettingPreference.saveTpmsLastReading(
            this,
            frontPressureRaw = latestState.front.pressureKpaCents,
            rearPressureRaw = latestState.rear.pressureKpaCents,
            frontTempC = latestState.front.temperatureC,
            rearTempC = latestState.rear.temperatureC,
            frontBatteryPct = latestState.front.battery,
            rearBatteryPct = latestState.rear.battery
        )
    }

    private fun restoreCachedReadings() {
        val frontP = SettingPreference.getTpmsLastFrontPressure(this)
        val rearP = SettingPreference.getTpmsLastRearPressure(this)
        val frontT = SettingPreference.getTpmsLastFrontTemp(this)
        val rearT = SettingPreference.getTpmsLastRearTemp(this)
        val frontB = SettingPreference.getTpmsLastFrontBattery(this)
        val rearB = SettingPreference.getTpmsLastRearBattery(this)
        latestState = latestState.copy(
            front = latestState.front.copy(
                pressureKpaCents = frontP,
                temperatureC = frontT,
                battery = frontB
            ),
            rear = latestState.rear.copy(
                pressureKpaCents = rearP,
                temperatureC = rearT,
                battery = rearB
            ),
            statusMessage = if (frontP >= 0 || rearP >= 0) "Cached TPMS values" else latestState.statusMessage
        )
        broadcastState(latestState)
    }

    private fun publishStatus(message: String) {
        latestState = latestState.copy(statusMessage = message)
        broadcastState(latestState)
        updateNotification(latestState)
    }

    private fun seedFromPrefs() {
        latestState = latestState.copy(
            frontDevice = WheelDeviceInfo(
                name = SettingPreference.getTpmsFrontName(this).orEmpty(),
                address = SettingPreference.getTpmsFrontAddress(this).orEmpty(),
                sensorId = if (SettingPreference.hasTpmsFrontSensor(this)) {
                    SettingPreference.getTpmsFrontSensorId(this)
                } else {
                    Int.MIN_VALUE
                }
            ),
            rearDevice = WheelDeviceInfo(
                name = SettingPreference.getTpmsRearName(this).orEmpty(),
                address = SettingPreference.getTpmsRearAddress(this).orEmpty(),
                sensorId = if (SettingPreference.hasTpmsRearSensor(this)) {
                    SettingPreference.getTpmsRearSensorId(this)
                } else {
                    Int.MIN_VALUE
                }
            ),
            pressureMinKpa = SettingPreference.getTpmsPressureMin(this),
            pressureMaxKpa = SettingPreference.getTpmsPressureMax(this),
            temperatureMax = SettingPreference.getTpmsTemperatureMax(this),
            displayUnit = SettingPreference.getTpmsUnit(this)
        )
        broadcastState(latestState)
    }

    private fun broadcastState(state: TpmsDeviceState) {
        Intent(ACTION_STATE).also {
            it.setPackage(packageName)
            it.putExtra(EXTRA_CONNECTION, state.connection.name)
            it.putExtra(EXTRA_STATUS, state.statusMessage)
            it.putExtra(EXTRA_FRONT_PRESSURE, state.front.pressureKpaCents)
            it.putExtra(EXTRA_REAR_PRESSURE, state.rear.pressureKpaCents)
            it.putExtra(EXTRA_FRONT_TEMP, state.front.temperatureC)
            it.putExtra(EXTRA_REAR_TEMP, state.rear.temperatureC)
            it.putExtra(EXTRA_FRONT_BATTERY, state.front.battery)
            it.putExtra(EXTRA_REAR_BATTERY, state.rear.battery)
            it.putExtra(EXTRA_UNIT, state.displayUnit.label)
            sendBroadcast(it)
        }
    }

    private fun ensureNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TPMS service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TPMS")
                .setContentText("LYTPMS scan monitoring")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        )
    }

    private fun updateNotification(state: TpmsDeviceState) {
        val unit = state.displayUnit
        val f = state.front.pressureWithUnit(unit)
        val r = state.rear.pressureWithUnit(unit)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TPMS")
            .setContentText("F $f / R $r")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
