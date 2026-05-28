package com.dooderge.bmw.motorrad.cluster

import android.Manifest
import android.accessibilityservice.AccessibilityService
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
import android.text.TextUtils
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
            Toast.makeText(this, "필수 권한이 허용되지 않았습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private var isBleReceiverRegistered = false

    private fun registerBleReceiver() {
        if (isBleReceiverRegistered) return

        val intentFilter = IntentFilter().apply {
            addAction(BleService.ACTION_CAR_STATUS_DATA_FILTER)
            addAction(BleService.ACTION_CHECK_INPUT_METHOD)
            addAction(BleService.ACTION_BT_CONNECTION_STATE_CHANGED)
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
            if (intent?.action == BleService.ACTION_CAR_STATUS_DATA_FILTER) {
                var json = intent.getStringExtra("car_status").toString()
                var newCarStatus = CarStatus()
                newCarStatus.fromJson(json)
                carStatus.copyFrom(newCarStatus)
                carStatus.setBtStatus(true)

//                Log.d("MainActivity", "Received car status: $carStatus")
            } else if (intent?.action == BleService.ACTION_CHECK_INPUT_METHOD) {
//                Toast.makeText(
//                    applicationContext,
//                    "블루투스 입력장치 설정이 올바르지 않습니다.\n" +
//                            "장치 초기화 > 블루투스 키보드 장치 연결 > 장치 재설정하세요",
//                    Toast.LENGTH_SHORT
//                ).show()
                carStatus.setBtStatus(false)
            } else if (intent?.action == BleService.ACTION_BT_CONNECTION_STATE_CHANGED) {
                val connected = intent.getBooleanExtra(BleService.EXTRA_BT_CONNECTED, false)
                carStatus.setBtStatus(connected)
            }
        }


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

        if (!checkOverlayPermission()) {
            Toast.makeText(
                this,
                "오버레이 권한이 필요합니다. 설정에서 권한을 허용해주세요.",
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
//                Toast.makeText(
//                    this,
//                    "블루투스 장치가 선택되지 않았습니다. 설정에서 장치를 선택해주세요.",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
        }
//        else {
//            // 선택된 BLE 장치 정보로 서비스 시작
//            bleService?.connectToDevice()
//        }
    }

    override fun onPause() {
        super.onPause()
        unregisterBleReceiver()
        bleService?.showOverlay()
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

        val firstBoot = intent.getBooleanExtra("firstBoot", false)


        setContent {
            BMWMotorradClusterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onPermissionsGranted = {
                            startServices()
                        },
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
                        context = this,
                        carStatus = carStatus,
                        firstBoot = firstBoot
                    )
                }
            }
        }

        startServices()


        // 접근성 서비스 활성화 체크 및 설정 화면 이동
//        if (!isAccessibilityServiceEnabled(this, VirtualKeyService::class.java)) {
//            Toast.makeText(this, "앱의 접근성 서비스를 활성화 해주세요.", Toast.LENGTH_LONG).show()
//            launchAccessibilitySettings(this)
//        }
    }

    private fun startHeadUnit()
    {
        val launchIntent = this.packageManager.getLaunchIntentForPackage(HEADUNIT_PACKAGE_NAME)
        if (launchIntent != null) {
            // 새로운 Task로 실행되도록 플래그 확인 및 추가
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (ComponentName.unflattenFromString(componentName) == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun launchAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
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