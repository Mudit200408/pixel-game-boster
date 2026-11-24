package com.pixel.gamebooster.overlay.model

enum class OverlayDisplayState {
    COLLAPSED_PILL,
    TOOLBAR,
    EXPANDED_PANEL
}

data class OverlayUiState(
    val packageName: String,
    val gameName: String,
    val displayState: OverlayDisplayState = OverlayDisplayState.COLLAPSED_PILL,
    val isDockedOnRight: Boolean = false,
    val overlayStyle: Int = 0, // 0=Bar, 1=Circle Button
    val overlayOpacity: Float = 0.94f, // 0.20f..1.0f
    val showLiveFps: Boolean = false,
    val overlayColorMode: Int = 0, // 0=Monet/Wallpaper, 1=Custom
    val overlayCustomColor: Int = 0xFF4285F4.toInt(),
    val layoutConfig: OverlayLayoutConfig = OverlayLayoutConfig.Default,
    val isImeVisible: Boolean = false,
    val interventionMode: Int = 2, // 3=Battery, 1=Balanced, 2=Boost
    val fpsOverride: Int = 0, // 0=Auto, 60, 90, 120
    val lockRefreshRate: Boolean = true,
    val dndEnabled: Boolean = false,
    val quickTapLocked: Boolean = true,
    val autoBrightnessLocked: Boolean = true,
    val backGestureLocked: Boolean = false,
    val cachedFreezerEnabled: Boolean = true,
    val wifiLowLatencyEnabled: Boolean = true,
    val wifiSuppressScansEnabled: Boolean = true,
    val wifiLockSufficiencyEnabled: Boolean = true,
    val wifiPreventArpEnabled: Boolean = true,
    val privateDnsBypassed: Boolean = true,
    val autoSyncPaused: Boolean = true,
    val batterySaverBlocked: Boolean = true,
    val appsSuspended: Boolean = false,
    val ramBoostActive: Boolean = false,
    val brightnessPercent: Float = 0.8f,
    val mediaVolumePercent: Float = 0.5f,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isCrossWindowBlurActive: Boolean = true,
    val currentFps: Int = 60,
    val temperatureC: Int = 38,
    val wattageW: Float = 4.2f,
    val isRecording: Boolean = false,
    val recordDoubleTapStop: Boolean = true,
)
