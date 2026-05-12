package com.dooderge.bmw.motorrad.cluster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    companion object {
        private const val PREFS_NAME = "splash_activity_prefs"
        private const val KEY_LAST_SPLASH_BOOT_MARKER = "last_splash_boot_marker"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasSplashAlreadyShownInCurrentBoot()) {
            openMainActivity(animate = false, firstBoot = false)
            return
        }

        markSplashShownInCurrentBoot()

        setContent {
            SplashScreen {
                openMainActivity(animate = true, firstBoot = true)
            }
        }
    }

    private fun hasSplashAlreadyShownInCurrentBoot(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SPLASH_BOOT_MARKER, null) == currentBootMarker()
    }

    private fun markSplashShownInCurrentBoot() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SPLASH_BOOT_MARKER, currentBootMarker())
            .apply()
    }

    private fun currentBootMarker(): String {
        return try {
            "boot_count:${Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT)}"
        } catch (_: Settings.SettingNotFoundException) {
            "boot_time:${(System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1000L}"
        }
    }

    private fun openMainActivity(animate: Boolean, firstBoot: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("firstBoot", firstBoot)
        startActivity(intent)
        if (animate) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            overridePendingTransition(0, 0)
        }
        finish()
    }

    override fun onResume()
    {
        super.onResume()

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun SplashScreen(onAnimationComplete: () -> Unit) {
    // 1. res/raw 폴더의 lottie 파일을 로드합니다.
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bmw_motorrad_start))

    // 2. 애니메이션 진행 상태를 관리합니다.
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1 // 1번만 재생
    )

    // 3. 애니메이션이 재생 완료되면 onTimeout 콜백을 호출합니다.
    LaunchedEffect(progress) {
        if (progress == 1f) { // 애니메이션이 끝까지 재생되면
            delay(1000)
            onAnimationComplete()
        }
    }

    // 화면 중앙에 Lottie 애니메이션을 표시합니다.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop

        // 화면 전체에 채우거나 크기 조절
        )
    }
}