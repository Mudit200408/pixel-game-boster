package com.pixel.gamebooster.data.model

/**
 * Audio capture modes supported by the in-game screen recorder.
 */
enum class RecordAudioSource(val value: Int, val label: String) {
    MUTE(0, "Mute"),
    MIC(1, "Mic"),
    DEVICE(2, "Device Audio"),
    BOTH(3, "Device + Mic");

    companion object {
        fun fromValue(value: Int): RecordAudioSource = entries.find { it.value == value } ?: DEVICE
    }
}

/**
 * Video recording resolution, frame rate, and bitrate presets.
 */
enum class RecordResolution(
    val value: Int,
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
) {
    FHD_60(0, "1080p 60fps", 1920, 1080, 60, 16_000_000),
    FHD_30(1, "1080p 30fps", 1920, 1080, 30, 10_000_000),
    HD_60(2, "720p 60fps", 1280, 720, 60, 8_000_000);

    companion object {
        fun fromValue(value: Int): RecordResolution = entries.find { it.value == value } ?: FHD_60
    }
}

/**
 * Screen recorder configuration preset.
 */
data class ScreenRecordConfig(
    val audioSource: RecordAudioSource = RecordAudioSource.DEVICE,
    val resolution: RecordResolution = RecordResolution.FHD_60,
    val showTouches: Boolean = false,
)
