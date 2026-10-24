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
 * TPMS Advanced 스타일 Advertise 스캐너 (GATT Connect 없음).
 *
 * UUID 필터만 쓰면 일부 펌웨어가 목록에 안 잡히므로,
 * 기본은 **무필터 스캔 + 소프트웨어 파싱** (오픈소스 UUID 필터는 보조).
 */
class TpmsAdvScanner(
    private val context: Context,
    private val onSample: (AdvTireSample) -> Unit,
    private val onScanList: (List<ScannedTpmsDevice>) -> Unit = {},
    private val onState: (TpmsConnectionState, String) -> Unit = { _, _ -> }
) {
    companion object {
        private const val TAG = "TpmsAdvScanner"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val seen = LinkedHashMap<Int, ScannedTpmsDevice>()
    private var scanning = false
    private var leScanner: android.bluetooth.le.BluetoothLeScanner? = null

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
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(mode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY) {
        // 이미 스캔 중이면 재시작 (설정 화면에서 목록이 비는 문제 방지)
        if (scanning) {
            stopScanInternal()
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter
        if (adapter == null || !adapter.isEnabled) {
            onState(TpmsConnectionState.ERROR, "Bluetooth is off")
            return
        }
        leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            onState(TpmsConnectionState.ERROR, "BLE Scanner null")
            return
        }
        val settings = ScanSettings.Builder()
            .setScanMode(mode)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()
        try {
            // 무필터: 센서가 Service UUID 를 광고에 안 넣는 경우도 수신
            leScanner?.startScan(null, settings, callback)
            scanning = true
            onState(TpmsConnectionState.SCANNING, "Scanning for TPMS…")
            Log.i(TAG, "scan started (no filter)")
        } catch (e: SecurityException) {
            Log.e(TAG, "scan permission", e)
            onState(TpmsConnectionState.ERROR, "Scan permission missing")
        } catch (e: Exception) {
            Log.e(TAG, "unfiltered startScan failed, try UUID filters", e)
            try {
                val filters = TpmsBleUuids.SCAN_SERVICE_UUIDS.map {
                    ScanFilter.Builder().setServiceUuid(it).build()
                }
                leScanner?.startScan(filters, settings, callback)
                scanning = true
                onState(TpmsConnectionState.SCANNING, "Scanning for TPMS… (UUID)")
                Log.i(TAG, "scan started (UUID filters)")
            } catch (e2: Exception) {
                Log.e(TAG, "startScan", e2)
                onState(TpmsConnectionState.ERROR, "Could not start scan")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
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
        stopScanInternal()
        seen.clear()
    }

    private fun handleResult(result: ScanResult) {
        val sample = TpmsAdvParsers.parse(result) ?: return
        mainHandler.post {
            onSample(sample)
            val device = ScannedTpmsDevice(
                name = result.scanRecord?.deviceName
                    ?: sample.brand.name,
                address = sample.macAddress.ifBlank {
                    result.device?.address.orEmpty()
                },
                rssi = sample.rssi,
                sensorId = sample.sensorId,
                sensorIdHex = sample.sensorIdHex,
                brand = sample.brand,
                suggestedSlot = sample.suggestedSlot(),
                pressureKpaCents = sample.pressureKpaCents,
                temperatureC = sample.temperatureC
            )
            seen[sample.sensorId] = device
            onScanList(seen.values.sortedByDescending { it.rssi })
            if (scanning) {
                onState(
                    TpmsConnectionState.LISTENING,
                    "Received ${seen.size} · ${sample.brand} ${sample.sensorIdHex}"
                )
            }
        }
    }
}
