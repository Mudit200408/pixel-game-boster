package com.pixel.gamebooster.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey
    val packageName: String,
    val customized: Boolean = false,
    val fpsOverride: Int = 0,
    val lockRefreshRate: Boolean = true,
    val boostRam: Boolean = true,
    val cachedFreezer: Boolean = true,
    val disableAutoBrightness: Boolean = true,
    val disableQuickTap: Boolean = true,
    val lockBackGesture: Boolean = false,
    val interventionMode: Int = 2,
    val stabilizeWifi: Boolean = true,
    val wifiLowLatency: Boolean = true,
    val wifiSuppressScans: Boolean = true,
    val wifiLockSufficiency: Boolean = true,
    val wifiPreventArp: Boolean = true,
    val blockBatterySaver: Boolean = true,
    val pauseAutoSync: Boolean = true,
    val pauseAirSync: Boolean = true,
    val disablePrivateDns: Boolean = true,
    val interruptionMode: Int = 1,
    val callHandlingMode: Int = 0,
    val suspendedApps: Set<String> = emptySet(),
    val lastPlayedTimestamp: Long = 0L,
)
