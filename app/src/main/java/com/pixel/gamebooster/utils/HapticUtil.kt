package com.pixel.gamebooster.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType as ComposeHapticFeedbackType

object HapticUtil {
    val isAppHapticsEnabled = mutableStateOf(true)

    @Suppress("DEPRECATION")
    fun isSystemHapticsEnabled(context: Context): Boolean {
        return runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1,
            ) != 0
        }.getOrDefault(true)
    }

    fun gate(context: Context, delegate: HapticFeedback): HapticFeedback =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: ComposeHapticFeedbackType) {
                if (!isAppHapticsEnabled.value) return
                if (!isSystemHapticsEnabled(context)) return
                runCatching {
                    delegate.performHapticFeedback(hapticFeedbackType)
                }
            }
        }

    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (!isSystemHapticsEnabled(view.context)) return
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (!isSystemHapticsEnabled(view.context)) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    fun performClick(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (!isSystemHapticsEnabled(view.context)) return
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performToggle(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (!isSystemHapticsEnabled(view.context)) return
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun performSegment(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (!isSystemHapticsEnabled(view.context)) return
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
