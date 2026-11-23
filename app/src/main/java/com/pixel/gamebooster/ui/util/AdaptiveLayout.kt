package com.pixel.gamebooster.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AdaptiveLayoutInfo(
    val isLandscape: Boolean,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isTablet: Boolean,
    val isWideScreen: Boolean,
    val isCompactHeight: Boolean,
)

/**
 * Pure function to compute [AdaptiveLayoutInfo] from raw configuration parameters.
 * Clamps negative dimensions to 0.dp (T-11-01).
 */
fun computeAdaptiveLayoutInfo(
    orientation: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): AdaptiveLayoutInfo {
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    val clampedWidth = maxOf(0, screenWidthDp)
    val clampedHeight = maxOf(0, screenHeightDp)
    val widthDp = clampedWidth.dp
    val heightDp = clampedHeight.dp
    val isTablet = minOf(clampedWidth, clampedHeight) >= 600
    val isWideScreen = isLandscape || isTablet
    val isCompactHeight = clampedHeight < 500

    return AdaptiveLayoutInfo(
        isLandscape = isLandscape,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        isTablet = isTablet,
        isWideScreen = isWideScreen,
        isCompactHeight = isCompactHeight,
    )
}

/**
 * Computes responsive breakpoint information from [LocalConfiguration.current].
 * - isLandscape: orientation == Configuration.ORIENTATION_LANDSCAPE
 * - screenWidthDp / screenHeightDp: clamped to at least 0.dp to guard against transient zero/negative configs (T-11-01)
 * - isTablet: minOf(screenWidthDp, screenHeightDp) >= 600.dp (sw600dp standard)
 * - isWideScreen: isLandscape || isTablet
 * - isCompactHeight: screenHeightDp < 500.dp
 */
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val config = LocalConfiguration.current
    return remember(config.orientation, config.screenWidthDp, config.screenHeightDp) {
        computeAdaptiveLayoutInfo(
            orientation = config.orientation,
            screenWidthDp = config.screenWidthDp,
            screenHeightDp = config.screenHeightDp,
        )
    }
}
