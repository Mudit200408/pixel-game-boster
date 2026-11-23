package com.pixel.gamebooster.interruption

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.telecom.CallScreeningService.CallResponse

/**
 * Helper utility for Call Screening role management and Telecom CallResponse construction (REQ-05, D-06, D-07).
 */
object CallScreeningHelper {
    const val MODE_HEADS_UP = 0
    const val MODE_SILENT = 1
    const val MODE_REJECT = 2

    @Volatile
    var activeCallHandlingMode: Int = MODE_HEADS_UP

    /**
     * Checks if Pixel Game Booster holds the system ROLE_CALL_SCREENING role.
     */
    fun isCallScreeningRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    /**
     * Creates an Intent to prompt the user to grant ROLE_CALL_SCREENING.
     */
    fun getRequestRoleIntent(context: Context): Intent {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING) ?: Intent()
    }

    /**
     * Builds a [CallResponse.Builder] based on the active call handling mode and game state.
     *
     * @param mode Call handling mode (0 = Heads-Up, 1 = Silent Ringing, 2 = Auto-Reject).
     * @param isGameActive True if game booster is actively running a game session.
     */
    fun buildCallResponse(mode: Int, isGameActive: Boolean): CallResponse.Builder {
        val builder = CallResponse.Builder()
        if (!isGameActive) {
            return builder.setDisallowCall(false)
        }

        when (mode) {
            MODE_REJECT -> {
                builder
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false)
            }
            MODE_SILENT -> {
                builder
                    .setDisallowCall(false)
                    .setSilenceCall(true)
            }
            else -> { // MODE_HEADS_UP (0)
                builder.setDisallowCall(false)
            }
        }
        return builder
    }
}
