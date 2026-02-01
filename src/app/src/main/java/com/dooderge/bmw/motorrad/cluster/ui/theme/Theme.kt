package com.dooderge.bmw.motorrad.cluster.ui.theme

import android.os.Build
import androidx.compose.animation.core.copy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.Typeface
import com.dooderge.bmw.motorrad.cluster.R

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
