package com.pixel.gamebooster.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(
    private val prefs: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    companion object {
        const val PREFS_NAME = "pixel_game_booster_prefs"

        const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        const val KEY_BOOSTER_ENABLED = "booster_enabled"
        const val KEY_INTERVENTION_MODE = "intervention_mode" // 2=Perf, 1=Std, 3=Batt
        const val KEY_FPS_OVERRIDE = "fps_override"
        const val KEY_LOCK_REFRESH_RATE = "lock_refresh_rate"
        const val KEY_BOOST_RAM = "boost_ram"
        const val KEY_CACHED_FREEZER = "cached_freezer"
        const val KEY_DISABLE_AUTO_BRIGHTNESS = "disable_auto_brightness"
        const val KEY_DISABLE_QUICK_TAP = "disable_quick_tap"
        const val KEY_STABILIZE_WIFI = "stabilize_wifi"
        const val KEY_WIFI_LOW_LATENCY = "wifi_low_latency"
        const val KEY_WIFI_SUPPRESS_SCANS = "wifi_suppress_scans"
        const val KEY_WIFI_LOCK_SUFFICIENCY = "wifi_lock_sufficiency"
        const val KEY_WIFI_PREVENT_ARP = "wifi_prevent_arp"
        const val KEY_BLOCK_BATTERY_SAVER = "block_battery_saver"
        const val KEY_PAUSE_AUTO_SYNC = "pause_auto_sync"
        const val KEY_DISABLE_PRIVATE_DNS = "disable_private_dns"
        const val KEY_RESTORE_DELAY = "restore_delay"
        const val KEY_THEME_MODE = "theme_mode" // 0=System, 1=Light, 2=Dark
        const val KEY_PITCH_BLACK = "pitch_black"

        const val KEY_GAMES = "game_packages"
        const val KEY_SUSPENDED_APPS = "suspended_user_packages"
        const val KEY_SUSPENDED_SERVICES = "suspended_system_services"

        // Default system services to suspend
        val DEFAULT_SERVICES = setOf(
            "com.google.android.aicore",
            "com.google.android.apps.turbo",
            "com.google.android.settings.intelligence",
            "com.google.android.apps.scone",
            "com.google.android.apps.carrier.carrierwifi",
            "com.google.android.projection.gearhead",
        )
    }

    // Live StateFlows for UI observation
    val hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false))
    val isBoosterEnabled = MutableStateFlow(prefs.getBoolean(KEY_BOOSTER_ENABLED, false))
    val interventionMode = MutableStateFlow(prefs.getInt(KEY_INTERVENTION_MODE, 2))
    val fpsOverride = MutableStateFlow(prefs.getInt(KEY_FPS_OVERRIDE, 0))
    val isLockRefreshRate = MutableStateFlow(prefs.getBoolean(KEY_LOCK_REFRESH_RATE, true))
    val isBoostRam = MutableStateFlow(prefs.getBoolean(KEY_BOOST_RAM, true))
    val isCachedFreezer = MutableStateFlow(prefs.getBoolean(KEY_CACHED_FREEZER, true))
    val isDisableAutoBrightness = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_AUTO_BRIGHTNESS, true))
    val isDisableQuickTap = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_QUICK_TAP, true))
    val isStabilizeWifi = MutableStateFlow(prefs.getBoolean(KEY_STABILIZE_WIFI, true))
    val isWifiLowLatency = MutableStateFlow(prefs.getBoolean(KEY_WIFI_LOW_LATENCY, true))
    val isWifiSuppressScans = MutableStateFlow(prefs.getBoolean(KEY_WIFI_SUPPRESS_SCANS, true))
    val isWifiLockSufficiency = MutableStateFlow(prefs.getBoolean(KEY_WIFI_LOCK_SUFFICIENCY, true))
    val isWifiPreventArp = MutableStateFlow(prefs.getBoolean(KEY_WIFI_PREVENT_ARP, true))
    val isBlockBatterySaver = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_BATTERY_SAVER, true))
    val isPauseAutoSync = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_AUTO_SYNC, true))
    val isDisablePrivateDns = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_PRIVATE_DNS, true))
    val restoreDelay = MutableStateFlow(prefs.getInt(KEY_RESTORE_DELAY, 5))
    val themeMode = MutableStateFlow(prefs.getInt(KEY_THEME_MODE, 0))
    val isPitchBlack = MutableStateFlow(prefs.getBoolean(KEY_PITCH_BLACK, false))

    val gameApps = MutableStateFlow(prefs.getStringSet(KEY_GAMES, emptySet()) ?: emptySet())
    val suspendedApps = MutableStateFlow(prefs.getStringSet(KEY_SUSPENDED_APPS, emptySet()) ?: emptySet())
    val suspendedServices = MutableStateFlow(prefs.getStringSet(KEY_SUSPENDED_SERVICES, DEFAULT_SERVICES) ?: DEFAULT_SERVICES)

    // Strong reference retention against WeakHashMap garbage collection in SharedPreferencesImpl
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_HAS_COMPLETED_ONBOARDING -> hasCompletedOnboarding.value = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
            KEY_BOOSTER_ENABLED -> isBoosterEnabled.value = prefs.getBoolean(KEY_BOOSTER_ENABLED, false)
            KEY_INTERVENTION_MODE -> interventionMode.value = prefs.getInt(KEY_INTERVENTION_MODE, 2)
            KEY_FPS_OVERRIDE -> fpsOverride.value = prefs.getInt(KEY_FPS_OVERRIDE, 0)
            KEY_LOCK_REFRESH_RATE -> isLockRefreshRate.value = prefs.getBoolean(KEY_LOCK_REFRESH_RATE, true)
            KEY_BOOST_RAM -> isBoostRam.value = prefs.getBoolean(KEY_BOOST_RAM, true)
            KEY_CACHED_FREEZER -> isCachedFreezer.value = prefs.getBoolean(KEY_CACHED_FREEZER, true)
            KEY_DISABLE_AUTO_BRIGHTNESS -> isDisableAutoBrightness.value = prefs.getBoolean(KEY_DISABLE_AUTO_BRIGHTNESS, true)
            KEY_DISABLE_QUICK_TAP -> isDisableQuickTap.value = prefs.getBoolean(KEY_DISABLE_QUICK_TAP, true)
            KEY_STABILIZE_WIFI -> isStabilizeWifi.value = prefs.getBoolean(KEY_STABILIZE_WIFI, true)
            KEY_WIFI_LOW_LATENCY -> isWifiLowLatency.value = prefs.getBoolean(KEY_WIFI_LOW_LATENCY, true)
            KEY_WIFI_SUPPRESS_SCANS -> isWifiSuppressScans.value = prefs.getBoolean(KEY_WIFI_SUPPRESS_SCANS, true)
            KEY_WIFI_LOCK_SUFFICIENCY -> isWifiLockSufficiency.value = prefs.getBoolean(KEY_WIFI_LOCK_SUFFICIENCY, true)
            KEY_WIFI_PREVENT_ARP -> isWifiPreventArp.value = prefs.getBoolean(KEY_WIFI_PREVENT_ARP, true)
            KEY_BLOCK_BATTERY_SAVER -> isBlockBatterySaver.value = prefs.getBoolean(KEY_BLOCK_BATTERY_SAVER, true)
            KEY_PAUSE_AUTO_SYNC -> isPauseAutoSync.value = prefs.getBoolean(KEY_PAUSE_AUTO_SYNC, true)
            KEY_DISABLE_PRIVATE_DNS -> isDisablePrivateDns.value = prefs.getBoolean(KEY_DISABLE_PRIVATE_DNS, true)
            KEY_RESTORE_DELAY -> restoreDelay.value = prefs.getInt(KEY_RESTORE_DELAY, 5)
            KEY_THEME_MODE -> themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
            KEY_PITCH_BLACK -> isPitchBlack.value = prefs.getBoolean(KEY_PITCH_BLACK, false)
            KEY_GAMES -> gameApps.value = prefs.getStringSet(KEY_GAMES, emptySet())?.toSet() ?: emptySet()
            KEY_SUSPENDED_APPS -> suspendedApps.value = prefs.getStringSet(KEY_SUSPENDED_APPS, emptySet())?.toSet() ?: emptySet()
            KEY_SUSPENDED_SERVICES -> suspendedServices.value = prefs.getStringSet(KEY_SUSPENDED_SERVICES, DEFAULT_SERVICES)?.toSet() ?: DEFAULT_SERVICES
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed).apply()
        hasCompletedOnboarding.value = completed
    }

    fun setHasCompletedOnboarding(completed: Boolean) {
        setCompletedOnboarding(completed)
    }

    fun setBoosterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BOOSTER_ENABLED, enabled).apply()
        isBoosterEnabled.value = enabled
    }

    fun setInterventionMode(mode: Int) {
        prefs.edit().putInt(KEY_INTERVENTION_MODE, mode).apply()
        interventionMode.value = mode
    }

    fun setFpsOverride(fps: Int) {
        prefs.edit().putInt(KEY_FPS_OVERRIDE, fps).apply()
        fpsOverride.value = fps
    }

    fun setLockRefreshRate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_REFRESH_RATE, enabled).apply()
        isLockRefreshRate.value = enabled
    }

    fun setBoostRam(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BOOST_RAM, enabled).apply()
        isBoostRam.value = enabled
    }

    fun setCachedFreezer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CACHED_FREEZER, enabled).apply()
        isCachedFreezer.value = enabled
    }

    fun setDisableAutoBrightness(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_AUTO_BRIGHTNESS, enabled).apply()
        isDisableAutoBrightness.value = enabled
    }

    fun setDisableQuickTap(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_QUICK_TAP, enabled).apply()
        isDisableQuickTap.value = enabled
    }

    fun setStabilizeWifi(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STABILIZE_WIFI, enabled).apply()
        isStabilizeWifi.value = enabled
    }

    fun setWifiLowLatency(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_LOW_LATENCY, enabled).apply()
        isWifiLowLatency.value = enabled
    }

    fun setWifiSuppressScans(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_SUPPRESS_SCANS, enabled).apply()
        isWifiSuppressScans.value = enabled
    }

    fun setWifiLockSufficiency(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_LOCK_SUFFICIENCY, enabled).apply()
        isWifiLockSufficiency.value = enabled
    }

    fun setWifiPreventArp(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_PREVENT_ARP, enabled).apply()
        isWifiPreventArp.value = enabled
    }

    fun setBlockBatterySaver(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_BATTERY_SAVER, enabled).apply()
        isBlockBatterySaver.value = enabled
    }

    fun setPauseAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_AUTO_SYNC, enabled).apply()
        isPauseAutoSync.value = enabled
    }

    fun setDisablePrivateDns(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_PRIVATE_DNS, enabled).apply()
        isDisablePrivateDns.value = enabled
    }

    fun setRestoreDelay(seconds: Int) {
        prefs.edit().putInt(KEY_RESTORE_DELAY, seconds).apply()
        restoreDelay.value = seconds
    }

    fun addGame(pkg: String) {
        val updated = (gameApps.value + pkg).toSet()
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        gameApps.value = updated
    }

    fun removeGame(pkg: String) {
        val updated = (gameApps.value - pkg).toSet()
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        gameApps.value = updated
    }

    fun setSuspendedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_SUSPENDED_APPS, apps).apply()
        suspendedApps.value = apps
    }

    fun toggleSuspendedApp(pkg: String) {
        val current = suspendedApps.value
        val updated = if (current.contains(pkg)) current - pkg else current + pkg
        setSuspendedApps(updated)
    }

    fun setSuspendedServices(services: Set<String>) {
        prefs.edit().putStringSet(KEY_SUSPENDED_SERVICES, services).apply()
        suspendedServices.value = services
    }

    fun toggleSuspendedService(service: String) {
        val current = suspendedServices.value
        val updated = if (current.contains(service)) current - service else current + service
        setSuspendedServices(updated)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        themeMode.value = mode
    }

    fun setPitchBlack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PITCH_BLACK, enabled).apply()
        isPitchBlack.value = enabled
    }
}
