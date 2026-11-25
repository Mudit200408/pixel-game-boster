package com.pixel.gamebooster.overlay.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiLock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.pixel.gamebooster.R
import com.pixel.gamebooster.overlay.model.OverlayBlockType
import com.pixel.gamebooster.overlay.model.OverlayTileCategory
import com.pixel.gamebooster.overlay.model.OverlayTileType
import com.pixel.gamebooster.overlay.model.OverlayUiState
import com.pixel.gamebooster.overlay.model.TelemetryMetric

@StringRes
fun OverlayTileType.labelRes(): Int = when (this) {
    OverlayTileType.LOCK_120HZ -> R.string.perf_force_120hz_title
    OverlayTileType.GAME_DND -> R.string.interruption_dnd_toggle_title
    OverlayTileType.SCREEN_RECORD -> R.string.overlay_record
    OverlayTileType.LIVE_FPS -> R.string.overlay_show_live_fps_title
    OverlayTileType.AUTO_BRIGHTNESS_LOCK -> R.string.perf_disable_auto_brightness_title
    OverlayTileType.BACK_GESTURE_LOCK -> R.string.perf_lock_back_gesture_title
    OverlayTileType.QUICK_TAP_LOCK -> R.string.freezer_disable_quick_tap_title
    OverlayTileType.BOOST_RAM -> R.string.perf_purge_ram_title
    OverlayTileType.CACHED_FREEZER -> R.string.perf_cached_freezer_title
    OverlayTileType.WIFI_LOW_LATENCY -> R.string.net_low_latency_title
    OverlayTileType.WIFI_SUPPRESS_SCANS -> R.string.net_suppress_scans_title
    OverlayTileType.WIFI_LOCK_SUFFICIENCY -> R.string.net_lock_sufficiency_title
    OverlayTileType.WIFI_PREVENT_ARP -> R.string.net_prevent_arp_title
    OverlayTileType.PRIVATE_DNS_BYPASS -> R.string.net_disable_private_dns_title
    OverlayTileType.PAUSE_AUTO_SYNC -> R.string.net_pause_sync_title
    OverlayTileType.BLOCK_BATTERY_SAVER -> R.string.net_block_battery_saver_title
    OverlayTileType.SUSPEND_APPS -> R.string.freezer_header_title
}

@Composable
fun OverlayTileType.iconVector(): ImageVector = when (this) {
    OverlayTileType.LOCK_120HZ -> ImageVector.vectorResource(R.drawable.ic_120hz)
    OverlayTileType.GAME_DND -> Icons.Rounded.DoNotDisturbOn
    OverlayTileType.SCREEN_RECORD -> Icons.Rounded.Videocam
    OverlayTileType.LIVE_FPS -> Icons.Rounded.Speed
    OverlayTileType.AUTO_BRIGHTNESS_LOCK -> Icons.Rounded.LightMode
    OverlayTileType.BACK_GESTURE_LOCK -> Icons.Rounded.TouchApp
    OverlayTileType.QUICK_TAP_LOCK -> Icons.Rounded.TouchApp
    OverlayTileType.BOOST_RAM -> Icons.Rounded.CleaningServices
    OverlayTileType.CACHED_FREEZER -> Icons.Rounded.AcUnit
    OverlayTileType.WIFI_LOW_LATENCY -> Icons.Rounded.Wifi
    OverlayTileType.WIFI_SUPPRESS_SCANS -> Icons.Rounded.Radar
    OverlayTileType.WIFI_LOCK_SUFFICIENCY -> Icons.Rounded.WifiLock
    OverlayTileType.WIFI_PREVENT_ARP -> Icons.Rounded.Security
    OverlayTileType.PRIVATE_DNS_BYPASS -> Icons.Rounded.Dns
    OverlayTileType.PAUSE_AUTO_SYNC -> Icons.Rounded.SyncDisabled
    OverlayTileType.BLOCK_BATTERY_SAVER -> Icons.Rounded.BatteryChargingFull
    OverlayTileType.SUSPEND_APPS -> Icons.Rounded.Block
}

fun OverlayTileType.isActiveIn(state: OverlayUiState): Boolean = when (this) {
    OverlayTileType.LOCK_120HZ -> state.lockRefreshRate
    OverlayTileType.GAME_DND -> state.dndEnabled
    OverlayTileType.SCREEN_RECORD -> state.isRecording
    OverlayTileType.LIVE_FPS -> state.showLiveFps
    OverlayTileType.AUTO_BRIGHTNESS_LOCK -> state.autoBrightnessLocked
    OverlayTileType.BACK_GESTURE_LOCK -> state.backGestureLocked
    OverlayTileType.QUICK_TAP_LOCK -> state.quickTapLocked
    OverlayTileType.BOOST_RAM -> state.ramBoostActive
    OverlayTileType.CACHED_FREEZER -> state.cachedFreezerEnabled
    OverlayTileType.WIFI_LOW_LATENCY -> state.wifiLowLatencyEnabled
    OverlayTileType.WIFI_SUPPRESS_SCANS -> state.wifiSuppressScansEnabled
    OverlayTileType.WIFI_LOCK_SUFFICIENCY -> state.wifiLockSufficiencyEnabled
    OverlayTileType.WIFI_PREVENT_ARP -> state.wifiPreventArpEnabled
    OverlayTileType.PRIVATE_DNS_BYPASS -> state.privateDnsBypassed
    OverlayTileType.PAUSE_AUTO_SYNC -> state.autoSyncPaused
    OverlayTileType.BLOCK_BATTERY_SAVER -> state.batterySaverBlocked
    OverlayTileType.SUSPEND_APPS -> state.appsSuspended
}

@StringRes
fun OverlayTileCategory.labelRes(): Int = when (this) {
    OverlayTileCategory.OVERLAY -> R.string.overlay_layout_cat_overlay
    OverlayTileCategory.DISPLAY -> R.string.overlay_layout_cat_display
    OverlayTileCategory.PERFORMANCE -> R.string.overlay_layout_cat_performance
    OverlayTileCategory.NETWORK -> R.string.overlay_layout_cat_network
    OverlayTileCategory.SUSPENSION -> R.string.overlay_layout_cat_suspension
}

@StringRes
fun OverlayBlockType.titleRes(): Int = when (this) {
    OverlayBlockType.TELEMETRY_PILL -> R.string.overlay_layout_block_telemetry
    OverlayBlockType.MODE_SWITCHER -> R.string.overlay_layout_block_mode
    OverlayBlockType.ACTION_TILES -> R.string.overlay_layout_block_tiles
    OverlayBlockType.BRIGHTNESS_SLIDER -> R.string.overlay_layout_block_brightness
    OverlayBlockType.VOLUME_SLIDER -> R.string.overlay_layout_block_volume
}

@StringRes
fun TelemetryMetric.labelRes(): Int = when (this) {
    TelemetryMetric.FPS -> R.string.overlay_layout_metric_fps
    TelemetryMetric.TEMPERATURE -> R.string.overlay_layout_metric_temp
    TelemetryMetric.WATTAGE -> R.string.overlay_layout_metric_watt
    TelemetryMetric.BATTERY -> R.string.overlay_layout_metric_battery
}
