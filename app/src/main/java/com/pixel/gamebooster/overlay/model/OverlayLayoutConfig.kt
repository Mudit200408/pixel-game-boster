package com.pixel.gamebooster.overlay.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

enum class OverlayBlockType {
    TELEMETRY_PILL,
    MODE_SWITCHER,
    ACTION_TILES,
    BRIGHTNESS_SLIDER,
    VOLUME_SLIDER,
}

data class OverlayBlockConfig(
    @SerializedName("type") val type: OverlayBlockType,
    @SerializedName("enabled") val enabled: Boolean = true,
)

enum class TelemetryMetric {
    FPS,
    TEMPERATURE,
    WATTAGE,
    BATTERY,
}

enum class OverlayTileCategory {
    OVERLAY,
    DISPLAY,
    PERFORMANCE,
    NETWORK,
    SUSPENSION,
}

enum class OverlayTileType(val category: OverlayTileCategory) {
    // Standard / Overlay
    LOCK_120HZ(OverlayTileCategory.OVERLAY),
    GAME_DND(OverlayTileCategory.OVERLAY),
    SCREEN_RECORD(OverlayTileCategory.OVERLAY),
    SCREENSHOT(OverlayTileCategory.OVERLAY),
    LIVE_FPS(OverlayTileCategory.OVERLAY),

    // Display & Input
    AUTO_BRIGHTNESS_LOCK(OverlayTileCategory.DISPLAY),
    BACK_GESTURE_LOCK(OverlayTileCategory.DISPLAY),
    QUICK_TAP_LOCK(OverlayTileCategory.DISPLAY),

    // Performance & Memory
    BOOST_RAM(OverlayTileCategory.PERFORMANCE),
    CACHED_FREEZER(OverlayTileCategory.PERFORMANCE),

    // Network & Power
    WIFI_LOW_LATENCY(OverlayTileCategory.NETWORK),
    WIFI_SUPPRESS_SCANS(OverlayTileCategory.NETWORK),
    WIFI_LOCK_SUFFICIENCY(OverlayTileCategory.NETWORK),
    WIFI_PREVENT_ARP(OverlayTileCategory.NETWORK),
    PRIVATE_DNS_BYPASS(OverlayTileCategory.NETWORK),
    PAUSE_AUTO_SYNC(OverlayTileCategory.NETWORK),
    PAUSE_AIRSYNC(OverlayTileCategory.NETWORK),
    BLOCK_BATTERY_SAVER(OverlayTileCategory.NETWORK),

    // Suspension
    SUSPEND_APPS(OverlayTileCategory.SUSPENSION),
}

