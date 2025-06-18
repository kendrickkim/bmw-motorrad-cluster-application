package com.dooderge.bmw.motorrad.cluster

import android.Manifest
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
import android.graphics.Typeface
import android.hardware.input.InputManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.MutableState
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginTop

class BleService : Service() {
    companion object {
        private const val CHANNEL_ID = "ble_overlay_channel"
        private const val NOTIFICATION_ID = 1
        public const val ACTION_CAR_STATUS_DATA_FILTER =
            "com.dooderge.bmw.motorrad.cluster.CAR_STATUS_CHANGED"
        public const val ACTION_CHECK_INPUT_METHOD =
            "com.dooderge.bmw.motorrad.cluster.CHECK_INPUT_METHOD"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    fun dpToSp(dp: Int): Int {
        return (dp * resources.displayMetrics.scaledDensity + 0.5f).toInt()
    }

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

        // 오버레이 생성 (View 기반)

        // BLE 연결 시작 (실제 연결 코드 필요)
        connectToDevice()
    }

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
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            setPadding(10, 10, 10, 10)
        }

        val gearImage = ImageView(this).apply {
            setImageResource(R.drawable.icon_gear_0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(70), dpToPx(70)
            )
        }
        var mFont =
            ResourcesCompat.getFont(applicationContext, R.font.f_e1234_i)
        val speedText = TextView(this).apply {
            text = "000"
            setTextColor(Color.WHITE)
            textSize = dpToSp(20).toFloat()
            typeface = mFont
            gravity = Gravity.CENTER or Gravity.RIGHT
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(130), dpToPx(70)
            )
            setPadding(0, dpToPx(5), 30, 0)
        }
        layout.addView(speedText)
        layout.addView(gearImage)
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


    var old_car_status: ByteArray = ByteArray(15)

    fun copyArray(source: ByteArray): ByteArray {
        val destination = ByteArray(source.size)
        for (i in source.indices) {
            destination[i] = source[i]
        }
        return destination
    }

    var currentCarStatus = CarStatus()
    var oldCarStatus = CarStatus()

    fun sendKeyEvent(keyCode: WW_VR_KEY, action: WW_VR_KEY) {
        Log.d("BleService", "sendKeyEvent: ${keyCode.key_name} - ${action.key_name}")
        Intent(VirtualKeyService.VIRTUAL_KEY_DATA_FILTER).also {
            it.putExtra("keycode", keyCode.value)
            it.putExtra("action", action.value)
            sendBroadcast(it)
        }
    }

    fun parseButtons() {
        fun button_status_changed(
            old_button: MutableState<Boolean>,
            new_button: MutableState<Boolean>,
            keycode: WW_VR_KEY
        ) {
            if (!old_button.value && new_button.value) {
                Log.d("BleService", "$keycode button down")
                sendKeyEvent(keycode, WW_VR_KEY.VR_KEY_ACTION_DOWN)
            }
            if (old_button.value && !new_button.value) {
                Log.d("BleService", "$keycode button up")
                sendKeyEvent(keycode, WW_VR_KEY.VR_KEY_ACTION_UP)
            }
        }

        button_status_changed(
            oldCarStatus.button_repeat_left,
            currentCarStatus.button_repeat_left,
            WW_VR_KEY.VR_KEY_REPEAT_LEFT
        )
        button_status_changed(
            oldCarStatus.button_repeat_right,
            currentCarStatus.button_repeat_right,
            WW_VR_KEY.VR_KEY_REPEAT_RIGHT
        )
        button_status_changed(
            oldCarStatus.button_repeat_center,
            currentCarStatus.button_repeat_center,
            WW_VR_KEY.VR_KEY_REPEAT_CENTER
        )
        button_status_changed(
            oldCarStatus.button_wheel_left,
            currentCarStatus.button_wheel_left,
            WW_VR_KEY.VR_KEY_WHEEL_LEFT
        )
        button_status_changed(
            oldCarStatus.button_wheel_right,
            currentCarStatus.button_wheel_right,
            WW_VR_KEY.VR_KEY_WHEEL_RIGHT
        )
    }

    fun parseWheel() {
        var is_up = false;

        var old_wheel_value = oldCarStatus.wheel_value.value
        var new_wheel_value = currentCarStatus.wheel_value.value

        if (old_wheel_value == -1 || new_wheel_value == -1) return


        if (old_wheel_value != new_wheel_value) {
//            Log.d(
//                "BleService",
//                "old wheel value: $old_wheel_value, new wheel value: $new_wheel_value"
//            )
            if (old_wheel_value < new_wheel_value) {
                is_up = true;
                if (new_wheel_value - old_wheel_value > 200) {
                    is_up = false;
                }
            } else if (old_wheel_value > new_wheel_value) {
                is_up = false;
                if (old_wheel_value - new_wheel_value > 200) {
                    is_up = true;
                }
            }

            if (is_up) {
                sendKeyEvent(WW_VR_KEY.VR_KEY_WHEEL_UP, WW_VR_KEY.VR_KEY_ACTION_PRESS)
            } else {
                sendKeyEvent(WW_VR_KEY.VR_KEY_WHEEL_DOWN, WW_VR_KEY.VR_KEY_ACTION_PRESS)
            }
        }

    }

    var connection_status = BluetoothProfile.STATE_DISCONNECTED

    var bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                connection_status = BluetoothProfile.STATE_CONNECTED
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connection_status = BluetoothProfile.STATE_DISCONNECTED
                gatt.close()
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                connection_status = BluetoothProfile.STATE_DISCONNECTED
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

        fun Byte.toUnsigendInt(): Int {
            var r: Int = this.toInt();
            if (r < 0) r += 256
            return r;
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid.toString() == "00002a01-0000-1000-8000-00805f9b34fb") {
                var data: ByteArray = characteristic.value

                if (data.isNotEmpty()) {

                    var dataString: String = "";
                    for (byte in data) {
                        dataString += byte.toUnsigendInt().toUByte().toString(16) + " "
                    }

                    Log.d("BleService", "Received data: $dataString")

                    var speed: Int = data[5].toUnsigendInt() + data[6].toUnsigendInt() * 256
                    var rpm = data[7].toUnsigendInt() + data[8].toUnsigendInt() * 256
                    var gear = data[3].toUnsigendInt()

                    currentCarStatus.setButtons(data[0])
                    currentCarStatus.gear.value = gear
                    currentCarStatus.speed.value = speed
                    currentCarStatus.rpm.value = rpm
                    currentCarStatus.voltage.value = data[4].toUnsigendInt()
                    currentCarStatus.isIgnitionOn.value = data[9].toUnsigendInt() == 1
                    currentCarStatus.wheel_value.value = data[2].toUnsigendInt()

                    if (!oldCarStatus.isSame(currentCarStatus)) {
//                        parseButtons()
//                        parseWheel()

                        if (currentCarStatus.speed.value != oldCarStatus.speed.value)
                            updateSpeed(currentCarStatus.speed.value)
                        if (currentCarStatus.gear.value != oldCarStatus.gear.value)
                            updateGear(currentCarStatus.gear.value)

                        oldCarStatus.copyFrom(currentCarStatus)

                        Intent(ACTION_CAR_STATUS_DATA_FILTER).also {
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
                if (connection_status == BluetoothProfile.STATE_CONNECTED) return

                // 실제 BLE 연결 코드 필요 (주석 처리)
                val deviceAddress = SettingPreference.getAddress(_this)
                val deviceName = SettingPreference.getDeviceName(_this)
                if (deviceAddress == null || deviceAddress.isEmpty()) return;
                if (!getInputDevice(deviceName ?: "")) {
                    var intent = Intent(ACTION_CHECK_INPUT_METHOD)
                    sendBroadcast(intent)
                    return
                }


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

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) windowManager?.removeView(overlayView)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder
}