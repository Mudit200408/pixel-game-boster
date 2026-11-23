package com.pixel.gamebooster.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.Log
import android.view.Display

object RefreshRateUtils {
    private const val TAG = "PixelGameBooster:RR"

    data class RefreshRateSnapshot(
        val minRate: Float,
        val peakRate: Float,
    )

    fun getCurrentSnapshot(context: Context): RefreshRateSnapshot {
        val min = runCatching {
            Settings.System.getFloat(context.contentResolver, "min_refresh_rate")
        }.getOrDefault(60f)
        val peak = runCatching {
            Settings.System.getFloat(context.contentResolver, "peak_refresh_rate")
        }.getOrDefault(120f)
        return RefreshRateSnapshot(min, peak)
    }

    fun applyFixedRefreshRate(context: Context, rate: Float = 120f) {
        val formatted = rate.toInt().toString()
        val cmd = "settings put system min_refresh_rate $formatted && " +
            "settings put system peak_refresh_rate $formatted && " +
            "settings put global min_refresh_rate $formatted && " +
            "settings put global peak_refresh_rate $formatted"
        ShellUtils.runCommand(context, cmd)
    }

    fun lockPreferredDisplayMode(context: Context, rate: Int = 120): Boolean {
        return try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val mode = display?.mode
            val width = mode?.physicalWidth ?: 1080
            val height = mode?.physicalHeight ?: 2400

            // cmd display set-user-preferred-display-mode <width> <height> <refreshRate> <displayId>
            val cmd = "cmd display set-user-preferred-display-mode $width $height $rate 0"
            ShellUtils.runCommand(context, cmd)
            Log.d(TAG, "Locked physical display mode to ${width}x${height}@${rate}Hz")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock physical display mode", e)
            false
        }
    }

    fun resetPreferredDisplayMode(context: Context) {
        ShellUtils.runCommand(context, "cmd display clear-user-preferred-display-mode 0")
        Log.d(TAG, "Cleared user preferred display mode")
    }

    fun restoreRefreshRate(context: Context, snapshot: RefreshRateSnapshot?) {
        val min = snapshot?.minRate?.toInt() ?: 60
        val peak = snapshot?.peakRate?.toInt() ?: 120
        val cmd = "settings put system min_refresh_rate $min && " +
            "settings put system peak_refresh_rate $peak && " +
            "settings put global min_refresh_rate $min && " +
            "settings put global peak_refresh_rate $peak"
        ShellUtils.runCommand(context, cmd)
        resetPreferredDisplayMode(context)
    }
}
