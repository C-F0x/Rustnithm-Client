package org.cf0x.rustnithm.Theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalExpressive = compositionLocalOf { false }

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

private fun generateColorScheme(
    seed: Color,
    isDark: Boolean,
    expressive: Boolean = false
): ColorScheme {
    val boost = if (expressive) 0.12f else 0f

    return if (isDark) {
        val primary = lerp(seed, Color.White, boost)
        val tertiary = lerp(
            lerp(seed, Color(0xFF00E5FF), if (expressive) 0.35f else 0.2f),
            Color.White,
            boost
        )
        val secondaryContainer = if (expressive)
            lerp(seed, Color(0xFF1A1A2E), 0.45f)
        else
            lerp(seed, Color.Black, 0.7f)

        darkColorScheme(
            primary = primary,
            onPrimary = lerp(seed, Color.Black, 0.8f),
            primaryContainer = lerp(seed, Color.Black, if (expressive) 0.45f else 0.6f),
            onPrimaryContainer = lerp(seed, Color.White, if (expressive) 0.95f else 0.9f),

            secondary = lerp(seed, Color.Gray, if (expressive) 0.25f else 0.4f),
            onSecondary = Color.Black,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = lerp(seed, Color.White, 0.8f),

            tertiary = tertiary,
            onTertiary = Color.Black,
            tertiaryContainer = lerp(tertiary, Color.Black, if (expressive) 0.4f else 0.6f),
            onTertiaryContainer = lerp(tertiary, Color.White, 0.9f),

            surface = if (expressive) Color(0xFF0E0E18) else Color(0xFF111114),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = lerp(seed, Color.Black, if (expressive) 0.75f else 0.85f),
            onSurfaceVariant = Color(0xFFCAC4D0),
            surfaceTint = if (expressive) primary.copy(alpha = 0.08f) else Color.Transparent,

            outline = lerp(seed, Color.Gray, if (expressive) 0.35f else 0.5f),
            outlineVariant = lerp(seed, Color.Black, if (expressive) 0.6f else 0.75f),
            error = Color(0xFFF2B8B5)
        )
    } else {
        val primary = lerp(seed, Color.Black, boost * 0.5f)
        val tertiary = lerp(seed, Color(0xFF0055CC), if (expressive) 0.4f else 0.15f)

        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lerp(seed, Color.White, if (expressive) 0.65f else 0.8f),
            onPrimaryContainer = lerp(seed, Color.Black, if (expressive) 0.8f else 0.7f),

            secondary = lerp(seed, Color.Gray, if (expressive) 0.15f else 0.3f),
            onSecondary = Color.White,
            secondaryContainer = lerp(seed, Color.White, if (expressive) 0.8f else 0.9f),
            onSecondaryContainer = lerp(seed, Color.Black, 0.7f),

            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = lerp(tertiary, Color.White, if (expressive) 0.7f else 0.85f),
            onTertiaryContainer = lerp(tertiary, Color.Black, 0.7f),

            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = lerp(seed, Color.White, if (expressive) 0.88f else 0.95f),
            onSurfaceVariant = Color(0xFF49454F),
            surfaceTint = if (expressive) primary.copy(alpha = 0.06f) else Color.Transparent,

            outline = lerp(seed, Color.Gray, if (expressive) 0.25f else 0.4f),
            outlineVariant = lerp(seed, Color.White, if (expressive) 0.6f else 0.75f),
            error = Color(0xFFB3261E)
        )
    }
}

private fun applyExpressiveBoost(scheme: ColorScheme, isDark: Boolean): ColorScheme {
    return if (isDark) {
        scheme.copy(
            primaryContainer = lerp(scheme.primaryContainer, Color.Black, 0.15f),
            onPrimaryContainer = lerp(scheme.onPrimaryContainer, Color.White, 0.1f),
            secondaryContainer = lerp(scheme.secondaryContainer, scheme.primary, 0.08f),
            tertiaryContainer = lerp(scheme.tertiaryContainer, Color.Black, 0.1f),
            onTertiaryContainer = lerp(scheme.onTertiaryContainer, Color.White, 0.1f),
            surfaceVariant = lerp(scheme.surfaceVariant, scheme.primary, 0.06f),
            surface = lerp(scheme.surface, scheme.primary, 0.04f),
            surfaceTint = scheme.primary.copy(alpha = 0.08f),
            outline = lerp(scheme.outline, scheme.primary, 0.2f),
            outlineVariant = lerp(scheme.outlineVariant, scheme.primary, 0.15f),
        )
    } else {
        scheme.copy(
            primaryContainer = lerp(scheme.primaryContainer, scheme.primary, 0.12f),
            onPrimaryContainer = lerp(scheme.onPrimaryContainer, Color.Black, 0.08f),
            secondaryContainer = lerp(scheme.secondaryContainer, scheme.primary, 0.06f),
            tertiaryContainer = lerp(scheme.tertiaryContainer, scheme.tertiary, 0.1f),
            onTertiaryContainer = lerp(scheme.onTertiaryContainer, Color.Black, 0.05f),
            surfaceVariant = lerp(scheme.surfaceVariant, scheme.primary, 0.05f),
            surfaceTint = scheme.primary.copy(alpha = 0.06f),
            outline = lerp(scheme.outline, scheme.primary, 0.2f),
            outlineVariant = lerp(scheme.outlineVariant, scheme.primary, 0.1f),
        )
    }
}

@Composable
fun RustnithmTheme(
    themeMode: Int = 2,
    useDynamicColor: Boolean = true,
    useExpressive: Boolean = false,
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
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (useExpressive) applyExpressiveBoost(base, isDark) else base
        }
        else -> generateColorScheme(finalSeed, isDark, useExpressive)
    }

    val shapes = if (useExpressive) ExpressiveShapes else StandardShapes

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

    CompositionLocalProvider(LocalExpressive provides useExpressive) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}