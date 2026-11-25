package com.pixel.gamebooster.data.model

/**
 * Actions that can be bound to Edge Bar / Floating Button gestures (Double Tap & Long Tap).
 */
enum class OverlayGestureAction(val prefValue: String) {
    NONE("none"),
    REVEAL_OVERLAY("reveal_overlay"),
    TOGGLE_RECORDING("toggle_recording"),
    TAKE_SCREENSHOT("take_screenshot"),
    TOGGLE_FPS("toggle_fps"),
    PURGE_RAM("purge_ram"),
    CYCLE_PERFORMANCE_MODE("cycle_performance_mode");

    companion object {
        fun fromPrefValue(value: String?): OverlayGestureAction {
            return entries.firstOrNull { it.prefValue == value } ?: NONE
        }
    }
}
