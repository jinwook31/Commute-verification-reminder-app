package com.milkaijun.militaryalarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Theme.kt 파일 내부의 ColorScheme 설정 부분을 찾아서 이렇게 바꿔주세요!

private val DarkColorScheme = darkColorScheme(
    primary = KaistBlue80,
    secondary = MilitarySteel80,
    tertiary = UrgentRed80
)

private val LightColorScheme = lightColorScheme(
    primary = KaistBlue40,
    secondary = MilitarySteel40,
    tertiary = UrgentRed40
)

@Composable
fun MilitaryalarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}