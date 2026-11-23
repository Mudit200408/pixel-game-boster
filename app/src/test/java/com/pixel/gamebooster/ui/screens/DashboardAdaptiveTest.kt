package com.pixel.gamebooster.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.ui.util.computeAdaptiveLayoutInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardAdaptiveTest {

    @Test
    fun phonePortraitRetainsSinglePaneWithStandardClearance() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 412,
            screenHeightDp = 915,
        )

        assertFalse("Phone portrait should not enable two-pane mode", isDashboardTwoPane(info))
        assertEquals(96.dp, getDashboardBottomPadding(info))
        assertEquals(285.dp, getDashboardLottieHeight(info))
    }

    @Test
    fun phoneLandscapeEnablesTwoPaneWithCompactHeightClearanceAndScaledLottie() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = 915,
            screenHeightDp = 412,
        )

        assertTrue("Phone landscape should enable two-pane mode", isDashboardTwoPane(info))
        assertEquals(68.dp, getDashboardBottomPadding(info))
        assertEquals(160.dp, getDashboardLottieHeight(info))
    }

    @Test
    fun tabletPortraitEnablesTwoPaneWithStandardClearanceAndTabletLottie() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 800,
            screenHeightDp = 1280,
        )

        assertTrue("Tablet portrait should enable two-pane mode", isDashboardTwoPane(info))
        assertEquals(96.dp, getDashboardBottomPadding(info))
        assertEquals(280.dp, getDashboardLottieHeight(info))
    }

    @Test
    fun tabletLandscapeEnablesTwoPaneWithStandardClearanceAndTabletLottie() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = 1280,
            screenHeightDp = 800,
        )

        assertTrue("Tablet landscape should enable two-pane mode", isDashboardTwoPane(info))
        assertEquals(96.dp, getDashboardBottomPadding(info))
        assertEquals(280.dp, getDashboardLottieHeight(info))
    }

    @Test
    fun foldableUnfoldedEnablesTwoPaneWithStandardClearance() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 673,
            screenHeightDp = 841,
        )

        assertTrue("Foldable unfolded portrait should enable two-pane mode", isDashboardTwoPane(info))
        assertEquals(96.dp, getDashboardBottomPadding(info))
        assertEquals(280.dp, getDashboardLottieHeight(info))
    }
}
