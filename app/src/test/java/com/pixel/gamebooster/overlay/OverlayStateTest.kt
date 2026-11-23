package com.pixel.gamebooster.overlay

import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import com.pixel.gamebooster.overlay.model.OverlayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStateTest {

    @Test
    fun overlayStateCycleFromCollapsedToToolbarToExpandedAndBack() {
        var state = OverlayUiState(packageName = "com.example.game", gameName = "Test Game")
        assertEquals(OverlayDisplayState.COLLAPSED_PILL, state.displayState)

        state = state.copy(displayState = OverlayDisplayState.TOOLBAR)
        assertEquals(OverlayDisplayState.TOOLBAR, state.displayState)

        state = state.copy(displayState = OverlayDisplayState.EXPANDED_PANEL)
        assertEquals(OverlayDisplayState.EXPANDED_PANEL, state.displayState)

        state = state.copy(displayState = OverlayDisplayState.COLLAPSED_PILL)
        assertEquals(OverlayDisplayState.COLLAPSED_PILL, state.displayState)
    }

    @Test
    fun defaultOverlayUiStateHasExpectedDefaults() {
        val state = OverlayUiState(packageName = "com.pixel.game", gameName = "Pixel Shooter")
        assertEquals("com.pixel.game", state.packageName)
        assertEquals("Pixel Shooter", state.gameName)
        assertEquals(OverlayDisplayState.COLLAPSED_PILL, state.displayState)
        assertFalse(state.isDockedOnRight)
        assertEquals(2, state.interventionMode)
        assertEquals(0, state.fpsOverride)
        assertTrue(state.lockRefreshRate)
        assertFalse(state.dndEnabled)
        assertFalse(state.backGestureLocked)
        assertTrue(state.quickTapLocked)
        assertTrue(state.autoBrightnessLocked)
        assertEquals(0.8f, state.brightnessPercent, 0.001f)
        assertEquals(0.5f, state.mediaVolumePercent, 0.001f)
        assertEquals(100, state.batteryPercent)
        assertFalse(state.isCharging)
        assertTrue(state.isCrossWindowBlurActive)
    }

    @Test
    fun stateImmutabilityAndCopyUpdates() {
        val original = OverlayUiState(packageName = "com.pixel.game", gameName = "Pixel Shooter")
        val updated = original.copy(
            dndEnabled = true,
            brightnessPercent = 0.65f,
            fpsOverride = 120,
            interventionMode = 1
        )

        assertEquals("com.pixel.game", original.packageName)
        assertFalse(original.dndEnabled)
        assertEquals(0.8f, original.brightnessPercent, 0.001f)
        assertEquals(0, original.fpsOverride)
        assertEquals(2, original.interventionMode)

        assertTrue(updated.dndEnabled)
        assertEquals(0.65f, updated.brightnessPercent, 0.001f)
        assertEquals(120, updated.fpsOverride)
        assertEquals(1, updated.interventionMode)
    }

    @Test
    fun heroModeTransitionsAndToggleStateUpdates() {
        var state = OverlayUiState(packageName = "com.pixel.game", gameName = "Pixel Shooter")
        // Battery mode
        state = state.copy(interventionMode = 3)
        assertEquals(3, state.interventionMode)
        // Standard / Balanced mode
        state = state.copy(interventionMode = 1)
        assertEquals(1, state.interventionMode)
        // Boost / Performance mode
        state = state.copy(interventionMode = 2)
        assertEquals(2, state.interventionMode)

        // 2x2 toggles
        state = state.copy(
            dndEnabled = true,
            backGestureLocked = true,
            quickTapLocked = false,
            autoBrightnessLocked = false
        )
        assertTrue(state.dndEnabled)
        assertTrue(state.backGestureLocked)
        assertFalse(state.quickTapLocked)
        assertFalse(state.autoBrightnessLocked)
    }

    @Test
    fun crossWindowBlurFallbackState() {
        val blurActiveState = OverlayUiState(packageName = "com.pixel.game", gameName = "Game", isCrossWindowBlurActive = true)
        assertTrue(blurActiveState.isCrossWindowBlurActive)

        val blurFallbackState = OverlayUiState(packageName = "com.pixel.game", gameName = "Game", isCrossWindowBlurActive = false)
        assertFalse(blurFallbackState.isCrossWindowBlurActive)
    }
}
