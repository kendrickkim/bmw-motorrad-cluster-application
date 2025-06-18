package com.dooderge.bmw.motorrad.cluster

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


class VirtualKeyService : AccessibilityService() {
    companion object {
        const val VIRTUAL_KEY_DATA_FILTER = "com.dooderge.bmw.motorrad.cluster.VIRTUAL_KEY_DATA"
    }

    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == VIRTUAL_KEY_DATA_FILTER) {
                val keyCode = intent.getIntExtra("keycode", -1)
                val action = intent.getIntExtra("action", -1)

                if (keyCode != -1 && action != -1) {
                    if (action == WW_VR_KEY.VR_KEY_ACTION_UP.value
                        || action == WW_VR_KEY.VR_KEY_ACTION_PRESS.value
                    ) {
                        handleKeyEvent(keyCode, action)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(keyReceiver, IntentFilter(VIRTUAL_KEY_DATA_FILTER))
    }

    var rootNode: AccessibilityNodeInfo? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 필요한 경우 접근성 이벤트 처리
        if (event!!.getEventType() === AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 윈도우 상태 변경 시
//            rootNode = getRootInActiveWindow()
        }
    }

    override fun onInterrupt() {
        // 서비스가 중단될 때 호출
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    private fun handleKeyEvent(keyCode: Int, action: Int) {
        Log.d("VirtualKeyService", "Handling key event: keyCode=$keyCode, action=$action")

        var changedKeyCode = when (keyCode) {
            WW_VR_KEY.VR_KEY_WHEEL_UP.value -> GLOBAL_ACTION_DPAD_UP
            WW_VR_KEY.VR_KEY_WHEEL_DOWN.value -> GLOBAL_ACTION_DPAD_DOWN
            WW_VR_KEY.VR_KEY_WHEEL_RIGHT.value -> GLOBAL_ACTION_BACK
            WW_VR_KEY.VR_KEY_WHEEL_LEFT.value -> GLOBAL_ACTION_DPAD_CENTER
            else -> return
        }
        rootNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
//
//        when (action) {
////            WW_VR_KEY.VR_KEY_ACTION_DOWN.value -> {
////                performGlobalAction(GLOBAL_ACTION_DPAD_LEFT)
////            }
//
//            WW_VR_KEY.VR_KEY_ACTION_UP.value -> {
//                performGlobalAction(GLOBAL_ACTION_DPAD_RIGHT)
//            }
//
//            WW_VR_KEY.VR_KEY_ACTION_PRESS.value -> {
//                when (changedKeyCode) {
//                    KeyEvent.KEYCODE_DPAD_LEFT -> performGlobalAction(GLOBAL_ACTION_DPAD_LEFT)
//                    KeyEvent.KEYCODE_DPAD_RIGHT -> performGlobalAction(GLOBAL_ACTION_DPAD_RIGHT)
//                    KeyEvent.KEYCODE_DPAD_CENTER -> performGlobalAction(GLOBAL_ACTION_DPAD_CENTER)
//                    KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
//                }
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(keyReceiver)
    }
} 