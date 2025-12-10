package com.google.services.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 少女风亮色主题
private val GirlyLightColorScheme = lightColorScheme(
    primary = Pink40,                    // 热粉色
    onPrimary = Color.White,
    primaryContainer = PinkLight80,      // 浅粉容器
    onPrimaryContainer = PinkDeep,

    secondary = Lavender40,              // 薰衣草紫
    onSecondary = Color.White,
    secondaryContainer = Lavender80,
    onSecondaryContainer = Color(0xFF4A4458),

    tertiary = Mint40,                   // 薄荷绿
    onTertiary = Color.White,
    tertiaryContainer = Mint80,
    onTertiaryContainer = Color(0xFF1D4A3D),

    background = CreamWhite,             // 奶油白背景
    onBackground = Color(0xFF1C1B1F),

    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = PinkLight80,        // 浅粉色表面
    onSurfaceVariant = Color(0xFF49454F),

    error = CoralPink,                   // 珊瑚粉错误色
    onError = Color.White
)

// 少女风暗色主题
private val GirlyDarkColorScheme = darkColorScheme(
    primary = SakuraPink,                // 樱花粉
    onPrimary = Color(0xFF3E001D),
    primaryContainer = RosePink,
    onPrimaryContainer = Color.White,

    secondary = SoftPurple,              // 软紫色
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Lavender40,
    onSecondaryContainer = Color.White,

    tertiary = Mint80,
    onTertiary = Color(0xFF003828),
    tertiaryContainer = Mint40,
    onTertiaryContainer = Color.White,

    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    error = CoralPink,
    onError = Color(0xFF601410)
)

@Composable
fun KeepLiveServiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 不使用动态颜色，保持少女风配色
    val colorScheme = if (darkTheme) GirlyDarkColorScheme else GirlyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
