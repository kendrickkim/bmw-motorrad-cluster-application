package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

var widthDesigned = 1920f
var heightDesigned = 1080f
var ratioImageAndScreen = 1f

fun setDesginedSizeByHeight(height: Float) {
    ratioImageAndScreen = height / heightDesigned
}

fun setDesignedSizeByWidth(width: Float) {
    ratioImageAndScreen = width / widthDesigned
}

@Composable
fun Int.designedDp(): Dp {
    return (this@designedDp * ratioImageAndScreen).pxToDp()
}

@Composable
fun Float.designedDp(): Dp {
    return (this@designedDp * ratioImageAndScreen).pxToDp()
}

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }
