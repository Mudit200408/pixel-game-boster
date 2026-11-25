package com.pixel.gamebooster.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pixel.gamebooster.data.db.AppDatabase
import com.pixel.gamebooster.data.db.dao.GameProfileDao
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.model.OverlayGestureAction
import com.pixel.gamebooster.data.model.toDomain
import com.pixel.gamebooster.data.model.toEntity
import com.pixel.gamebooster.overlay.model.OverlayLayoutConfig
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsRepository(
    private val prefs: SharedPreferences,
    val gameProfileDao: GameProfileDao? = null,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        AppDatabase.getInstance(context).gameProfileDao(),
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
        const val KEY_LOCK_BACK_GESTURE = "lock_back_gesture"
        const val KEY_STABILIZE_WIFI = "stabilize_wifi"
        const val KEY_WIFI_LOW_LATENCY = "wifi_low_latency"
        const val KEY_WIFI_SUPPRESS_SCANS = "wifi_suppress_scans"
        const val KEY_WIFI_LOCK_SUFFICIENCY = "wifi_lock_sufficiency"
        const val KEY_WIFI_PREVENT_ARP = "wifi_prevent_arp"
        const val KEY_BLOCK_BATTERY_SAVER = "block_battery_saver"
        const val KEY_PAUSE_AUTO_SYNC = "pause_auto_sync"
        const val KEY_PAUSE_AIRSYNC = "pause_airsync"
        const val KEY_DISABLE_PRIVATE_DNS = "disable_private_dns"
        const val KEY_THEME_MODE = "theme_mode" // 0=System, 1=Light, 2=Dark
        const val KEY_PITCH_BLACK = "pitch_black"
        const val KEY_TACTILE_HAPTICS = "tactile_haptics"
        const val KEY_GAME_MODE_TOASTS = "game_mode_toasts"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        const val KEY_OVERLAY_STYLE = "overlay_style" // 0 = Bar, 1 = Circle Button
        const val KEY_OVERLAY_OPACITY = "overlay_opacity" // 0.20f..1.0f
        const val KEY_OVERLAY_SHOW_LIVE_FPS = "overlay_show_live_fps"
        const val KEY_OVERLAY_COLOR_MODE = "overlay_color_mode" // 0 = Monet/Wallpaper, 1 = Custom
        const val KEY_OVERLAY_CUSTOM_COLOR = "overlay_custom_color"
        const val KEY_OVERLAY_LAYOUT = "overlay_layout_json"
        const val KEY_RECORD_AUDIO_SOURCE = "record_audio_source" // 0=Mute, 1=Mic, 2=Device, 3=Both
        const val KEY_RECORD_RESOLUTION = "record_resolution" // 0=1080p 60fps, 1=1080p 30fps, 2=720p 60fps
        const val KEY_RECORD_SHOW_TOUCHES = "record_show_touches"
        const val KEY_RECORD_DOUBLE_TAP_STOP = "record_double_tap_stop"
        const val KEY_OVERLAY_DOUBLE_TAP_ACTION = "overlay_double_tap_action"
        const val KEY_OVERLAY_LONG_TAP_ACTION = "overlay_long_tap_action"
        const val KEY_OVERLAY_DOUBLE_TAP_DELAY_MS = "overlay_double_tap_delay_ms"
        const val DEFAULT_DOUBLE_TAP_DELAY_MS = 200

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
            "com.google.android.as",
        )

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Live StateFlows for UI observation
    val hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false))
    val isBoosterEnabled = MutableStateFlow(prefs.getBoolean(KEY_BOOSTER_ENABLED, true))
    val interventionMode = MutableStateFlow(prefs.getInt(KEY_INTERVENTION_MODE, 2))
    val fpsOverride = MutableStateFlow(prefs.getInt(KEY_FPS_OVERRIDE, 0))
    val isLockRefreshRate = MutableStateFlow(prefs.getBoolean(KEY_LOCK_REFRESH_RATE, true))
    val isBoostRam = MutableStateFlow(prefs.getBoolean(KEY_BOOST_RAM, true))
    val isCachedFreezer = MutableStateFlow(prefs.getBoolean(KEY_CACHED_FREEZER, true))
    val isDisableAutoBrightness = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_AUTO_BRIGHTNESS, true))
    val isDisableQuickTap = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_QUICK_TAP, true))
    val isLockBackGesture = MutableStateFlow(prefs.getBoolean(KEY_LOCK_BACK_GESTURE, false))
    val isStabilizeWifi = MutableStateFlow(prefs.getBoolean(KEY_STABILIZE_WIFI, true))
    val isWifiLowLatency = MutableStateFlow(prefs.getBoolean(KEY_WIFI_LOW_LATENCY, true))
    val isWifiSuppressScans = MutableStateFlow(prefs.getBoolean(KEY_WIFI_SUPPRESS_SCANS, true))
    val isWifiLockSufficiency = MutableStateFlow(prefs.getBoolean(KEY_WIFI_LOCK_SUFFICIENCY, true))
    val isWifiPreventArp = MutableStateFlow(prefs.getBoolean(KEY_WIFI_PREVENT_ARP, true))
    val isBlockBatterySaver = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_BATTERY_SAVER, true))
    val isPauseAutoSync = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_AUTO_SYNC, true))
    val isPauseAirSync = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_AIRSYNC, true))
    val isDisablePrivateDns = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_PRIVATE_DNS, true))
    val themeMode = MutableStateFlow(prefs.getInt(KEY_THEME_MODE, 0))
    val isPitchBlack = MutableStateFlow(prefs.getBoolean(KEY_PITCH_BLACK, false))
    val isTactileHaptics = MutableStateFlow(prefs.getBoolean(KEY_TACTILE_HAPTICS, true))
    val isGameModeToastEnabled = MutableStateFlow(prefs.getBoolean(KEY_GAME_MODE_TOASTS, true))
    val isOverlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY_ENABLED, true))
    val overlayStyle = MutableStateFlow(prefs.getInt(KEY_OVERLAY_STYLE, 0))
    val overlayOpacity = MutableStateFlow(prefs.getFloat(KEY_OVERLAY_OPACITY, 0.94f).coerceIn(0.20f, 1.0f))
    val showLiveFps = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY_SHOW_LIVE_FPS, false))
    val overlayColorMode = MutableStateFlow(prefs.getInt(KEY_OVERLAY_COLOR_MODE, 0))
    val overlayCustomColor = MutableStateFlow(prefs.getInt(KEY_OVERLAY_CUSTOM_COLOR, 0xFF4285F4.toInt()))
    val overlayLayout = MutableStateFlow(OverlayLayoutConfig.fromJson(prefs.getString(KEY_OVERLAY_LAYOUT, null)))
    val recordAudioSource = MutableStateFlow(prefs.getInt(KEY_RECORD_AUDIO_SOURCE, 2))
    val recordResolution = MutableStateFlow(prefs.getInt(KEY_RECORD_RESOLUTION, 0))
    val recordShowTouches = MutableStateFlow(prefs.getBoolean(KEY_RECORD_SHOW_TOUCHES, false))
    val recordDoubleTapStop = MutableStateFlow(prefs.getBoolean(KEY_RECORD_DOUBLE_TAP_STOP, true))
    val overlayDoubleTapAction = MutableStateFlow(
        OverlayGestureAction.fromPrefValue(prefs.getString(KEY_OVERLAY_DOUBLE_TAP_ACTION, null)),
    )
    val overlayLongTapAction = MutableStateFlow(
        OverlayGestureAction.fromPrefValue(prefs.getString(KEY_OVERLAY_LONG_TAP_ACTION, null)),
    )
    val overlayDoubleTapDelayMs = MutableStateFlow(
        prefs.getInt(KEY_OVERLAY_DOUBLE_TAP_DELAY_MS, DEFAULT_DOUBLE_TAP_DELAY_MS).coerceIn(100, 500),
    )

    val gameApps = MutableStateFlow(prefs.getStringSet(KEY_GAMES, emptySet())?.toSet() ?: emptySet())
    val suspendedApps = MutableStateFlow(prefs.getStringSet(KEY_SUSPENDED_APPS, emptySet())?.toSet() ?: emptySet())
    val suspendedServices = MutableStateFlow(prefs.getStringSet(KEY_SUSPENDED_SERVICES, DEFAULT_SERVICES)?.toSet() ?: DEFAULT_SERVICES)

    // Strong reference retention against WeakHashMap garbage collection in SharedPreferencesImpl
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_HAS_COMPLETED_ONBOARDING -> hasCompletedOnboarding.value = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
            KEY_BOOSTER_ENABLED -> isBoosterEnabled.value = prefs.getBoolean(KEY_BOOSTER_ENABLED, true)
            KEY_INTERVENTION_MODE -> interventionMode.value = prefs.getInt(KEY_INTERVENTION_MODE, 2)
            KEY_FPS_OVERRIDE -> fpsOverride.value = prefs.getInt(KEY_FPS_OVERRIDE, 0)
            KEY_LOCK_REFRESH_RATE -> isLockRefreshRate.value = prefs.getBoolean(KEY_LOCK_REFRESH_RATE, true)
            KEY_BOOST_RAM -> isBoostRam.value = prefs.getBoolean(KEY_BOOST_RAM, true)
            KEY_CACHED_FREEZER -> isCachedFreezer.value = prefs.getBoolean(KEY_CACHED_FREEZER, true)
            KEY_DISABLE_AUTO_BRIGHTNESS -> isDisableAutoBrightness.value = prefs.getBoolean(KEY_DISABLE_AUTO_BRIGHTNESS, true)
            KEY_DISABLE_QUICK_TAP -> isDisableQuickTap.value = prefs.getBoolean(KEY_DISABLE_QUICK_TAP, true)
            KEY_LOCK_BACK_GESTURE -> isLockBackGesture.value = prefs.getBoolean(KEY_LOCK_BACK_GESTURE, false)
            KEY_STABILIZE_WIFI -> isStabilizeWifi.value = prefs.getBoolean(KEY_STABILIZE_WIFI, true)
            KEY_WIFI_LOW_LATENCY -> isWifiLowLatency.value = prefs.getBoolean(KEY_WIFI_LOW_LATENCY, true)
            KEY_WIFI_SUPPRESS_SCANS -> isWifiSuppressScans.value = prefs.getBoolean(KEY_WIFI_SUPPRESS_SCANS, true)
            KEY_WIFI_LOCK_SUFFICIENCY -> isWifiLockSufficiency.value = prefs.getBoolean(KEY_WIFI_LOCK_SUFFICIENCY, true)
            KEY_WIFI_PREVENT_ARP -> isWifiPreventArp.value = prefs.getBoolean(KEY_WIFI_PREVENT_ARP, true)
            KEY_BLOCK_BATTERY_SAVER -> isBlockBatterySaver.value = prefs.getBoolean(KEY_BLOCK_BATTERY_SAVER, true)
            KEY_PAUSE_AUTO_SYNC -> isPauseAutoSync.value = prefs.getBoolean(KEY_PAUSE_AUTO_SYNC, true)
            KEY_PAUSE_AIRSYNC -> isPauseAirSync.value = prefs.getBoolean(KEY_PAUSE_AIRSYNC, true)
            KEY_DISABLE_PRIVATE_DNS -> isDisablePrivateDns.value = prefs.getBoolean(KEY_DISABLE_PRIVATE_DNS, true)
            KEY_THEME_MODE -> themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
            KEY_PITCH_BLACK -> isPitchBlack.value = prefs.getBoolean(KEY_PITCH_BLACK, false)
            KEY_TACTILE_HAPTICS -> {
                val enabled = prefs.getBoolean(KEY_TACTILE_HAPTICS, true)
                isTactileHaptics.value = enabled
                HapticUtil.isAppHapticsEnabled.value = enabled
            }
            KEY_GAME_MODE_TOASTS -> isGameModeToastEnabled.value = prefs.getBoolean(KEY_GAME_MODE_TOASTS, true)
            KEY_OVERLAY_ENABLED -> isOverlayEnabled.value = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
            KEY_OVERLAY_STYLE -> overlayStyle.value = prefs.getInt(KEY_OVERLAY_STYLE, 0)
            KEY_OVERLAY_OPACITY -> overlayOpacity.value = prefs.getFloat(KEY_OVERLAY_OPACITY, 0.94f).coerceIn(0.20f, 1.0f)
            KEY_OVERLAY_SHOW_LIVE_FPS -> showLiveFps.value = prefs.getBoolean(KEY_OVERLAY_SHOW_LIVE_FPS, false)
            KEY_OVERLAY_COLOR_MODE -> overlayColorMode.value = prefs.getInt(KEY_OVERLAY_COLOR_MODE, 0)
            KEY_OVERLAY_CUSTOM_COLOR -> overlayCustomColor.value = prefs.getInt(KEY_OVERLAY_CUSTOM_COLOR, 0xFF4285F4.toInt())
            KEY_OVERLAY_LAYOUT -> overlayLayout.value = OverlayLayoutConfig.fromJson(prefs.getString(KEY_OVERLAY_LAYOUT, null))
            KEY_RECORD_AUDIO_SOURCE -> recordAudioSource.value = prefs.getInt(KEY_RECORD_AUDIO_SOURCE, 2)
            KEY_RECORD_RESOLUTION -> recordResolution.value = prefs.getInt(KEY_RECORD_RESOLUTION, 0)
            KEY_RECORD_SHOW_TOUCHES -> recordShowTouches.value = prefs.getBoolean(KEY_RECORD_SHOW_TOUCHES, false)
            KEY_RECORD_DOUBLE_TAP_STOP -> recordDoubleTapStop.value = prefs.getBoolean(KEY_RECORD_DOUBLE_TAP_STOP, true)
            KEY_OVERLAY_DOUBLE_TAP_ACTION -> overlayDoubleTapAction.value =
                OverlayGestureAction.fromPrefValue(prefs.getString(KEY_OVERLAY_DOUBLE_TAP_ACTION, null))
            KEY_OVERLAY_LONG_TAP_ACTION -> overlayLongTapAction.value =
                OverlayGestureAction.fromPrefValue(prefs.getString(KEY_OVERLAY_LONG_TAP_ACTION, null))
            KEY_OVERLAY_DOUBLE_TAP_DELAY_MS -> overlayDoubleTapDelayMs.value =
                prefs.getInt(KEY_OVERLAY_DOUBLE_TAP_DELAY_MS, DEFAULT_DOUBLE_TAP_DELAY_MS).coerceIn(100, 500)
            KEY_GAMES -> gameApps.value = prefs.getStringSet(KEY_GAMES, emptySet())?.toSet() ?: emptySet()
            KEY_SUSPENDED_APPS -> suspendedApps.value = prefs.getStringSet(KEY_SUSPENDED_APPS, emptySet())?.toSet() ?: emptySet()
            KEY_SUSPENDED_SERVICES -> suspendedServices.value = prefs.getStringSet(KEY_SUSPENDED_SERVICES, DEFAULT_SERVICES)?.toSet() ?: DEFAULT_SERVICES
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        HapticUtil.isAppHapticsEnabled.value = prefs.getBoolean(KEY_TACTILE_HAPTICS, true)
        if (gameProfileDao != null) {
            repositoryScope.launch {
                seedConfiguredGames()
            }
        }
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

    fun setLockBackGesture(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_BACK_GESTURE, enabled).apply()
        isLockBackGesture.value = enabled
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

    fun setPauseAirSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_AIRSYNC, enabled).apply()
        isPauseAirSync.value = enabled
    }

    fun setDisablePrivateDns(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DISABLE_PRIVATE_DNS, enabled).apply()
        isDisablePrivateDns.value = enabled
    }

    fun addGame(pkg: String) {
        val updated = (gameApps.value + pkg).toSet()
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        gameApps.value = updated
        repositoryScope.launch {
            if (gameProfileDao?.getProfile(pkg) == null) {
                gameProfileDao?.upsertProfile(createDefaultEffectiveProfile(pkg).toEntity())
            }
        }
    }

    fun addGames(pkgs: Collection<String>) {
        if (pkgs.isEmpty()) return
        val updated = (gameApps.value + pkgs).toSet()
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        gameApps.value = updated
        repositoryScope.launch {
            for (pkg in pkgs) {
                if (gameProfileDao?.getProfile(pkg) == null) {
                    gameProfileDao?.upsertProfile(createDefaultEffectiveProfile(pkg).toEntity())
                }
            }
        }
    }

    fun removeGame(pkg: String) {
        val updated = (gameApps.value - pkg).toSet()
        prefs.edit().putStringSet(KEY_GAMES, updated).apply()
        gameApps.value = updated
        repositoryScope.launch {
            gameProfileDao?.deleteByPackageName(pkg)
        }
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

    fun setTactileHaptics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TACTILE_HAPTICS, enabled).apply()
        isTactileHaptics.value = enabled
        HapticUtil.isAppHapticsEnabled.value = enabled
    }

    fun setGameModeToastEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_MODE_TOASTS, enabled).apply()
        isGameModeToastEnabled.value = enabled
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
        isOverlayEnabled.value = enabled
    }

    fun setOverlayStyle(style: Int) {
        prefs.edit().putInt(KEY_OVERLAY_STYLE, style).apply()
        overlayStyle.value = style
    }

    fun setOverlayOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.20f, 1.0f)
        prefs.edit().putFloat(KEY_OVERLAY_OPACITY, clamped).apply()
        overlayOpacity.value = clamped
    }

    fun setShowLiveFps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_SHOW_LIVE_FPS, enabled).apply()
        showLiveFps.value = enabled
    }

    fun setOverlayColorMode(mode: Int) {
        prefs.edit().putInt(KEY_OVERLAY_COLOR_MODE, mode).apply()
        overlayColorMode.value = mode
    }

    fun setOverlayCustomColor(colorArgb: Int) {
        prefs.edit().putInt(KEY_OVERLAY_CUSTOM_COLOR, colorArgb).apply()
        overlayCustomColor.value = colorArgb
    }

    fun setOverlayLayout(config: OverlayLayoutConfig) {
        val sanitized = config.sanitized()
        prefs.edit().putString(KEY_OVERLAY_LAYOUT, sanitized.toJson()).apply()
        overlayLayout.value = sanitized
    }

    fun resetOverlayLayout() {
        setOverlayLayout(OverlayLayoutConfig.Default)
    }

    fun setRecordAudioSource(source: Int) {
        prefs.edit().putInt(KEY_RECORD_AUDIO_SOURCE, source).apply()
        recordAudioSource.value = source
    }

    fun setRecordResolution(resolution: Int) {
        prefs.edit().putInt(KEY_RECORD_RESOLUTION, resolution).apply()
        recordResolution.value = resolution
    }

    fun setRecordShowTouches(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECORD_SHOW_TOUCHES, enabled).apply()
        recordShowTouches.value = enabled
    }

    fun setRecordDoubleTapStop(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECORD_DOUBLE_TAP_STOP, enabled).apply()
        recordDoubleTapStop.value = enabled
    }

    fun setOverlayDoubleTapAction(action: OverlayGestureAction) {
        prefs.edit().putString(KEY_OVERLAY_DOUBLE_TAP_ACTION, action.prefValue).apply()
        overlayDoubleTapAction.value = action
    }

    fun setOverlayLongTapAction(action: OverlayGestureAction) {
        prefs.edit().putString(KEY_OVERLAY_LONG_TAP_ACTION, action.prefValue).apply()
        overlayLongTapAction.value = action
    }

    fun setOverlayDoubleTapDelayMs(delayMs: Int) {
        val clamped = delayMs.coerceIn(100, 500)
        prefs.edit().putInt(KEY_OVERLAY_DOUBLE_TAP_DELAY_MS, clamped).apply()
        overlayDoubleTapDelayMs.value = clamped
    }

    // --- Per-Game Fallback Engine & Persistence ---

    fun createDefaultEffectiveProfile(packageName: String): GameProfile {
        return GameProfile(
            packageName = packageName,
            customized = false,
            fpsOverride = fpsOverride.value,
            lockRefreshRate = isLockRefreshRate.value,
            boostRam = isBoostRam.value,
            cachedFreezer = isCachedFreezer.value,
            disableAutoBrightness = isDisableAutoBrightness.value,
            disableQuickTap = isDisableQuickTap.value,
            lockBackGesture = isLockBackGesture.value,
            interventionMode = interventionMode.value,
            stabilizeWifi = isStabilizeWifi.value,
            wifiLowLatency = isWifiLowLatency.value,
            wifiSuppressScans = isWifiSuppressScans.value,
            wifiLockSufficiency = isWifiLockSufficiency.value,
            wifiPreventArp = isWifiPreventArp.value,
            blockBatterySaver = isBlockBatterySaver.value,
            pauseAutoSync = isPauseAutoSync.value,
            pauseAirSync = isPauseAirSync.value,
            disablePrivateDns = isDisablePrivateDns.value,
            interruptionMode = 1,
            callHandlingMode = 0,
            suspendedApps = suspendedApps.value,
            lastPlayedTimestamp = 0L,
        )
    }

    suspend fun getEffectiveProfile(packageName: String): GameProfile {
        val entity = gameProfileDao?.getProfile(packageName)
        return if (entity != null && entity.customized) {
            entity.toDomain()
        } else {
            createDefaultEffectiveProfile(packageName).copy(
                lastPlayedTimestamp = entity?.lastPlayedTimestamp ?: 0L,
            )
        }
    }

    suspend fun getGameProfile(packageName: String): GameProfile? {
        return gameProfileDao?.getProfile(packageName)?.toDomain()
    }

    fun observeEffectiveProfile(packageName: String): Flow<GameProfile> {
        val globalTriggers = combine(
            listOf(
                isLockRefreshRate,
                isBoostRam,
                isCachedFreezer,
                isDisableAutoBrightness,
                isDisableQuickTap,
                isLockBackGesture,
                isStabilizeWifi,
                isWifiLowLatency,
                isWifiSuppressScans,
                isWifiLockSufficiency,
                isWifiPreventArp,
                isBlockBatterySaver,
                isPauseAutoSync,
                isPauseAirSync,
                isDisablePrivateDns,
                fpsOverride,
                interventionMode,
                suspendedApps,
            )
        ) { Unit }

        val profileFlow = gameProfileDao?.getProfileFlow(packageName) ?: flowOf(null)

        return combine(profileFlow, globalTriggers) { entity, _ ->
            if (entity != null && entity.customized) {
                entity.toDomain()
            } else {
                createDefaultEffectiveProfile(packageName).copy(
                    lastPlayedTimestamp = entity?.lastPlayedTimestamp ?: 0L,
                )
            }
        }
    }

    fun getAllProfilesFlow(): Flow<List<GameProfile>> {
        return gameProfileDao?.getAllProfilesFlow()?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    suspend fun saveGameProfile(profile: GameProfile) {
        gameProfileDao?.upsertProfile(profile.toEntity())
    }

    suspend fun saveProfile(profile: GameProfile) = saveGameProfile(profile)

    suspend fun setProfileCustomized(packageName: String, customized: Boolean) {
        val existing = gameProfileDao?.getProfile(packageName)
        val entityToSave = if (existing != null) {
            existing.copy(customized = customized)
        } else {
            createDefaultEffectiveProfile(packageName).toEntity().copy(customized = customized)
        }
        gameProfileDao?.upsertProfile(entityToSave)
    }

    suspend fun deleteGameProfile(packageName: String) {
        gameProfileDao?.deleteByPackageName(packageName)
    }

    suspend fun removeProfile(packageName: String) = deleteGameProfile(packageName)

    suspend fun updateLastPlayed(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        val existing = gameProfileDao?.getProfile(packageName)
        if (existing != null) {
            gameProfileDao.upsertProfile(existing.copy(lastPlayedTimestamp = timestamp))
        } else {
            val defaultEntity = createDefaultEffectiveProfile(packageName).toEntity().copy(
                lastPlayedTimestamp = timestamp,
            )
            gameProfileDao?.upsertProfile(defaultEntity)
        }
    }

    suspend fun seedConfiguredGames() {
        val dao = gameProfileDao ?: return
        val configured = gameApps.value
        for (pkg in configured) {
            if (dao.getProfile(pkg) == null) {
                dao.upsertProfile(createDefaultEffectiveProfile(pkg).toEntity())
            }
        }
    }
}
