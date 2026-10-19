package com.dooderge.bmw.motorrad.cluster

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat

class BleService : Service() {
    companion object {
        private const val CHANNEL_ID = "ble_overlay_channel"
        private const val NOTIFICATION_ID = 1
        public const val ACTION_CAR_STATUS_DATA_FILTER =
            "com.dooderge.bmw.motorrad.cluster.CAR_STATUS_CHANGED"
        public const val ACTION_CHECK_INPUT_METHOD =
            "com.dooderge.bmw.motorrad.cluster.CHECK_INPUT_METHOD"
        public const val ACTION_BT_CONNECTION_STATE_CHANGED =
            "com.dooderge.bmw.motorrad.cluster.BT_CONNECTION_STATE_CHANGED"
        const val EXTRA_BT_CONNECTED = "connected"
        const val ACTION_START_LIN_LOG =
            "com.dooderge.bmw.motorrad.cluster.START_LIN_LOG"
        const val ACTION_STOP_LIN_LOG =
            "com.dooderge.bmw.motorrad.cluster.STOP_LIN_LOG"
        const val ACTION_LIN_LOG_STATE =
            "com.dooderge.bmw.motorrad.cluster.LIN_LOG_STATE"
        const val EXTRA_LIN_LOGGING = "logging"

        //        public var WHEEL_RPM_TO_SPEED = 0.121f // 1 RPM = 0.121 km/h
//		public var WHEEL_RPM_TO_SPEED = 0.98f
        public var WHEEL_RPM_TO_SPEED = 0.884f
    }


    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    // fun dpToSp(dp: Int): Int {
    //     return (dp * resources.displayMetrics.scaledDensity + 0.5f).toInt()
    // }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()
        // Foreground Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "BLE+Overlay 서비스 채널", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("클러스터 서비스 동작 중")
                .setContentText("BLE 통신 및 오버레이 표시").setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        startForeground(NOTIFICATION_ID, notification)

        // read wheel RPM to speed factor from settings
        WHEEL_RPM_TO_SPEED = SettingPreference.getWheelRpmToSpeedFactor(this)
        if (SettingPreference.isLinLogging(this)) {
            linDataLogger.start()
        }
//		WHEEL_RPM_TO_SPEED = 300.0f

        // 오버레이 생성 (View 기반)

        // BLE 연결 시작 (실제 연결 코드 필요)
        connectToDevice()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun makeOverLaywindow() {
        if (windowManager != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val (savedX, savedY) = SettingPreference.getOverlayPosition(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.x = savedX
        params.y = savedY

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(180, 0, 0, 0))
                cornerRadius = dpToPx(10).toFloat()
                setStroke(dpToPx(1), Color.argb(100, 255, 255, 255))
            }
            background = drawable
            setPadding(dpToPx(5), dpToPx(5), dpToPx(10), dpToPx(5))
        }

        val gearImage = ImageView(this).apply {
            setImageResource(R.drawable.icon_gear_0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60), dpToPx(60)
            )
        }
        var mFont =
            ResourcesCompat.getFont(applicationContext, R.font.robot_mono_bold_italic)
        val speedText = TextView(this).apply {
            text = "0"
            setTextColor(Color.WHITE)
//            textSize = dpToSp(20).toFloat()
            typeface = mFont
            includeFontPadding = false
//            gravity = Gravity.TOP or Gravity.RIGHT
//            gravity = Gravity.CENTER
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(130), dpToPx(70)
            )

            TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this,
                TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM
            )

