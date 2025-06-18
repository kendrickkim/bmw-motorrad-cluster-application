package com.dooderge.bmw.motorrad.cluster

enum class WW_VR_KEY(var value: Int, var key_name: String = "") {
    VR_KEY_REPEAT_LEFT(0, "repeat_left"),
    VR_KEY_REPEAT_RIGHT(1, "repeat_right"),
    VR_KEY_REPEAT_CENTER(2, "repeat_center"),
    VR_KEY_WHEEL_LEFT(3, "wheel_left"),
    VR_KEY_WHEEL_RIGHT(4, "wheel_right"),
    VR_KEY_WHEEL_UP(5, "wheel_up"),
    VR_KEY_WHEEL_DOWN(6, "wheel_down"),
    VR_KEY_ACTION_DOWN(101, "action_down"),
    VR_KEY_ACTION_UP(102, "action_up"),
    VR_KEY_ACTION_PRESS(103, "action_press"),
}