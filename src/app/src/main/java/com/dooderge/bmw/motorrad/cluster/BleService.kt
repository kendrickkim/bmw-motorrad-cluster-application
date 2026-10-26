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
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsPressureUnit
import com.dooderge.bmw.motorrad.cluster.tpms.TpmsProtocol
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        public var WHEEL_RPM_TO_SPEED = 0.884f

        @Volatile
        private var instance: BleService? = null

        fun setAppInForeground(inForeground: Boolean) {
            val service = instance ?: return
            service.appInForeground = inForeground
            service.applyOverlayVisibility()
        }

        /** 설정 화면에서만 켜는 미리보기. 저장하지 않는다. */
        fun setOverlayPreview(visible: Boolean) {
            val service = instance ?: return
            service.overlayPreview = visible
            service.applyOverlayVisibility()
        }

        fun setLargeOverlay(enabled: Boolean) {
            val service = instance ?: return
            service.mainHandler.post { service.rebuildOverlay() }
        }

        /** 엔진 롱프레스 등 — 다른 액티비티가 떠 있어도 서비스에서 계속 재생. */
        fun toggleLinCapture() {
            instance?.toggleLinCaptureInternal()
        }

        fun isLinCapturePlaying(): Boolean =
            instance?.currentCarStatus?.linCapturePlaying?.value == true

        private val captureTimestampFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        private const val SIM_TIRE_PRESSURE = "100.0"
        private const val SIM_TIRE_META = "99.8 °C"
    }

    private data class CompactOverlay(
        val speed: TextView,
        val gear: ImageView,
    )

    private data class LargeOverlay(
        val gear: ImageView,
        val speed: TextView,
        val rpm: TextView,
        val voltage: TextView,
        val distance: TextView,
        val coolant: TextView,
        val range: TextView,
        val frontPressure: TextView,
        val rearPressure: TextView,
        val frontDetail: TextView,
        val rearDetail: TextView,
    )

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var compactOverlay: CompactOverlay? = null
    private var largeOverlay: LargeOverlay? = null
    private var appInForeground = true
    private var overlayPreview = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private var linCaptureJob: Job? = null
    private var linCaptureGeneration = 0

    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()
        instance = this
        // Foreground Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "BLE and overlay service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Cluster service running")
                .setContentText("BLE link and overlay").setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        startForeground(NOTIFICATION_ID, notification)

        // read wheel RPM to speed factor from settings
        WHEEL_RPM_TO_SPEED = SettingPreference.getWheelRpmToSpeedFactor(this)
        if (SettingPreference.isLinLogging(this)) {
            linDataLogger.start()
        }

        // 오버레이 생성 (View 기반)

        // BLE 연결 시작 (실제 연결 코드 필요)
        connectToDevice()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun makeOverLaywindow() {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        if (overlayView != null) return
        attachOverlayView()
    }

    private fun rebuildOverlay() {
        val wm = windowManager ?: return
        overlayView?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
        compactOverlay = null
        largeOverlay = null
        attachOverlayView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlayView() {
        val wm = windowManager ?: return
        val (savedX, savedY) = SettingPreference.getOverlayPosition(this)
        val useLarge = SettingPreference.isLargeOverlay(this)
        val params = WindowManager.LayoutParams(
            if (useLarge) dpToPx(200) else WindowManager.LayoutParams.WRAP_CONTENT,
            if (useLarge) WindowManager.LayoutParams.MATCH_PARENT
            else WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = savedX
        params.y = if (useLarge) 0 else savedY
        overlayParams = params

        val root = if (useLarge) buildLargeOverlay() else buildCompactOverlay()
        overlayView = root
        attachOverlayDrag(root, params)
        wm.addView(root, params)
        refreshOverlayContent()
        applyOverlayVisibility()
    }

    private fun buildCompactOverlay(): LinearLayout {
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
            layoutParams = LinearLayout.LayoutParams(dpToPx(60), dpToPx(60))
        }
        val mFont = ResourcesCompat.getFont(applicationContext, R.font.robot_mono_bold_italic)
        val speedText = TextView(this).apply {
            text = "0"
            setTextColor(Color.WHITE)
            typeface = mFont
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), dpToPx(70))
            TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            setPadding(0, 0, dpToPx(15), 0)
        }
        layout.addView(speedText)
        layout.addView(gearImage)
        compactOverlay = CompactOverlay(speedText, gearImage)
        largeOverlay = null
        return layout
    }

    private fun buildLargeOverlay(): LinearLayout {
        val labelGray = Color.rgb(0x80, 0x80, 0x80)
        val mono = ResourcesCompat.getFont(applicationContext, R.font.robot_mono_bold_italic)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.argb(180, 0, 0, 0))
                cornerRadius = dpToPx(10).toFloat()
            }
            setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        fun autoSize(tv: TextView, minSp: Int, maxSp: Int) {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                tv, minSp, maxSp, 1, android.util.TypedValue.COMPLEX_UNIT_SP
            )
        }

        val gearImage = ImageView(this).apply {
            setImageResource(R.drawable.icon_gear_0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(dpToPx(77), dpToPx(90)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(6)
            }
        }
        root.addView(gearImage)

        fun valueWithUnit(
            maxSp: Int,
            minSp: Int,
            unit: String,
            unitSizeSp: Float,
            valueHeightDp: Int,
        ): Pair<TextView, LinearLayout> {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val value = TextView(this).apply {
                text = "0"
                setTextColor(Color.WHITE)
                typeface = mono
                includeFontPadding = false
                gravity = Gravity.END or Gravity.BOTTOM
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(valueHeightDp), 1f)
                autoSize(this, minSp, maxSp)
            }
            val unitView = TextView(this).apply {
                text = unit
                setTextColor(Color.WHITE)
                textSize = unitSizeSp
                includeFontPadding = false
                setPadding(dpToPx(4), 0, 0, dpToPx(4))
            }
            row.addView(value)
            row.addView(unitView)
            return value to row
        }

        val (speedText, speedRow) = valueWithUnit(56, 32, "km/h", 16f, 58)
        root.addView(speedRow)
        val (rpmText, rpmRow) = valueWithUnit(36, 22, "RPM", 15f, 40)
        rpmRow.layoutParams = (rpmRow.layoutParams as LinearLayout.LayoutParams).apply {
            topMargin = dpToPx(2)
            bottomMargin = dpToPx(8)
        }
        root.addView(rpmRow)

        fun gridLabel(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(labelGray)
            textSize = 14f
            includeFontPadding = false
            maxLines = 1
        }

        fun gridValue(): TextView = TextView(this).apply {
            text = "--"
            setTextColor(Color.WHITE)
            typeface = mono
            includeFontPadding = false
            maxLines = 1
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(30)
            )
            autoSize(this, 13, 20)
        }

        fun gridCell(label: String): Pair<TextView, LinearLayout> {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(gridLabel(label))
            val value = gridValue()
            col.addView(value)
            return value to col
        }

        fun twoColRow(leftLabel: String, rightLabel: String): Pair<TextView, TextView> {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(4) }
            }
            val (leftVal, leftCol) = gridCell(leftLabel)
            val (rightVal, rightCol) = gridCell(rightLabel)
            row.addView(leftCol)
            row.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1)
            })
            row.addView(rightCol)
            root.addView(row)
            return leftVal to rightVal
        }

        val (voltageText, distanceText) = twoColRow("Voltage", "Distance")
        val (coolantText, rangeText) = twoColRow("Coolant", "Range")

        root.addView(TextView(this).apply {
            text = "Tire pressure"
            setTextColor(labelGray)
            textSize = 14f
            gravity = Gravity.CENTER
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
                bottomMargin = dpToPx(4)
            }
        })

        val tireRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        fun tireCol(): Pair<TextView, TextView> {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val pressure = TextView(this).apply {
                text = "--"
                setTextColor(Color.WHITE)
                typeface = mono
                includeFontPadding = false
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(28)
                )
                autoSize(this, 13, 19)
            }
            val detail = TextView(this).apply {
                text = "--"
                setTextColor(labelGray)
                includeFontPadding = false
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(20)
                )
                autoSize(this, 10, 13)
            }
            col.addView(pressure)
            col.addView(detail)
            tireRow.addView(col)
            return pressure to detail
        }
        val (frontPressure, frontDetail) = tireCol()
        val (rearPressure, rearDetail) = tireCol()
        root.addView(tireRow)
        // 줄인 라인 간격만큼 남는 공간을 하단에 확보
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        })

        largeOverlay = LargeOverlay(
            gear = gearImage,
            speed = speedText,
            rpm = rpmText,
            voltage = voltageText,
            distance = distanceText,
            coolant = coolantText,
            range = rangeText,
            frontPressure = frontPressure,
            rearPressure = rearPressure,
            frontDetail = frontDetail,
            rearDetail = rearDetail,
        )
        compactOverlay = null
        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlayDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false
            private val longPressTimeout = 500L
            private val longPressRunnable = Runnable { isMoving = true }
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        mainHandler.postDelayed(longPressRunnable, longPressTimeout)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isMoving) {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            // 큰 오버레이는 화면 높이 고정 — 가로만 이동
                            if (params.height != WindowManager.LayoutParams.MATCH_PARENT) {
                                params.y = initialY + (event.rawY - initialTouchY).toInt()
                            }
                            windowManager?.updateViewLayout(overlayView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        mainHandler.removeCallbacks(longPressRunnable)
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
    }

    private fun refreshOverlayContent() {
        val status = currentCarStatus
        compactOverlay?.let { ui ->
            ui.speed.text = "${status.speed.value}"
            ui.gear.setImageResource(getResourceIdByGearNo(status.gear.value))
            return
        }
        val ui = largeOverlay ?: return
        ui.gear.setImageResource(getResourceIdByGearNo(status.gear.value))
        ui.speed.text = "${status.speed.value} "
        ui.rpm.text = "${status.rpm.value} "
        ui.voltage.text = String.format("%.1f v", status.voltage.value / 10.0)
        ui.distance.text = "${status.odometerKm.value} km"
        // ui.coolant.text = String.format("%.1f°C", status.coolant_temperature.value / 10.0)
        ui.coolant.text = String.format("%d°C", status.coolant_temperature.value / 10)
        ui.range.text = "${status.fuelRangeKm.value} km"

        if (status.linCapturePlaying.value) {
            // 메인 시뮬레이션(SectionTirePressure)과 동일 표시
            ui.frontPressure.text = SIM_TIRE_PRESSURE
            ui.rearPressure.text = SIM_TIRE_PRESSURE
            ui.frontDetail.text = SIM_TIRE_META
            ui.rearDetail.text = SIM_TIRE_META
        } else {
            val unit = SettingPreference.getTpmsUnit(this)
            val frontRaw = SettingPreference.getTpmsLastFrontPressure(this)
            val rearRaw = SettingPreference.getTpmsLastRearPressure(this)
            val frontTemp = SettingPreference.getTpmsLastFrontTemp(this)
            val rearTemp = SettingPreference.getTpmsLastRearTemp(this)
            ui.frontPressure.text = formatOverlayPressure(frontRaw, unit)
            ui.rearPressure.text = formatOverlayPressure(rearRaw, unit)
            ui.frontDetail.text = formatOverlayTireTemp(frontTemp)
            ui.rearDetail.text = formatOverlayTireTemp(rearTemp)
        }
    }

    private fun formatOverlayPressure(raw: Int, unit: TpmsPressureUnit): String {
        if (raw < 0) return "--"
        // 큰 오버레이: 단위(PSI/Bar/kPa) 문구 없이 숫자만
        return TpmsProtocol.formatPressure(raw, unit)
            .substringBefore(' ')
            .ifBlank { "--" }
    }

    private fun formatOverlayTireTemp(tempC: Int): String {
        return if (tempC < 0) "-- °C" else "$tempC °C"
    }

    var currentCarStatus = CarStatus()
    var oldCarStatus = CarStatus()

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
    }

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
                // 시뮬레이션 중에는 캡처 재생이 상태를 소유한다.
                if (currentCarStatus.linCapturePlaying.value) return

                if (data.size == LinFrame.LENGTH) {
                    LinFrame.apply(data, currentCarStatus)
                } else if (data.size >= 12) {
                    applyPackedCarStatus(data)
                } else {
                    return
                }

                if (!oldCarStatus.isSame(currentCarStatus)) {
                        mainHandler.post { refreshOverlayContent() }
                        oldCarStatus.copyFrom(currentCarStatus)
                        broadcastCarStatus()
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

    private var connectTimer: Timer? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice() {
        connectTimer?.cancel()
        val timer = Timer()
        connectTimer = timer

        val _this = this;

        timer.schedule(object : TimerTask() {
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

    fun hideOverlay() {
        overlayView?.visibility = View.GONE
    }

    fun showOverlay() {
        overlayView?.visibility = View.VISIBLE
    }

    private fun applyOverlayVisibility() {
        val show = overlayPreview || !appInForeground
        overlayView?.visibility = if (show) View.VISIBLE else View.GONE
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

    private fun broadcastCarStatus() {
        Intent(ACTION_CAR_STATUS_DATA_FILTER).also {
            it.setPackage(packageName)
            it.putExtra("car_status", currentCarStatus.toJson())
            sendBroadcast(it)
        }
    }

    private fun toggleLinCaptureInternal() {
        if (!SettingPreference.isLinTestEnabled(this)) return
        if (currentCarStatus.linCapturePlaying.value || linCaptureJob?.isActive == true) {
            stopLinCapture()
            return
        }
        startLinCapture()
    }

    private fun startLinCapture() {
        val generation = ++linCaptureGeneration
        linCaptureJob?.cancel()
        linCaptureJob = serviceScope.launch(Dispatchers.IO) {
            val lines = resources.openRawResource(R.raw.lin_capture)
                .bufferedReader()
                .readLines()
            withContext(Dispatchers.Main) {
                currentCarStatus.linCapturePlaying.value = true
                currentCarStatus.linCaptureLine.value = 0
                broadcastCarStatus()
            }
            try {
                while (isActive && generation == linCaptureGeneration) {
                    var previousTimestamp: Long? = null
                    var applied = false
                    for ((index, line) in lines.withIndex()) {
                        if (!isActive || generation != linCaptureGeneration) return@launch
                        val sample = parseLinCaptureLine(line) ?: continue
                        if (applied) {
                            val wait = sample.timestampMillis?.let { timestamp ->
                                previousTimestamp?.let { previous ->
                                    (timestamp - previous).coerceIn(0L, 1_000L)
                                }
                            } ?: 10L
                            if (wait > 0L) delay(wait)
                        }
                        previousTimestamp = sample.timestampMillis
                        applied = true
                        val lineNumber = index + 1
                        withContext(Dispatchers.Main) {
                            if (generation != linCaptureGeneration) return@withContext
                            currentCarStatus.linCaptureLine.value = lineNumber
                            LinFrame.apply(sample.frame, currentCarStatus)
                            SettingPreference.updateIgnitionTrip(this@BleService, currentCarStatus)
                            oldCarStatus.copyFrom(currentCarStatus)
                            refreshOverlayContent()
                            broadcastCarStatus()
                        }
                    }
                    // 마지막 라인까지 재생되면 1번부터 다시 시작
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (generation == linCaptureGeneration) {
                        currentCarStatus.linCapturePlaying.value = false
                        broadcastCarStatus()
                        refreshOverlayContent()
                    }
                }
            }
        }
    }

    private fun stopLinCapture() {
        linCaptureGeneration++
        linCaptureJob?.cancel()
        linCaptureJob = null
        currentCarStatus.linCapturePlaying.value = false
        broadcastCarStatus()
        refreshOverlayContent()
    }

    private data class LinCaptureSample(
        val frame: ByteArray,
        val timestampMillis: Long?
    )

    private fun parseLinCaptureLine(line: String): LinCaptureSample? {
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size < LinFrame.LENGTH) return null
        var offset = 0
        var timestampMillis: Long? = null
        if (parts.size >= LinFrame.LENGTH + 2 &&
            parts[0].matches(Regex("""\d{4}-\d{2}-\d{2}""")) &&
            parts[1].contains(':')
        ) {
            timestampMillis = parseCaptureTimestamp(parts[0], parts[1]) ?: return null
            offset = 2
        }
        if (parts.size - offset < LinFrame.LENGTH) return null
        val frame = ByteArray(LinFrame.LENGTH)
        for (index in 0 until LinFrame.LENGTH) {
            val token = parts[offset + index].removePrefix("0x").removePrefix("0X")
            val value = token.toIntOrNull(16) ?: return null
            frame[index] = value.toByte()
        }
        return LinCaptureSample(frame, timestampMillis)
    }

    private fun parseCaptureTimestamp(date: String, time: String): Long? {
        return try {
            LocalDateTime.parse("$date $time", captureTimestampFormatter)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private val linDataLogger = LinDataLogger(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LIN_LOG -> {
                if (!linDataLogger.start()) {
                    toast("Could not create the log file")
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
        stopLinCapture()
        serviceJob.cancel()
        connectTimer?.cancel()
        connectTimer = null
        linDataLogger.release()
        if (instance == this) instance = null
        super.onDestroy()
        if (overlayView != null) windowManager?.removeView(overlayView)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder
}
