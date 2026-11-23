package com.pixel.gamebooster.ui.util

import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutInfoTest {

    @Test
    fun phonePortraitClassificationMatchesExpectedFlags() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 412,
            screenHeightDp = 915,
        )

        assertFalse(info.isLandscape)
        assertFalse(info.isTablet)
        assertFalse(info.isWideScreen)
        assertFalse(info.isCompactHeight)
        assertEquals(412.dp, info.screenWidthDp)
        assertEquals(915.dp, info.screenHeightDp)
    }

    @Test
    fun phoneLandscapeClassificationEnablesCompactHeightAndWideScreen() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = 915,
            screenHeightDp = 412,
        )

        assertTrue(info.isLandscape)
        assertFalse(info.isTablet)
        assertTrue(info.isWideScreen)
        assertTrue(info.isCompactHeight)
        assertEquals(915.dp, info.screenWidthDp)
        assertEquals(412.dp, info.screenHeightDp)
    }

    @Test
    fun tabletPortraitClassificationEnablesTabletAndWideScreenWithoutCompactHeight() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 800,
            screenHeightDp = 1280,
        )

        assertFalse(info.isLandscape)
        assertTrue(info.isTablet)
        assertTrue(info.isWideScreen)
        assertFalse(info.isCompactHeight)
        assertEquals(800.dp, info.screenWidthDp)
        assertEquals(1280.dp, info.screenHeightDp)
    }

    @Test
    fun guardsAgainstNegativeDimensionsDuringRapidOrientationChange() {
        val info = computeAdaptiveLayoutInfo(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = -10,
            screenHeightDp = -5,
        )

        assertEquals(0.dp, info.screenWidthDp)
        assertEquals(0.dp, info.screenHeightDp)
        assertFalse(info.isTablet)
        assertTrue(info.isCompactHeight)
    }
}
