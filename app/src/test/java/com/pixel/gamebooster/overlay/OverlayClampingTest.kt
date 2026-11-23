package com.pixel.gamebooster.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayClampingTest {

    @Test
    fun clampingWithinSafeDrawingPreventsOverlayOutOfScreenBounds() {
        val screenHeight = 2400
        val safeTop = 120
        val safeBottom = 160
        val minAllowedY = safeTop + 32
        val maxAllowedY = screenHeight - safeBottom - 128

        val rawTopY = 50
        val clampedTop = rawTopY.coerceIn(minAllowedY, maxAllowedY)
        assertEquals(minAllowedY, clampedTop)

        val rawBottomY = 2500
        val clampedBottom = rawBottomY.coerceIn(minAllowedY, maxAllowedY)
        assertEquals(maxAllowedY, clampedBottom)

        val validY = 1200
        val clampedValid = validY.coerceIn(minAllowedY, maxAllowedY)
        assertEquals(validY, clampedValid)
    }

    @Test
    fun clampVerticalTouchCalculationMatchesSafetyBoundaries() {
        val screenHeight = 1080
        val safeTop = 64
        val safeBottom = 96
        val minAllowedY = safeTop + 16
        val maxAllowedY = screenHeight - safeBottom - 64

        // Clamp below minimum
        val negativeY = -100
        assertEquals(minAllowedY, negativeY.coerceIn(minAllowedY, maxAllowedY))

        // Clamp above maximum
        val excessiveY = 2000
        assertEquals(maxAllowedY, excessiveY.coerceIn(minAllowedY, maxAllowedY))

        // In-bounds position remains untouched
        val midY = 500
        assertEquals(500, midY.coerceIn(minAllowedY, maxAllowedY))
    }

    @Test
    fun gameOverlayManagerClampYMatchesBounds() {
        val screenHeight = 1080
        val safeTop = 64
        val safeBottom = 96
        val clampedMin = GameOverlayManager.clampY(-50, screenHeight, safeTop, safeBottom, minOffset = 16, maxOffset = 64)
        assertEquals(safeTop + 16, clampedMin)

        val clampedMax = GameOverlayManager.clampY(3000, screenHeight, safeTop, safeBottom, minOffset = 16, maxOffset = 64)
        assertEquals(screenHeight - safeBottom - 64, clampedMax)

        val clampedValid = GameOverlayManager.clampY(400, screenHeight, safeTop, safeBottom, minOffset = 16, maxOffset = 64)
        assertEquals(400, clampedValid)
    }
}