//            setPadding(0, dpToPx(-10), 30, 0)
            setPadding(0, 0, dpToPx(15), 0)
        }

        // Unused overlay RPM label. Speed and gear are the views actually added.
        // val rpmText = TextView(this).apply { ... }
        layout.addView(speedText)
        layout.addView(gearImage)

        var last_touch_millis: Long = 0;

        gearImage.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val current_touch_millis = System.currentTimeMillis()
                    if (current_touch_millis - last_touch_millis < 300) {
//                        Log.d("BleService", "onTouch: ${event.action}")
                        return true
                    }
                    last_touch_millis = current_touch_millis

                }
                return false
            }
        })

        overlayView = layout

        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false
            private val longPressTimeout = 500L
            private val handler = Handler(Looper.getMainLooper())
            private val longPressRunnable = Runnable { isMoving = true }
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        handler.postDelayed(longPressRunnable, longPressTimeout)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (isMoving) {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(overlayView, params)
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        if (isMoving) {
                            SettingPreference.setOverlayPosition(
                                this@BleService, params.x, params.y
                            )
                            isMoving = false
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(overlayView, params)
        hideOverlay()
    }


    // var old_car_status: ByteArray = ByteArray(15)
    // fun copyArray(source: ByteArray): ByteArray { ... }

    var currentCarStatus = CarStatus()
    var oldCarStatus = CarStatus()

    private fun Byte.u8(): Int = toInt() and 0xFF

    private fun applyPackedCarStatus(data: ByteArray) {
        val wheelRpm = data[7].u8() + data[8].u8() * 256
        currentCarStatus.setButtons(data[2])
        currentCarStatus.gear.value = data[5].u8()
        currentCarStatus.speed.value = (wheelRpm * WHEEL_RPM_TO_SPEED).toInt()
        currentCarStatus.rpm.value = data[7].u8() + data[10].u8() * 256
        currentCarStatus.voltage.value = data[6].u8()
        currentCarStatus.isIgnitionOn.value = data[11].u8() == 1
        currentCarStatus.ignitionKnown.value = true
        currentCarStatus.wheel_value.value = data[4].u8()
        // Unused alternate layout that looked for an ASCII "V1" prefix:
        // gear = data[3], speed from data[5..6], rpm from data[7..8], voltage = data[4]
    }

    // fun sendKeyEvent(...) { broadcast to VirtualKeyService }
    // fun parseButtons() { ... }
    // fun parseWheel() { ... }

    var connection_status = BluetoothProfile.STATE_DISCONNECTED

    var bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt.discoverServices()
                    connection_status = BluetoothProfile.STATE_CONNECTED
                    notifyBtConnectionStateChanged(true)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connection_status = BluetoothProfile.STATE_DISCONNECTED
                    gatt.close()
                    notifyBtConnectionStateChanged(false)
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    connection_status = BluetoothProfile.STATE_CONNECTING
                    notifyBtConnectionStateChanged(false)
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service =
                    gatt.getService(java.util.UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"))
                val characteristic =
                    service.getCharacteristic(java.util.UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"))
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)

                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid.toString() == "00002a01-0000-1000-8000-00805f9b34fb") {
                val data = characteristic.value ?: return
                linDataLogger.append(data)

                if (data.size == LinFrame.LENGTH) {
                    LinFrame.apply(data, currentCarStatus)
                } else if (data.size >= 12) {
                    applyPackedCarStatus(data)
                } else {
                    return
                }

                if (!oldCarStatus.isSame(currentCarStatus)) {
//                        parseButtons()
//                        parseWheel()

                        if (currentCarStatus.speed.value != oldCarStatus.speed.value)
                            updateSpeed(currentCarStatus.speed.value)
                        if (currentCarStatus.gear.value != oldCarStatus.gear.value)
                            updateGear(currentCarStatus.gear.value)

                        oldCarStatus.copyFrom(currentCarStatus)

                        Intent(ACTION_CAR_STATUS_DATA_FILTER).also {
                            it.setPackage(packageName)
                            it.putExtra("car_status", currentCarStatus.toJson())
                            sendBroadcast(it)
                        }
                        // Notify UI or other components if needed
                    } else {
//                        Log.d("BleService", "No change in car status")
                    }

                }
            }
        }

    fun getInputDevice(deviceName: String): Boolean {
        if (deviceName.isEmpty()) return false;
        val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        val inputDeviceIds = inputManager.inputDeviceIds
        for (deviceId in inputDeviceIds) {
            val inputDevice = inputManager.getInputDevice(deviceId)
            if (inputDevice != null && !inputDevice.isVirtual) {
                // 소스에 키보드가 포함되어 있는지 확인
                if (inputDevice.sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
                    val device_name = inputDevice.name

                    if (deviceName == device_name)
                        return true;
                }
            }
        }
        return false;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice() {

        val tmr_connect = Timer()

        val _this = this;

        tmr_connect.schedule(object : TimerTask() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun run() {
                if (connection_status == BluetoothProfile.STATE_CONNECTED ||
                    connection_status == BluetoothProfile.STATE_CONNECTING
                ) return

                // 실제 BLE 연결 코드 필요 (주석 처리)
                val deviceAddress = SettingPreference.getAddress(_this)
                val deviceName = SettingPreference.getDeviceName(_this)
                if (deviceAddress == null || deviceAddress.isEmpty()) return;
                if (!getInputDevice(deviceName ?: "")) {
                    var intent = Intent(ACTION_CHECK_INPUT_METHOD)
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                    notifyBtConnectionStateChanged(false)
                    return
                }

                notifyBtConnectionStateChanged(true)
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                var device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
                device.connectGatt(_this, false, bluetoothGattCallback)
            }
        }, 2000, 3000) // 서비스가 시작된 후 2초뒤 3초마다 연결 시도
    }

    fun getResourceIdByGearNo(gearNo: Int): Int {
        return when (gearNo) {
            0 -> R.drawable.icon_gear_0
            1 -> R.drawable.icon_gear_1
            2 -> R.drawable.icon_gear_2
            3 -> R.drawable.icon_gear_3
            4 -> R.drawable.icon_gear_4
            5 -> R.drawable.icon_gear_5
            6 -> R.drawable.icon_gear_6
            9 -> R.drawable.icon_gear_9
            else -> R.drawable.icon_gear_0
        }
    }

    fun updateSpeed(speed: Int) {
        val layout = overlayView as? LinearLayout ?: return
        (layout.getChildAt(0) as? TextView)?.text = "${speed}"
    }

    fun updateGear(gear: Int) {
        val layout = overlayView as? LinearLayout ?: return
        (layout.getChildAt(1) as? ImageView)?.setImageResource(getResourceIdByGearNo(gear))
    }

    fun hideOverlay() {
        overlayView?.visibility = View.GONE
    }

    fun showOverlay() {
        overlayView?.visibility = View.VISIBLE
    }

    fun getConnectionStatus(): Int {
        return connection_status
    }

    private fun notifyBtConnectionStateChanged(connected: Boolean) {
        Intent(ACTION_BT_CONNECTION_STATE_CHANGED).also {
            it.setPackage(packageName)
            it.putExtra(EXTRA_BT_CONNECTED, connected)
            sendBroadcast(it)
        }
    }

    private val linDataLogger = LinDataLogger(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LIN_LOG -> {
                if (!linDataLogger.start()) {
                    toast("로그 파일을 만들지 못했습니다")
                }
                broadcastLinLogState()
            }
            ACTION_STOP_LIN_LOG -> {
                val names = linDataLogger.stop()
                broadcastLinLogState()
                if (names.isNotEmpty()) {
                    toast(names.joinToString("\n"))
                }
            }
        }
        return START_STICKY
    }

    private fun broadcastLinLogState() {
        Intent(ACTION_LIN_LOG_STATE).also {
            it.setPackage(packageName)
            it.putExtra(EXTRA_LIN_LOGGING, linDataLogger.active)
            sendBroadcast(it)
        }
    }

    private fun toast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        linDataLogger.release()
        super.onDestroy()
        if (overlayView != null) windowManager?.removeView(overlayView)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder
}
