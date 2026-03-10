package org.cf0x.rustnithm.Theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 皮肤颜色引擎，用于 Canvas 绘图层的颜色派生
 */
class SkinColorEngine(
    val seedColor: Color,
    val isDark: Boolean
) {
    fun getDividerColor(alpha: Float = 0.3f): Color {
        val factor = 0.35f
        return if (isDark) {
            lerp(seedColor, Color.White, factor).copy(alpha = alpha)
        } else {
            lerp(seedColor, Color.Black, factor).copy(alpha = alpha)
        }
    }

    fun getAreaColor(isActive: Boolean, alpha: Float? = null): Color {
        return if (isActive) {
            seedColor.copy(alpha = alpha ?: 0.6f)
        } else {
            if (isDark) Color.DarkGray.copy(alpha = 0.15f)
            else Color.LightGray.copy(alpha = 0.15f)
        }
    }
}

/**
 * 核心色板生成函数：根据种子色派生全套 M3 色调
 */
private fun generateColorScheme(seed: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = seed,
            onPrimary = lerp(seed, Color.Black, 0.8f),
            primaryContainer = lerp(seed, Color.Black, 0.6f),
            onPrimaryContainer = lerp(seed, Color.White, 0.9f),

            secondary = lerp(seed, Color.Gray, 0.4f),
            onSecondary = Color.Black,
            secondaryContainer = lerp(seed, Color.Black, 0.7f),
            onSecondaryContainer = lerp(seed, Color.White, 0.8f),

            tertiary = lerp(seed, Color.Cyan, 0.2f),
            onTertiary = Color.Black,

            surface = Color(0xFF111114),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = lerp(seed, Color.Black, 0.85f),
            onSurfaceVariant = Color(0xFFCAC4D0),

            outline = lerp(seed, Color.Gray, 0.5f),
            error = Color(0xFFF2B8B5)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = lerp(seed, Color.White, 0.8f),
            onPrimaryContainer = lerp(seed, Color.Black, 0.7f),

            secondary = lerp(seed, Color.Gray, 0.3f),
            onSecondary = Color.White,
            secondaryContainer = lerp(seed, Color.White, 0.9f),

            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = lerp(seed, Color.White, 0.95f),
            onSurfaceVariant = Color(0xFF49454F),

            outline = lerp(seed, Color.Gray, 0.4f),
            error = Color(0xFFB3261E)
        )
    }
}

@Composable
fun RustnithmTheme(
    themeMode: Int = 2,
    useDynamicColor: Boolean = true,
    customSeedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        0 -> false
        1 -> true
        else -> isSystemInDarkTheme()
    }

    val finalSeed = customSeedColor ?: Color(0xFF0061A4)

    val colorScheme: ColorScheme = when {
        useDynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            generateColorScheme(finalSeed, isDark)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            if (activity is ComponentActivity) {
                activity.enableEdgeToEdge()
            }
            val window = activity.window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}