data class OverlayLayoutConfig(
    @SerializedName("blocks") val blocks: List<OverlayBlockConfig> = defaultBlocks(),
    @SerializedName("tiles") val tiles: List<OverlayTileType> = defaultTiles(),
    @SerializedName("telemetryMetrics") val telemetryMetrics: List<TelemetryMetric> = defaultTelemetryMetrics(),
) {
    val isTelemetryPillVisible: Boolean
        get() = blocks.any { it.type == OverlayBlockType.TELEMETRY_PILL && it.enabled } &&
            telemetryMetrics.isNotEmpty()

    fun isMetricVisible(metric: TelemetryMetric): Boolean =
        isTelemetryPillVisible && telemetryMetrics.contains(metric)

    val isAnyBatteryTelemetryVisible: Boolean
        get() = isTelemetryPillVisible && telemetryMetrics.any {
            it == TelemetryMetric.TEMPERATURE ||
                it == TelemetryMetric.WATTAGE ||
                it == TelemetryMetric.BATTERY
        }

    fun moveBlock(fromIndex: Int, toIndex: Int): OverlayLayoutConfig {
        if (fromIndex == toIndex || fromIndex !in blocks.indices || toIndex !in blocks.indices) {
            return this
        }
        val updated = blocks.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        return copy(blocks = updated)
    }

    fun setBlockEnabled(type: OverlayBlockType, enabled: Boolean): OverlayLayoutConfig {
        val updated = blocks.map { block ->
            if (block.type == type) block.copy(enabled = enabled) else block
        }
        return copy(blocks = updated)
    }

    fun setTelemetryMetricEnabled(metric: TelemetryMetric, enabled: Boolean): OverlayLayoutConfig {
        val updated = if (enabled) {
            if (telemetryMetrics.contains(metric)) telemetryMetrics else {
                // Keep canonical order of TelemetryMetric.entries
                TelemetryMetric.entries.filter { it == metric || telemetryMetrics.contains(it) }
            }
        } else {
            telemetryMetrics.filterNot { it == metric }
        }
        return copy(telemetryMetrics = updated)
    }

    fun moveTile(fromIndex: Int, toIndex: Int): OverlayLayoutConfig {
        if (fromIndex == toIndex || fromIndex !in tiles.indices || toIndex !in tiles.indices) {
            return this
        }
        val updated = tiles.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        return copy(tiles = updated)
    }

    fun addTile(tile: OverlayTileType): OverlayLayoutConfig {
        if (tiles.contains(tile)) return this
        return copy(tiles = tiles + tile)
    }

    fun removeTile(tile: OverlayTileType): OverlayLayoutConfig {
        return copy(tiles = tiles.filterNot { it == tile })
    }

    fun replaceTile(index: Int, newTile: OverlayTileType): OverlayLayoutConfig {
        if (index !in tiles.indices) return this
        val existingIndex = tiles.indexOf(newTile)
        val updated = tiles.toMutableList()
        if (existingIndex >= 0 && existingIndex != index) {
            // Swap positions if the chosen tile is already active
            val oldTile = updated[index]
            updated[index] = newTile
            updated[existingIndex] = oldTile
        } else {
            updated[index] = newTile
        }
        return copy(tiles = updated)
    }

    /**
     * Ensures every OverlayBlockType appears exactly once (preserving saved order),
     * deduplicates tiles and metrics, and protects against nulls from malformed JSON.
     */
    fun sanitized(): OverlayLayoutConfig {
        val safeBlocks = blocks.orEmpty().filterNotNull().filter { (it.type as OverlayBlockType?) != null }
        val seenTypes = mutableSetOf<OverlayBlockType>()
        val normalizedBlocks = mutableListOf<OverlayBlockConfig>()
        for (block in safeBlocks) {
            if (seenTypes.add(block.type)) {
                normalizedBlocks.add(block)
            }
        }
        for (type in OverlayBlockType.entries) {
            if (seenTypes.add(type)) {
                normalizedBlocks.add(OverlayBlockConfig(type = type, enabled = true))
            }
        }

        val safeTiles = tiles.orEmpty().filterNotNull().distinct()
        val safeMetrics = telemetryMetrics.orEmpty().filterNotNull().distinct()

        return OverlayLayoutConfig(
            blocks = normalizedBlocks,
            tiles = safeTiles,
            telemetryMetrics = safeMetrics,
        )
    }

    fun toJson(): String = gson.toJson(this)

    companion object {
        private val gson = Gson()

        fun defaultBlocks(): List<OverlayBlockConfig> = listOf(
            OverlayBlockConfig(OverlayBlockType.TELEMETRY_PILL, enabled = true),
            OverlayBlockConfig(OverlayBlockType.MODE_SWITCHER, enabled = true),
            OverlayBlockConfig(OverlayBlockType.ACTION_TILES, enabled = true),
            OverlayBlockConfig(OverlayBlockType.BRIGHTNESS_SLIDER, enabled = true),
            OverlayBlockConfig(OverlayBlockType.VOLUME_SLIDER, enabled = true),
        )

        fun defaultTiles(): List<OverlayTileType> = listOf(
            OverlayTileType.LOCK_120HZ,
            OverlayTileType.GAME_DND,
            OverlayTileType.SCREEN_RECORD,
            OverlayTileType.AUTO_BRIGHTNESS_LOCK,
            OverlayTileType.LIVE_FPS,
            OverlayTileType.BACK_GESTURE_LOCK,
        )

        fun defaultTelemetryMetrics(): List<TelemetryMetric> = TelemetryMetric.entries.toList()

        val Default: OverlayLayoutConfig = OverlayLayoutConfig(
            blocks = defaultBlocks(),
            tiles = defaultTiles(),
            telemetryMetrics = defaultTelemetryMetrics(),
        )

        fun fromJson(json: String?): OverlayLayoutConfig {
            if (json.isNullOrBlank()) return Default
            return runCatching {
                gson.fromJson(json, OverlayLayoutConfig::class.java)?.sanitized() ?: Default
            }.getOrDefault(Default)
        }
    }
}
