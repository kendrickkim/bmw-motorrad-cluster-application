package com.dooderge.bmw.motorrad.cluster.tpms

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * LYTPMS 공식 앱(`BaseTireBleService`)과 동일한 Advertise 스캔.
 *
 * - 무필터 LE scan (`ScanFilter` empty)
 * - `SCAN_MODE_LOW_LATENCY`
 * - 연속 수신 시 60초 스캔 → 2초 휴지 → 재스캔 사이클
 */
class TpmsAdvScanner(
    private val context: Context,
    private val onSample: (AdvTireSample) -> Unit,
    private val onScanList: (List<ScannedTpmsDevice>) -> Unit = {},
    private val onState: (TpmsConnectionState, String) -> Unit = { _, _ -> }
) {
    companion object {
        private const val TAG = "TpmsAdvScanner"
        /** Official: stop after 60_000ms then restart after 2_000ms */
        const val SCAN_BURST_MS = 60_000L
        const val SCAN_PAUSE_MS = 2_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val seen = LinkedHashMap<Int, ScannedTpmsDevice>()
    private var scanning = false
    private var cycleEnabled = false
    private var leScanner: android.bluetooth.le.BluetoothLeScanner? = null

    private val stopBurstRunnable = Runnable {
        stopScanInternal()
        onState(TpmsConnectionState.LISTENING, "Scan pause…")
        if (cycleEnabled) {
            mainHandler.postDelayed(restartBurstRunnable, SCAN_PAUSE_MS)
        }
    }

    private val restartBurstRunnable = Runnable {
        if (cycleEnabled) startBurst()
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed code=$errorCode")
            scanning = false
            onState(TpmsConnectionState.ERROR, "Scan failed ($errorCode)")
            // Official: retry after 2s (or 8s if error 2)
            val delay = if (errorCode == 2) 8_000L else SCAN_PAUSE_MS
            if (cycleEnabled) {
                mainHandler.postDelayed(restartBurstRunnable, delay)
            }
        }
    }

    /** 설정 UI / 단발 스캔: 사이클 없이 연속 스캔 */
    @SuppressLint("MissingPermission")
    fun startScan(mode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) {
        cycleEnabled = false
        mainHandler.removeCallbacks(stopBurstRunnable)
        mainHandler.removeCallbacks(restartBurstRunnable)
        startBurst(mode)
    }

    /** 폴링 ON: 공식 앱과 동일 60s/2s 사이클 */
    fun startCycling() {
        cycleEnabled = true
        mainHandler.removeCallbacks(stopBurstRunnable)
        mainHandler.removeCallbacks(restartBurstRunnable)
        startBurst()
    }

    @SuppressLint("MissingPermission")
    private fun startBurst(mode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) {
        if (scanning) {
            stopScanInternal()
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter
        if (adapter == null || !adapter.isEnabled) {
            onState(TpmsConnectionState.ERROR, "Bluetooth is off")
            if (cycleEnabled) {
                mainHandler.postDelayed(restartBurstRunnable, 3_000L)
            }
            return
        }
        leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            onState(TpmsConnectionState.ERROR, "BLE Scanner null")
            return
        }
        val settings = ScanSettings.Builder()
            .setScanMode(mode)
            .setReportDelay(0)
            .build()
        try {
            // Official: empty ScanFilter list
            val filters = listOf(ScanFilter.Builder().build())
            leScanner?.startScan(filters, settings, callback)
            scanning = true
            onState(TpmsConnectionState.SCANNING, "Scanning TPMS…")
            Log.i(TAG, "scan burst started cycle=$cycleEnabled")
            if (cycleEnabled) {
                mainHandler.postDelayed(stopBurstRunnable, SCAN_BURST_MS)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "scan permission", e)
            onState(TpmsConnectionState.ERROR, "Scan permission missing")
        } catch (e: Exception) {
            Log.e(TAG, "startScan failed", e)
            onState(TpmsConnectionState.ERROR, "Could not start scan")
            if (cycleEnabled) {
                mainHandler.postDelayed(restartBurstRunnable, 5_000L)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        cycleEnabled = false
        mainHandler.removeCallbacks(stopBurstRunnable)
        mainHandler.removeCallbacks(restartBurstRunnable)
        stopScanInternal()
        onState(TpmsConnectionState.DISCONNECTED, "Scan stopped")
        Log.i(TAG, "scan stopped")
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!scanning) return
        try {
            leScanner?.flushPendingScanResults(callback)
            leScanner?.stopScan(callback)
        } catch (_: Exception) {
        }
        scanning = false
    }

    fun isScanning(): Boolean = scanning

    fun clearScanList() {
        seen.clear()
        onScanList(emptyList())
    }

    fun release() {
        cycleEnabled = false
        mainHandler.removeCallbacks(stopBurstRunnable)
        mainHandler.removeCallbacks(restartBurstRunnable)
        stopScanInternal()
        seen.clear()
    }

    private fun handleResult(result: ScanResult) {
        val sample = TpmsAdvParsers.parse(result) ?: return
        mainHandler.post {
            onSample(sample)
            val device = ScannedTpmsDevice(
                name = result.scanRecord?.deviceName ?: "LYTPMS",
                address = sample.macAddress.ifBlank {
                    result.device?.address.orEmpty()
                },
                rssi = sample.rssi,
                sensorId = sample.sensorId,
                sensorIdHex = sample.sensorIdHex,
                brand = sample.brand,
                suggestedSlot = null,
                pressureKpaCents = sample.pressureKpaCents,
                temperatureC = sample.temperatureC
            )
            seen[sample.sensorId] = device
            onScanList(seen.values.sortedByDescending { it.rssi })
            if (scanning || cycleEnabled) {
                onState(
                    TpmsConnectionState.LISTENING,
                    "Received ${seen.size} · ${sample.sensorIdHex}"
                )
            }
        }
    }
}
