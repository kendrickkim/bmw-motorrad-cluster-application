package com.dooderge.bmw.motorrad.cluster.ui.component

import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 시스템 글꼴 배율을 1로 고정해 클러스터 레이아웃이 깨지지 않게 한다.
 */
@Composable
fun ClusterFixedFontScale(content: @Composable () -> Unit) {
	val density = LocalDensity.current
	CompositionLocalProvider(
		LocalDensity provides Density(
			density = density.density,
			fontScale = 1f
		),
		content = content
	)
}

/**
 * 몰입형 풀스크린 + 화면 유지. Activities onCreate/onResume에서 공통 호출.
 */
@Suppress("DEPRECATION")
fun Window.applyClusterImmersiveMode() {
	decorView.systemUiVisibility = (
		View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			or View.SYSTEM_UI_FLAG_FULLSCREEN
			or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		)
	addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}
