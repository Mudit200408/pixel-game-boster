package com.pixel.gamebooster

import com.pixel.gamebooster.overlay.model.OverlayBlockType
import com.pixel.gamebooster.overlay.model.OverlayLayoutConfig
import com.pixel.gamebooster.overlay.model.OverlayTileType
import com.pixel.gamebooster.overlay.model.TelemetryMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayLayoutConfigTest {

    @Test
    fun defaultConfig_hasAllBlocksAndStandardSixTiles() {
        val config = OverlayLayoutConfig.Default
        assertEquals(
            listOf(
                OverlayBlockType.TELEMETRY_PILL,
                OverlayBlockType.MODE_SWITCHER,
                OverlayBlockType.ACTION_TILES,
                OverlayBlockType.BRIGHTNESS_SLIDER,
                OverlayBlockType.VOLUME_SLIDER,
            ),
            config.blocks.map { it.type },
        )
        assertTrue(config.blocks.all { it.enabled })
        assertEquals(6, config.tiles.size)
        assertTrue(config.isTelemetryPillVisible)
        assertTrue(config.isMetricVisible(TelemetryMetric.FPS))
        assertTrue(config.isAnyBatteryTelemetryVisible)
    }

    @Test
    fun moveBlock_reordersBlocksAccurately() {
        val moved = OverlayLayoutConfig.Default
            .moveBlock(0, 4) // Move Telemetry Pill to bottom
            .moveBlock(3, 2) // Move Volume Slider above Brightness Slider

        assertEquals(OverlayBlockType.TELEMETRY_PILL, moved.blocks.last().type)
        val brightnessIdx = moved.blocks.indexOfFirst { it.type == OverlayBlockType.BRIGHTNESS_SLIDER }
        val volumeIdx = moved.blocks.indexOfFirst { it.type == OverlayBlockType.VOLUME_SLIDER }
        assertTrue(volumeIdx < brightnessIdx)
    }

    @Test
    fun disableTelemetryPill_hidesAllTelemetryAndMetrics() {
        val disabled = OverlayLayoutConfig.Default.setBlockEnabled(OverlayBlockType.TELEMETRY_PILL, false)
        assertFalse(disabled.isTelemetryPillVisible)
        assertFalse(disabled.isMetricVisible(TelemetryMetric.FPS))
        assertFalse(disabled.isAnyBatteryTelemetryVisible)
    }

    @Test
    fun toggleTelemetryMetrics_updatesVisibilityGranularly() {
        val fpsOnly = OverlayLayoutConfig.Default
            .setTelemetryMetricEnabled(TelemetryMetric.TEMPERATURE, false)
            .setTelemetryMetricEnabled(TelemetryMetric.WATTAGE, false)
            .setTelemetryMetricEnabled(TelemetryMetric.BATTERY, false)

        assertTrue(fpsOnly.isTelemetryPillVisible)
        assertTrue(fpsOnly.isMetricVisible(TelemetryMetric.FPS))
        assertFalse(fpsOnly.isAnyBatteryTelemetryVisible)

        val none = fpsOnly.setTelemetryMetricEnabled(TelemetryMetric.FPS, false)
        assertFalse(none.isTelemetryPillVisible)
    }

    @Test
    fun tileMutations_addRemoveReplaceAndReorder() {
        val added = OverlayLayoutConfig.Default.addTile(OverlayTileType.PRIVATE_DNS_BYPASS)
        assertEquals(7, added.tiles.size)
        assertEquals(OverlayTileType.PRIVATE_DNS_BYPASS, added.tiles.last())

        // Adding duplicate is a no-op
        assertEquals(7, added.addTile(OverlayTileType.PRIVATE_DNS_BYPASS).tiles.size)

        // Replace index 0 (LOCK_120HZ) with WIFI_LOW_LATENCY
        val replaced = added.replaceTile(0, OverlayTileType.WIFI_LOW_LATENCY)
        assertEquals(OverlayTileType.WIFI_LOW_LATENCY, replaced.tiles[0])
        assertFalse(replaced.tiles.contains(OverlayTileType.LOCK_120HZ))

        // Replace with already-active tile swaps positions
        val swapped = replaced.replaceTile(0, OverlayTileType.GAME_DND)
        assertEquals(OverlayTileType.GAME_DND, swapped.tiles[0])
        assertEquals(OverlayTileType.WIFI_LOW_LATENCY, swapped.tiles[1])

        // Move and remove
        val moved = swapped.moveTile(0, 2)
        assertEquals(OverlayTileType.GAME_DND, moved.tiles[2])
        val removed = moved.removeTile(OverlayTileType.GAME_DND)
        assertFalse(removed.tiles.contains(OverlayTileType.GAME_DND))
    }

    @Test
    fun jsonSerializationRoundTripAndCorruptFallback() {
        val custom = OverlayLayoutConfig.Default
            .moveBlock(4, 0)
            .setBlockEnabled(OverlayBlockType.TELEMETRY_PILL, false)
            .removeTile(OverlayTileType.SCREEN_RECORD)
            .addTile(OverlayTileType.BOOST_RAM)
            .addTile(OverlayTileType.PAUSE_AUTO_SYNC)

        val restored = OverlayLayoutConfig.fromJson(custom.toJson())
        assertEquals(custom, restored)

        assertEquals(OverlayLayoutConfig.Default, OverlayLayoutConfig.fromJson(null))
        assertEquals(OverlayLayoutConfig.Default, OverlayLayoutConfig.fromJson("{broken json"))
    }
}
