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
 * TPMS Advanced 방식: Advertise 스캔만으로 Front/Rear 압력 수신.
 * GATT Connect / Write / STUDY_PAIR 없음.
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
        /** @deprecated GATT 제거 — START_POLLING 과 동일 */
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
        const val EXTRA_UNIT = "unit"
        const val EXTRA_KEEP_CONNECTED = "keepConnected"

        private const val CHANNEL_ID = "tpms_channel"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "TpmsService"
        private const val POLL_INTERVAL_MS = 60L * 60L * 1000L
        private const val REFRESH_SCAN_MS = 20_000L

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

    private val hourlyPollRunnable = object : Runnable {
        override fun run() {
            if (!pollingEnabled) return
            beginRefreshScan(reason = "hourly")
            mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val stopRefreshRunnable = Runnable {
        if (!settingsUiOpen) {
            scanner?.stopScan()
            publishStatus("Waiting for the next scan")
        }
    }

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
            startPollingSchedule(runImmediately = true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN -> {
                settingsUiOpen = true
                scanner?.clearScanList()
                scanner?.startScan()
            }
            ACTION_STOP_SCAN -> {
                if (!pollingEnabled || !settingsUiOpen) {
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
                startPollingSchedule(runImmediately = true)
            }
            ACTION_REFRESH -> beginRefreshScan(reason = "manual")
            null -> Unit
        }
        return START_STICKY
    }

    /** 설정 화면에서 센서 ID 바인딩 후 호출 */
    fun bindSensor(position: TirePosition, sample: ScannedTpmsDevice) {
        when (position) {
            TirePosition.FRONT -> {
                SettingPreference.setTpmsFrontSensor(
                    this,
                    name = "${sample.brand.name} ${sample.name}",
                    address = sample.address,
                    sensorId = sample.sensorId
                )
            }
            TirePosition.REAR -> {
                SettingPreference.setTpmsRearSensor(
                    this,
                    name = "${sample.brand.name} ${sample.name}",
                    address = sample.address,
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
                macAddress = sample.address,
                advLocation = sample.suggestedSlot?.let {
                    if (it == TirePosition.FRONT) AdvWheelLocation.FRONT_LEFT
                    else AdvWheelLocation.REAR_LEFT
                }
            )
        )
    }

    fun bindSensorId(position: TirePosition, sensorId: Int, label: String = "") {
        val hex = TpmsProtocol.sensorIdToHex(sensorId)
        when (position) {
            TirePosition.FRONT ->
                SettingPreference.setTpmsFrontSensor(this, label.ifBlank { "Sensor" }, hex, sensorId)
            TirePosition.REAR ->
                SettingPreference.setTpmsRearSensor(this, label.ifBlank { "Sensor" }, hex, sensorId)
        }
        seedFromPrefs()
        // QR만 등록하고 스캔이 꺼져 있으면 수신 불가 → 즉시 스캔
        settingsUiOpen = true
        beginRefreshScan(reason = "bind")
    }

    override fun onDestroy() {
        stopPollingSchedule()
        mainHandler.removeCallbacks(stopRefreshRunnable)
        scanner?.release()
        scanner = null
        super.onDestroy()
    }

    private fun startPollingSchedule(runImmediately: Boolean) {
        if (!SettingPreference.hasTpmsSensorsConfigured(this)) {
            Log.i(TAG, "polling skipped — no sensors")
            return
        }
        pollingEnabled = true
        mainHandler.removeCallbacks(hourlyPollRunnable)
        if (runImmediately) beginRefreshScan(reason = "boot")
        mainHandler.postDelayed(hourlyPollRunnable, POLL_INTERVAL_MS)
        Log.i(TAG, "polling started")
    }

    private fun stopPollingSchedule() {
        pollingEnabled = false
        mainHandler.removeCallbacks(hourlyPollRunnable)
        mainHandler.removeCallbacks(stopRefreshRunnable)
    }

    private fun beginRefreshScan(reason: String) {
        if (!SettingPreference.hasTpmsSensorsConfigured(this) && !settingsUiOpen) return
        publishStatus("Scanning TPMS… ($reason)")
        scanner?.startScan()
        mainHandler.removeCallbacks(stopRefreshRunnable)
        if (!settingsUiOpen) {
            mainHandler.postDelayed(stopRefreshRunnable, REFRESH_SCAN_MS)
        }
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
            if (sample.sensorId == boundId) return true
            // QR 로 저장한 6hex 와 advertise ID hex 비교 (엔디안/표시 불일치 대비)
            val hex = sample.sensorIdHex.uppercase()
            if (!boundHex.isNullOrBlank() && boundHex.length == 6 &&
                hex.equals(boundHex, ignoreCase = true)
            ) {
                return true
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
                    name = sample.brand.name,
                    address = sample.macAddress.ifBlank { latestState.frontDevice.address }
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
                    name = sample.brand.name,
                    address = sample.macAddress.ifBlank { latestState.rearDevice.address }
                ),
                statusMessage = "Rear ${sample.sensorIdHex} received"
            )
        }

        // 미바인딩 + Sysgration 위치 힌트 (미리보기)
        if (!matched && sample.advLocation != null) {
            val slot = sample.advLocation.toClusterSlot()
            if (slot == TirePosition.FRONT && !hasFront) {
                latestState = latestState.copy(
                    front = toReading(slot, sample),
                    statusMessage = "Unbound front candidate ${sample.sensorIdHex}"
                )
                matched = true
            } else if (slot == TirePosition.REAR && !hasRear) {
                latestState = latestState.copy(
                    rear = toReading(slot, sample),
                    statusMessage = "Unbound rear candidate ${sample.sensorIdHex}"
                )
                matched = true
            }
        }

        if (matched) {
            persistLatestReadings()
            broadcastState(latestState)
            updateNotification(latestState)
        } else {
            Log.d(
                TAG,
                "sample not bound: ${sample.brand} id=${sample.sensorId} hex=${sample.sensorIdHex} " +
                    "frontBound=$hasFront/$frontId rearBound=$hasRear/$rearId"
            )
        }
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
            rearTempC = latestState.rear.temperatureC
        )
    }

    private fun restoreCachedReadings() {
        val frontP = SettingPreference.getTpmsLastFrontPressure(this)
        val rearP = SettingPreference.getTpmsLastRearPressure(this)
        val frontT = SettingPreference.getTpmsLastFrontTemp(this)
        val rearT = SettingPreference.getTpmsLastRearTemp(this)
        latestState = latestState.copy(
            front = latestState.front.copy(
                pressureKpaCents = frontP,
                temperatureC = frontT
            ),
            rear = latestState.rear.copy(
                pressureKpaCents = rearP,
                temperatureC = rearT
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
                .setContentText("Advertise scan monitoring")
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
