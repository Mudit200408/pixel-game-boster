package com.pixel.gamebooster.services

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.pixel.gamebooster.interruption.CallScreeningHelper
import com.pixel.gamebooster.utils.GameModeManager

/**
 * Intercepts incoming voice calls during active gameplay to prevent full-screen dialer interruption (REQ-05, D-06, D-07).
 *
 * Secured with [android.permission.BIND_SCREENING_SERVICE] so only the Android OS Telecom framework
 * can bind or deliver call details.
 */
class GameBoosterCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "PixelGameBooster:CallScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val isGameActive = GameModeManager.isGameModeActive
        val mode = CallScreeningHelper.activeCallHandlingMode
        Log.d(TAG, "Screening incoming call: isGameActive=$isGameActive, mode=$mode")

        val response = CallScreeningHelper.buildCallResponse(mode, isGameActive).build()
        respondToCall(callDetails, response)
    }
}
