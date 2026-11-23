package com.pixel.gamebooster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.pixel.gamebooster.utils.HapticUtil

private val ExpressiveDarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        surfaceContainerLowest = Color(0xFF0D0E11),
        surfaceContainerLow = Color(0xFF1A1B1F),
        surfaceContainer = Color(0xFF1E1F23),
        surfaceContainerHigh = Color(0xFF292A2E),
        surfaceContainerHighest = Color(0xFF333539),
    )

private val ExpressiveLightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F3F7),
        surfaceContainer = Color(0xFFEEEDF2),
        surfaceContainerHigh = Color(0xFFE8E7EC),
        surfaceContainerHighest = Color(0xFFE2E2E6),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelGameBoosterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val dynamicScheme =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (darkTheme && pitchBlackTheme) {
                    dynamicScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                    )
                } else {
                    dynamicScheme
                }
            }
            darkTheme -> {
                if (pitchBlackTheme) {
                    ExpressiveDarkColorScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                    )
                } else {
                    ExpressiveDarkColorScheme
                }
            }
            else -> ExpressiveLightColorScheme
        }

    val gatedHaptics = HapticUtil.gate(context, LocalHapticFeedback.current)
    CompositionLocalProvider(LocalHapticFeedback provides gatedHaptics) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
