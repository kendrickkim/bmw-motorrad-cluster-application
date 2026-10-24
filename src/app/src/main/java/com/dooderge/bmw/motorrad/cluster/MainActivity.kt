package com.dooderge.bmw.motorrad.cluster

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.dooderge.bmw.motorrad.cluster.launcher.LauncherActivity
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsService
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsSettingActivity
import com.dooderge.bmw.motorrad.cluster.ui.screens.MainScreen
import com.dooderge.bmw.motorrad.cluster.ui.theme.BMWMotorradClusterTheme

class MainActivity : ComponentActivity() {

    private var carStatus = CarStatus()

    private val HEADUNIT_PACKAGE_NAME = "com.andrerinas.headunitrevived" // gb.xxy.hr

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            // WiFi 관련 권한
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,

            // BLE 관련 권한
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,

            // 알림 권한
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            // WiFi 관련 권한
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,

            // 위치 권한
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startServices()
        } else {
            Toast.makeText(this, "Required permissions were not granted.", Toast.LENGTH_LONG).show()
        }
    }

    private var isBleReceiverRegistered = false

    private fun registerBleReceiver() {
        if (isBleReceiverRegistered) return

        val intentFilter = IntentFilter().apply {
            addAction(BleService.ACTION_CAR_STATUS_DATA_FILTER)
            addAction(BleService.ACTION_CHECK_INPUT_METHOD)
            addAction(BleService.ACTION_BT_CONNECTION_STATE_CHANGED)
            addAction(TpmsService.ACTION_STATE)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bleDataReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(bleDataReceiver, intentFilter)
            }
            isBleReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("MainActivity", "Error registering receiver", e)
        }
    }

    private fun unregisterBleReceiver() {
        if (!isBleReceiverRegistered) return
        try {
            unregisterReceiver(bleDataReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering receiver", e)
        } finally {
            isBleReceiverRegistered = false
        }
    }

    private val bleDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TpmsService.ACTION_STATE) {
                applyTpmsBroadcast(intent)
                return
            }
            if (intent?.action == BleService.ACTION_CAR_STATUS_DATA_FILTER) {
                var json = intent.getStringExtra("car_status").toString()
                var newCarStatus = CarStatus()
                newCarStatus.fromJson(json)
                carStatus.copyFrom(newCarStatus)
                carStatus.setBtStatus(true)
                SettingPreference.updateIgnitionTrip(this@MainActivity, carStatus)
            } else if (intent?.action == BleService.ACTION_CHECK_INPUT_METHOD) {
                carStatus.setBtStatus(false)
            } else if (intent?.action == BleService.ACTION_BT_CONNECTION_STATE_CHANGED) {
                val connected = intent.getBooleanExtra(BleService.EXTRA_BT_CONNECTED, false)
                carStatus.setBtStatus(connected)
            }
        }
    }

    private fun applyTpmsBroadcast(intent: Intent) {
        carStatus.tpmsFrontPressureRaw.value =
            intent.getIntExtra(TpmsService.EXTRA_FRONT_PRESSURE, -1)
        carStatus.tpmsRearPressureRaw.value =
            intent.getIntExtra(TpmsService.EXTRA_REAR_PRESSURE, -1)
        carStatus.tpmsFrontTempC.value =
            intent.getIntExtra(TpmsService.EXTRA_FRONT_TEMP, -1)
        carStatus.tpmsRearTempC.value =
            intent.getIntExtra(TpmsService.EXTRA_REAR_TEMP, -1)
        carStatus.tpmsFrontBatteryPct.value =
            intent.getIntExtra(TpmsService.EXTRA_FRONT_BATTERY, -1)
        carStatus.tpmsRearBatteryPct.value =
            intent.getIntExtra(TpmsService.EXTRA_REAR_BATTERY, -1)
        carStatus.tpmsUnitLabel.value =
            intent.getStringExtra(TpmsService.EXTRA_UNIT) ?: SettingPreference.getTpmsUnit(this).label
        carStatus.tpmsConnection.value =
            intent.getStringExtra(TpmsService.EXTRA_CONNECTION) ?: "DISCONNECTED"
        carStatus.tpmsStatusMessage.value =
            intent.getStringExtra(TpmsService.EXTRA_STATUS).orEmpty()
    }

    private fun loadTpmsSettingsIntoCarStatus() {
        carStatus.tpmsEnabled.value = SettingPreference.isTpmsEnabled(this)
        carStatus.tpmsUnitLabel.value = SettingPreference.getTpmsUnit(this).label
        carStatus.tpmsPressureMinKpa.value = SettingPreference.getTpmsPressureMin(this)
        carStatus.tpmsPressureMaxKpa.value = SettingPreference.getTpmsPressureMax(this)
        // 부팅 직후·폴링 전에도 마지막 조회값을 표시
        carStatus.tpmsFrontPressureRaw.value =
            SettingPreference.getTpmsLastFrontPressure(this)
        carStatus.tpmsRearPressureRaw.value =
            SettingPreference.getTpmsLastRearPressure(this)
        carStatus.tpmsFrontTempC.value =
            SettingPreference.getTpmsLastFrontTemp(this)
        carStatus.tpmsRearTempC.value =
            SettingPreference.getTpmsLastRearTemp(this)
        carStatus.tpmsFrontBatteryPct.value =
            SettingPreference.getTpmsLastFrontBattery(this)
        carStatus.tpmsRearBatteryPct.value =
            SettingPreference.getTpmsLastRearBattery(this)
    }

    var bleService: BleService? = null
    private var isBoundBleService = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (name?.className == BleService::class.java.name) {
                val binder = service as? BleService.LocalBinder
                bleService = binder?.getService()
                isBoundBleService = true
                bleService?.makeOverLaywindow()
                bleService?.hideOverlay()
                syncBtConnectionStatusFromService()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (name?.packageName == BleService::class.java.name) {
                bleService = null
                isBoundBleService = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        registerBleReceiver()
        // TPMS Settings Save 후 복귀 시 저장된 설정으로 서비스 기동
        startTpmsIfEnabled()

        if (!checkOverlayPermission()) {
            Toast.makeText(
                this,
                "Overlay permission is required. Allow it in Settings.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            bleService?.hideOverlay()
            // 풀스크린 설정
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )

            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            checkAndRequestPermissions()

            syncBtConnectionStatusFromService()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterBleReceiver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 풀스크린 설정
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )

        // 화면 꺼짐 방지
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        SettingPreference.restoreIgnitionTrip(this, carStatus)
        loadTpmsSettingsIntoCarStatus()

        val firstBoot = intent.getBooleanExtra("firstBoot", false)


        setContent {
            BMWMotorradClusterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onAndroidAutoClick = {
                            startHeadUnit()
                        },
                        onSettingsClick = {
                            val settingsIntent = Intent(this, SettingActivity::class.java)
                            // settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(settingsIntent)
                        },
                        onLauncherClick = {
                            val launcherIntent = Intent(this, LauncherActivity::class.java)
                            startActivity(launcherIntent)
                        },
                        onTpmsClick = {
                            startActivity(Intent(this, TpmsSettingActivity::class.java))
                        },
                        onEngineClick = { BleService.toggleLinCapture() },
                        context = this,
                        carStatus = carStatus,
                        firstBoot = firstBoot
                    )
                }
            }
        }

        startServices()
    }

    private fun startHeadUnit()
    {
        val launchIntent = this.packageManager.getLaunchIntentForPackage(HEADUNIT_PACKAGE_NAME)
        if (launchIntent != null) {
            // 새로운 Task로 실행되도록 플래그 확인 및 추가
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 큰 오버레이일 때만 헤드유닛 왼쪽 여백(세션 전용, 설정에 저장되지 않음)
            if (SettingPreference.isLargeOverlay(this)) {
                launchIntent.putExtra("user_left_padding", 700)
            }
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("AppLaunch", "앱 실행 실패: ${e.message}")
            }
        } else {
            Log.e("AppLaunch", "패키지를 찾을 수 없음: $HEADUNIT_PACKAGE_NAME")
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            startHeadUnit()
            return true
        }

        return false

    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, BleService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBoundBleService) {
            unbindService(serviceConnection)
            isBoundBleService = false
        }
    }

    private fun checkAndRequestPermissions() {
        if (!hasRequiredPermissions()) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        } else {
            startServices()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startServices() {
        val bleServiceIntent = Intent(this, BleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bleServiceIntent)
        } else {
            startService(bleServiceIntent)
        }
        startTpmsIfEnabled()
    }

    private fun startTpmsIfEnabled() {
        loadTpmsSettingsIntoCarStatus()
        if (!SettingPreference.isTpmsEnabled(this)) return
        if (!SettingPreference.hasTpmsSensorsConfigured(this)) return
        // 부팅 시 1회 조회 + 1시간 주기 (상시 연결 아님)
        TpmsService.startPolling(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun syncBtConnectionStatusFromService() {
        val deviceName = SettingPreference.getDeviceName(this) ?: ""
        val deviceAddress = SettingPreference.getAddress(this) ?: ""
        if (deviceName.isEmpty() || deviceAddress.isEmpty()) {
            carStatus.setBtStatus(false)
            return
        }
        val gattConnected =
            bleService?.getConnectionStatus() == BluetoothProfile.STATE_CONNECTED
        carStatus.setBtStatus(gattConnected)
    }

    private fun checkOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        } else {
            return true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            return
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // back 키 동작시 무시
    }
}