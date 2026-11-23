package com.pixel.gamebooster.services

import com.pixel.gamebooster.interruption.CallScreeningHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test suite verifying Telecom CallResponse builder logic across game states and modes (REQ-05, D-06, D-07).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallScreeningTest {

    @Test
    fun inactiveGameMode_passesCallThroughWithoutDisallowOrSilence() {
        // Test 1: When game mode is inactive, CallResponse passes calls through without disallowing or silencing
        val response = CallScreeningHelper.buildCallResponse(
            mode = CallScreeningHelper.MODE_REJECT,
            isGameActive = false,
        ).build()

        assertFalse("Inactive game mode must not disallow calls", response.disallowCall)
        assertFalse("Inactive game mode must not reject calls", response.rejectCall)
        assertFalse("Inactive game mode must not silence calls", response.silenceCall)
    }

    @Test
    fun activeGameMode_headsUp_allowsCallThroughForHeadsUpBanner() {
        // Test 2: In active game mode with mode 0 (Heads-Up), CallResponse allows call through
        // so DND full-screen intent suppression keeps it in heads-up view per D-06
        val response = CallScreeningHelper.buildCallResponse(
            mode = CallScreeningHelper.MODE_HEADS_UP,
            isGameActive = true,
        ).build()

        assertFalse("Heads-Up mode must not disallow incoming calls", response.disallowCall)
        assertFalse("Heads-Up mode must not reject incoming calls", response.rejectCall)
        assertFalse("Heads-Up mode must not silence calls", response.silenceCall)
    }

    @Test
    fun activeGameMode_silentRinging_silencesCallWithoutDisallowing() {
        // Test 3: In active game mode with mode 1 (Silent Ringing), CallResponse sets silenceCall = true and disallowCall = false
        val response = CallScreeningHelper.buildCallResponse(
            mode = CallScreeningHelper.MODE_SILENT,
            isGameActive = true,
        ).build()

        assertFalse("Silent ringing mode must not disallow calls", response.disallowCall)
        assertFalse("Silent ringing mode must not reject calls", response.rejectCall)
        assertTrue("Silent ringing mode must silence call audio/vibration", response.silenceCall)
    }

    @Test
    fun activeGameMode_autoReject_disallowsAndRejectsCall() {
        // Test 4: In active game mode with mode 2 (Auto-Reject), CallResponse sets disallowCall = true and rejectCall = true
        val response = CallScreeningHelper.buildCallResponse(
            mode = CallScreeningHelper.MODE_REJECT,
            isGameActive = true,
        ).build()

        assertTrue("Auto-reject mode must disallow calls", response.disallowCall)
        assertTrue("Auto-reject mode must terminate/reject calls", response.rejectCall)
        assertTrue("Auto-reject mode must skip notification", response.skipNotification)
        assertFalse("Auto-reject mode must preserve call log entry", response.skipCallLog)
    }
}
