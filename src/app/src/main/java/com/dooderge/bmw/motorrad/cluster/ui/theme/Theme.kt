package com.dooderge.bmw.motorrad.cluster.ui.theme

import android.os.Build
import androidx.compose.animation.core.copy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dooderge.bmw.motorrad.cluster.R

// val ClusterAccentColor = Color(0xff5AB4CB)
// val ClusterSectionTitleColor = Color(0xff5AB4CB)
// val ClusterBackgroundRes = R.drawable.cluster_background_sky_blue

val ClusterBackgroundRes = R.drawable.cluster_background_black
val ClusterSectionTitleColor = Color(0xffaaaaaa)
val ClusterAccentColor = Color(0xffaaaaaa)

/** 메인 수치(거리, 전압, 속도 보조 표시 등) */
val ClusterValueColor = Color(0xffffffff)

/** 보조 수치(TPMS 온도·배터리 등) */
val ClusterValueMetaColor = Color(0xff888888)

/** Apps 섹션 아이콘 tint */
val ClusterAppsIconColor = Color(0xffaaaaaa)

/** 설정·TPMS·앱 리스트 오른쪽 액션 버튼 */
val ClusterSideButtonContainerColor = Color(0xff333333)
val ClusterSideButtonContentColor = Color.White

/** 강조 액션 버튼(TPMS QR 등) · 설정/TPMS primary */
val ClusterActionButtonContainerColor = Color(0xFF4376A1)
val ClusterActionButtonContentColor = Color.White

/** 설정 / TPMS / 로그 화면 공통 Material dark scheme */
val ClusterSettingsColorScheme = darkColorScheme(
    primary = ClusterActionButtonContainerColor,
    onPrimary = ClusterActionButtonContentColor,
    primaryContainer = ClusterActionButtonContainerColor,
    onPrimaryContainer = ClusterActionButtonContentColor,
    secondary = ClusterActionButtonContainerColor,
    onSecondary = ClusterActionButtonContentColor,
    secondaryContainer = Color(0xFF2C4F6B),
    tertiary = ClusterActionButtonContainerColor,
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = ClusterActionButtonContainerColor
)

/** 설정·TPMS Switch 공통 색 */
@Composable
fun clusterSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = ClusterActionButtonContainerColor,
    checkedBorderColor = ClusterActionButtonContainerColor,
    uncheckedThumbColor = Color(0xFFAAAAAA),
    uncheckedTrackColor = Color(0xFF333333),
    uncheckedBorderColor = Color(0xFF666666)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.Black,
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFF3700B3),
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

val interFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold)
)

@Composable
fun BMWMotorradClusterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val defaultTypography = Typography()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
        typography = Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = interFamily),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = interFamily),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = interFamily),
            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = interFamily),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = interFamily),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = interFamily),
            titleLarge = defaultTypography.titleLarge.copy(fontFamily = interFamily),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = interFamily),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = interFamily),
            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = interFamily),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = interFamily),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = interFamily),
            labelLarge = defaultTypography.labelLarge.copy(fontFamily = interFamily),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = interFamily),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = interFamily)
        ),
    )
}
