package com.pixel.gamebooster.overlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.WindowManager
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import com.pixel.gamebooster.overlay.model.OverlayTileType
import com.pixel.gamebooster.overlay.model.OverlayUiState
import com.pixel.gamebooster.overlay.model.TelemetryMetric
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.RefreshRateUtils
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(FlowPreview::class)
class OverlayViewModel(
    private val context: Context,
    private val packageName: String,
    private val repository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val initialGameName: String = resolveGameName(context, packageName)
    private val initialBatteryLevel: Int = resolveBatteryLevel(context)
    private val initialCharging: Boolean = resolveChargingStatus(context)
    private val initialBrightness: Float = resolveBrightness(context)
    private val initialVolume: Float = resolveVolume(context)
    private val initialBlurSupport: Boolean = resolveBlurSupport(context)
    private val initialTemperature: Int = resolveBatteryTemperature(context)
    private val initialWattage: Float = resolveBatteryWattage(context)
    private val initialFps: Int = resolveCurrentFps(context)

    private val _uiState = MutableStateFlow(
        OverlayUiState(
            packageName = packageName,
            gameName = initialGameName,
            displayState = OverlayDisplayState.COLLAPSED_PILL,
            brightnessPercent = initialBrightness,
            mediaVolumePercent = initialVolume,
            batteryPercent = initialBatteryLevel,
            isCharging = initialCharging,
            isCrossWindowBlurActive = initialBlurSupport,
            currentFps = initialFps,
            temperatureC = initialTemperature,
            wattageW = initialWattage,
        )
    )
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    private val profilePersistDebounce = MutableStateFlow<GameProfile?>(null)

    init {
        scope.launch {
            val initialProfile = runCatching {
                repository.getEffectiveProfile(packageName)
            }.getOrDefault(repository.createDefaultEffectiveProfile(packageName))

            _uiState.update { current ->
                current.copy(
                    interventionMode = initialProfile.interventionMode,
                    fpsOverride = initialProfile.fpsOverride,
                    lockRefreshRate = initialProfile.lockRefreshRate,
                    dndEnabled = initialProfile.interruptionMode != 0,
                    quickTapLocked = initialProfile.disableQuickTap,
                    autoBrightnessLocked = initialProfile.disableAutoBrightness,
                    backGestureLocked = initialProfile.lockBackGesture || GameModeManager.isBackGestureSystemLocked(context),
                    cachedFreezerEnabled = initialProfile.cachedFreezer,
                    wifiLowLatencyEnabled = initialProfile.stabilizeWifi && initialProfile.wifiLowLatency,
                    wifiSuppressScansEnabled = initialProfile.stabilizeWifi && initialProfile.wifiSuppressScans,
                    wifiLockSufficiencyEnabled = initialProfile.stabilizeWifi && initialProfile.wifiLockSufficiency,
                    wifiPreventArpEnabled = initialProfile.stabilizeWifi && initialProfile.wifiPreventArp,
                    privateDnsBypassed = initialProfile.disablePrivateDns,
                    autoSyncPaused = initialProfile.pauseAutoSync,
                    batterySaverBlocked = initialProfile.blockBatterySaver,
                    appsSuspended = initialProfile.suspendedApps.isNotEmpty() || repository.suspendedServices.value.isNotEmpty(),
                    overlayStyle = repository.overlayStyle.value,
                    overlayOpacity = repository.overlayOpacity.value,
                    showLiveFps = repository.showLiveFps.value,
                    overlayColorMode = repository.overlayColorMode.value,
                    overlayCustomColor = repository.overlayCustomColor.value,
                    layoutConfig = repository.overlayLayout.value,
                    recordDoubleTapStop = repository.recordDoubleTapStop.value,
                    doubleTapAction = repository.overlayDoubleTapAction.value,
                    longTapAction = repository.overlayLongTapAction.value,
                    doubleTapDelayMs = repository.overlayDoubleTapDelayMs.value,
                )
            }
            updateMonitoringState()
            refreshLiveBrightnessAsync()
        }

        repository.overlayStyle
            .onEach { style ->
                _uiState.update { it.copy(overlayStyle = style) }
            }
            .launchIn(scope)

        repository.overlayOpacity
            .onEach { opacity ->
                _uiState.update { it.copy(overlayOpacity = opacity) }
            }
            .launchIn(scope)

        repository.showLiveFps
            .onEach { enabled ->
                _uiState.update { it.copy(showLiveFps = enabled) }
                updateMonitoringState()
            }
            .launchIn(scope)

        repository.overlayColorMode
            .onEach { mode ->
                _uiState.update { it.copy(overlayColorMode = mode) }
            }
            .launchIn(scope)

        repository.overlayCustomColor
            .onEach { color ->
                _uiState.update { it.copy(overlayCustomColor = color) }
            }
            .launchIn(scope)

        repository.overlayLayout
            .onEach { layout ->
                _uiState.update { it.copy(layoutConfig = layout) }
                updateMonitoringState()
            }
            .launchIn(scope)

        repository.recordDoubleTapStop
            .onEach { enabled ->
                _uiState.update { it.copy(recordDoubleTapStop = enabled) }
            }
            .launchIn(scope)

        repository.overlayDoubleTapAction
            .onEach { action ->
                _uiState.update { it.copy(doubleTapAction = action) }
            }
            .launchIn(scope)

        repository.overlayLongTapAction
            .onEach { action ->
                _uiState.update { it.copy(longTapAction = action) }
            }
            .launchIn(scope)

        repository.overlayDoubleTapDelayMs
            .onEach { delayMs ->
                _uiState.update { it.copy(doubleTapDelayMs = delayMs) }
            }
            .launchIn(scope)

        com.pixel.gamebooster.recording.ScreenRecordService.isRecording
            .onEach { recording ->
                _uiState.update { it.copy(isRecording = recording) }
            }
            .launchIn(scope)

        profilePersistDebounce
            .filterNotNull()
            .debounce(300L)
            .onEach { profile ->
                runCatching {
                    repository.saveProfile(profile.copy(customized = true))
                }
            }
            .launchIn(scope)

        updateMonitoringState()
    }

    private var telemetryJob: Job? = null
    private var fpsMonitorJob: Job? = null
    private var cachedSurfaceFlingerLayer: String? = null

    private fun updateMonitoringState() {
        val state = _uiState.value
        val isExpanded = state.displayState == OverlayDisplayState.EXPANDED_PANEL
        val needFps = (isExpanded && state.layoutConfig.isMetricVisible(TelemetryMetric.FPS)) || state.showLiveFps
        val needTelemetry = isExpanded && state.layoutConfig.isAnyBatteryTelemetryVisible

        if (needFps) {
            if (fpsMonitorJob?.isActive != true) {
                startFpsMonitoring()
            }
        } else {
            fpsMonitorJob?.cancel()
            fpsMonitorJob = null
            TaskFpsMonitor.unregister()
        }

        if (needTelemetry) {
            if (telemetryJob?.isActive != true) {
                startTelemetryMonitoring()
            }
        } else {
            telemetryJob?.cancel()
            telemetryJob = null
        }
    }

    private fun startFpsMonitoring() {
        fpsMonitorJob?.cancel()
        fpsMonitorJob = scope.launch(Dispatchers.IO) {
            // 1. Prefer native IWindowManager.registerTaskFpsCallback (same as Google Game Dashboard)
            val registered = TaskFpsMonitor.register(context, packageName) { liveFps ->
                _uiState.update { it.copy(currentFps = liveFps) }
            }
            if (registered) return@launch

            // 2. Fallback: SurfaceFlinger --latency polling only if TaskFpsCallback is unavailable
            while (isActive) {
                val displayFps = resolveCurrentFps(context)
                var measuredFps = displayFps
                if (ShellUtils.isShizukuAvailable() && ShellUtils.hasShizukuPermission() && packageName.isNotBlank()) {
                    runCatching {
                        val layer = cachedSurfaceFlingerLayer ?: run {
                            val listCmd = "dumpsys SurfaceFlinger --list | grep -F \"$packageName\""
                            val listOut = ShellUtils.runCommandWithOutput(context, listCmd, notifyOnError = false)
                            val candidates = listOut?.lineSequence()
                                ?.map { it.trim() }
                                ?.filter { it.isNotEmpty() && !it.startsWith("Background") }
                                ?.toList()
                                .orEmpty()
                            val picked = candidates.firstOrNull { it.contains("SurfaceView[") || it.contains("BLAST") }
                                ?: candidates.firstOrNull()
                            cachedSurfaceFlingerLayer = picked
                            picked
                        }
                        val output = if (!layer.isNullOrBlank()) {
                            ShellUtils.runCommandWithOutput(context, "dumpsys SurfaceFlinger --latency \"$layer\"", notifyOnError = false)
                        } else {
                            ShellUtils.runCommandWithOutput(context, "dumpsys SurfaceFlinger --latency $packageName", notifyOnError = false)
                        }
                        if (output.isNullOrBlank()) {
                            cachedSurfaceFlingerLayer = null
                        } else {
                            val lines = output.trim().split("\n")
                            if (lines.size > 2) {
                                val now = System.nanoTime()
                                val oneSecAgo = now - 1_000_000_000L
                                val timestamps = lines.drop(1).mapNotNull { line ->
                                    val parts = line.trim().split(WHITESPACE_REGEX)
                                    if (parts.size >= 2) {
                                        parts[1].toLongOrNull()?.takeIf { ts -> ts > 0L && ts != Long.MAX_VALUE }
                                    } else null
                                }.filter { it in oneSecAgo..now }

                                if (timestamps.isNotEmpty()) {
                                    measuredFps = timestamps.size.coerceIn(1, displayFps)
                                }
                            }
                        }
                    }
                }
                _uiState.update { it.copy(currentFps = measuredFps) }
                delay(1000L)
            }
        }
    }

    private fun startTelemetryMonitoring() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, ifilter)
                val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 380) ?: 380
                val temperature = tempTenths / 10

                val currentMicroAmps = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000
                val wattage = if (currentMicroAmps != 0) {
                    val amps = kotlin.math.abs(currentMicroAmps).toFloat() / 1_000_000f
                    val volts = voltageMv.toFloat() / 1000f
                    (amps * volts * 10).roundToInt() / 10f
                } else {
                    4.2f
                }

                _uiState.update { current ->
                    current.copy(
                        batteryPercent = batteryLevel,
                        temperatureC = temperature,
                        wattageW = wattage,
                    )
                }

                delay(1500L)
            }
        }
    }

    fun setDisplayState(state: OverlayDisplayState) {
        if (state == OverlayDisplayState.EXPANDED_PANEL) {
            _uiState.update {
                it.copy(
                    displayState = state,
                    brightnessPercent = if (ShellUtils.hasShizukuPermission()) {
                        it.brightnessPercent
                    } else {
                        resolveBrightness(context)
                    },
                    mediaVolumePercent = resolveVolume(context),
                )
            }
            refreshLiveBrightnessAsync()
        } else {
            _uiState.update { it.copy(displayState = state) }
        }
        updateMonitoringState()
    }

    private fun refreshLiveBrightnessAsync() {
        scope.launch(Dispatchers.IO) {
            val hasShizuku = ShellUtils.isShizukuAvailable() && ShellUtils.hasShizukuPermission()
            if (hasShizuku) {
                val info = getBrightnessInfoViaShizuku()
                if (info != null) {
                    val minB = runCatching { info.javaClass.getField("brightnessMinimum").getFloat(info) }.getOrDefault(0f)
                    val maxB = runCatching { info.javaClass.getField("brightnessMaximum").getFloat(info) }.getOrDefault(1f)
                    val liveB = runCatching { info.javaClass.getField("brightness").getFloat(info) }.getOrNull()
                    if (liveB != null && !liveB.isNaN() && liveB >= 0f) {
                        if (maxB > minB && !minB.isNaN() && !maxB.isNaN()) {
                            cachedBrightnessRange = minB to maxB
                        }
                        val normalized = if (maxB > minB && !minB.isNaN() && !maxB.isNaN()) {
                            ((liveB - minB) / (maxB - minB)).coerceIn(0f, 1f)
                        } else {
                            liveB.coerceIn(0f, 1f)
                        }
                        val gamma = linearToGammaBrightness(normalized)
                        Log.d(TAG, "Live brightness via Shizuku BrightnessInfo: live=$liveB range=[$minB,$maxB] -> gamma=$gamma")
                        _uiState.update { it.copy(brightnessPercent = gamma) }
                        return@launch
                    }
                }

                val binderLinear = getBrightnessViaShizuku()
                if (binderLinear != null && !binderLinear.isNaN() && binderLinear >= 0f) {
                    val (minB, maxB) = cachedBrightnessRange ?: resolveBrightnessRange(context)
                    val normalized = if (maxB > minB) {
                        ((binderLinear - minB) / (maxB - minB)).coerceIn(0f, 1f)
                    } else {
                        binderLinear.coerceIn(0f, 1f)
                    }
                    val gamma = linearToGammaBrightness(normalized)
                    Log.d(TAG, "Live brightness via Shizuku getBrightness: live=$binderLinear -> gamma=$gamma")
                    _uiState.update { it.copy(brightnessPercent = gamma) }
                    return@launch
                }
            }

            val resolved = resolveBrightness(context)
            _uiState.update { it.copy(brightnessPercent = resolved) }
        }
    }

    fun setDockedOnRight(isRight: Boolean) {
        _uiState.update { it.copy(isDockedOnRight = isRight) }
    }

    fun setImeVisible(visible: Boolean) {
        _uiState.update { it.copy(isImeVisible = visible) }
    }

    fun setInterventionMode(mode: Int) {
        _uiState.update { it.copy(interventionMode = mode) }
        val fps = _uiState.value.fpsOverride
        scope.launch(Dispatchers.IO) {
            GameModeManager.applyGameIntervention(context, packageName, mode, fps)
            updatePersistedProfile { it.copy(interventionMode = mode) }
        }
    }

    fun cycleInterventionMode() {
        val nextMode = when (_uiState.value.interventionMode) {
            3 -> 1 // Battery -> Balanced
            1 -> 2 // Balanced -> Boost
            else -> 3 // Boost -> Battery
        }
        setInterventionMode(nextMode)
        val labelRes = when (nextMode) {
            3 -> com.pixel.gamebooster.R.string.overlay_mode_battery
            1 -> com.pixel.gamebooster.R.string.overlay_mode_balanced
            else -> com.pixel.gamebooster.R.string.overlay_mode_boost
        }
        scope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, context.getString(labelRes), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun executeGestureAction(action: com.pixel.gamebooster.data.model.OverlayGestureAction) {
        when (action) {
            com.pixel.gamebooster.data.model.OverlayGestureAction.NONE -> Unit
            com.pixel.gamebooster.data.model.OverlayGestureAction.REVEAL_OVERLAY -> {
                setDisplayState(OverlayDisplayState.EXPANDED_PANEL)
            }
            com.pixel.gamebooster.data.model.OverlayGestureAction.TOGGLE_RECORDING -> toggleRecording()
            com.pixel.gamebooster.data.model.OverlayGestureAction.TAKE_SCREENSHOT -> takeScreenshot()
            com.pixel.gamebooster.data.model.OverlayGestureAction.TOGGLE_FPS -> toggleLiveFps()
            com.pixel.gamebooster.data.model.OverlayGestureAction.PURGE_RAM -> {
                purgeRamNow()
                scope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.pixel.gamebooster.R.string.overlay_tile_boost_ram_toast),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            com.pixel.gamebooster.data.model.OverlayGestureAction.CYCLE_PERFORMANCE_MODE -> cycleInterventionMode()
        }
    }

    fun toggleDnd() {
        val targetDnd = !_uiState.value.dndEnabled
        _uiState.update { it.copy(dndEnabled = targetDnd) }
        scope.launch(Dispatchers.IO) {
            if (targetDnd) {
                DNDManager.applyInterruptionPolicy(context, true)
            } else {
                DNDManager.restoreInterruptionPolicy(context)
            }
            updatePersistedProfile { it.copy(interruptionMode = if (targetDnd) 1 else 0) }
        }
    }

    fun toggle120Hz() {
        val targetLock = !_uiState.value.lockRefreshRate
        _uiState.update { it.copy(lockRefreshRate = targetLock) }
        scope.launch(Dispatchers.IO) {
            if (targetLock) {
                RefreshRateUtils.applyFixedRefreshRate(context, 120f)
                RefreshRateUtils.lockPreferredDisplayMode(context, 120)
            } else {
                RefreshRateUtils.resetPreferredDisplayMode(context)
            }
            updatePersistedProfile { it.copy(lockRefreshRate = targetLock) }
        }
    }

    private var brightnessWriteJob: Job? = null

    fun setBrightness(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(brightnessPercent = clamped, autoBrightnessLocked = true) }
        val (minB, maxB) = resolveBrightnessRange(context)
        val unitLinear = gammaToLinearBrightness(clamped)
        val linearFloat = (minB + unitLinear * (maxB - minB)).coerceIn(0.004f, maxB)
        val brightnessInt = (unitLinear * 255f).roundToInt().coerceIn(1, 255)
        val formattedFloat = String.format(java.util.Locale.US, "%.4f", linearFloat)
        val hasShizuku = ShellUtils.isShizukuAvailable() && ShellUtils.hasShizukuPermission()
        val canWrite = Settings.System.canWrite(context)

        brightnessWriteJob?.cancel()
        brightnessWriteJob = scope.launch(Dispatchers.IO) {
            if (canWrite) {
                runCatching {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    )
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightnessInt,
                    )
                }
            }
            if (hasShizuku) {
                if (canWrite) delay(35L) else delay(40L)
                val cmd = if (canWrite) {
                    "settings put system screen_brightness_float $formattedFloat"
                } else {
                    "settings put system screen_brightness_mode 0 && settings put system screen_brightness_float $formattedFloat && settings put system screen_brightness $brightnessInt"
                }
                ShellUtils.runCommand(context, cmd)
            }
        }
    }

    fun setMediaVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(mediaVolumePercent = clamped) }
        scope.launch(Dispatchers.IO) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * maxVol).toInt()
                runCatching {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                }
            }
        }
    }

    fun toggleLiveFps() {
        val target = !_uiState.value.showLiveFps
        _uiState.update { it.copy(showLiveFps = target) }
        repository.setShowLiveFps(target)
        updateMonitoringState()
    }

    fun toggleQuickTap() {
        val targetLock = !_uiState.value.quickTapLocked
        _uiState.update { it.copy(quickTapLocked = targetLock) }
        scope.launch(Dispatchers.IO) {
            val cmd = if (targetLock) "settings put secure columbus_enabled 0" else "settings put secure columbus_enabled 1"
            ShellUtils.runCommand(context, cmd)
            updatePersistedProfile { it.copy(disableQuickTap = targetLock) }
        }
    }

    fun toggleAutoBrightness() {
        val targetLock = !_uiState.value.autoBrightnessLocked
        _uiState.update { it.copy(autoBrightnessLocked = targetLock) }
        scope.launch(Dispatchers.IO) {
            val cmd = if (targetLock) "settings put system screen_brightness_mode 0" else "settings put system screen_brightness_mode 1"
            ShellUtils.runCommand(context, cmd)
            updatePersistedProfile { it.copy(disableAutoBrightness = targetLock) }
        }
    }

    fun toggleBackGestureLock() {
        val currentlyLocked = GameModeManager.isBackGestureSystemLocked(context)
        val targetLock = !currentlyLocked
        _uiState.update { it.copy(backGestureLocked = targetLock) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setBackGestureLock(context, targetLock)
            updatePersistedProfile { it.copy(lockBackGesture = targetLock) }
        }
    }

    fun setFpsOverride(fps: Int) {
        _uiState.update { it.copy(fpsOverride = fps) }
        val mode = _uiState.value.interventionMode
        scope.launch(Dispatchers.IO) {
            GameModeManager.applyGameIntervention(context, packageName, mode, fps)
            updatePersistedProfile { it.copy(fpsOverride = fps) }
        }
    }

    fun setRecording(recording: Boolean) {
        _uiState.update { it.copy(isRecording = recording) }
    }

    fun stopRecording() {
        if (com.pixel.gamebooster.recording.ScreenRecordService.isRecording.value) {
            com.pixel.gamebooster.recording.ScreenRecordService.stopRecording(context)
        }
    }

    fun toggleRecording() {
        if (com.pixel.gamebooster.recording.ScreenRecordService.isRecording.value) {
            com.pixel.gamebooster.recording.ScreenRecordService.stopRecording(context)
        } else {
            ScreenRecordTrampolineActivity.launch(context)
        }
    }

    fun takeScreenshot() {
        setDisplayState(OverlayDisplayState.COLLAPSED_PILL)
        scope.launch {
            // Wait for the expanded panel exit animation and window collapse before capturing
            delay(320L)
            val capturedViaA11y = com.pixel.gamebooster.services.GameBoosterAccessibilityService.instance
                ?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                ?: false
            if (!capturedViaA11y) {
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    ShellUtils.runCommand(context, "input keyevent 120")
                }
            }
        }
    }

    fun onTileClick(tile: OverlayTileType) {
        when (tile) {
            OverlayTileType.LOCK_120HZ -> toggle120Hz()
            OverlayTileType.GAME_DND -> toggleDnd()
            OverlayTileType.SCREEN_RECORD -> toggleRecording()
            OverlayTileType.SCREENSHOT -> takeScreenshot()
            OverlayTileType.LIVE_FPS -> toggleLiveFps()
            OverlayTileType.AUTO_BRIGHTNESS_LOCK -> toggleAutoBrightness()
            OverlayTileType.BACK_GESTURE_LOCK -> toggleBackGestureLock()
            OverlayTileType.QUICK_TAP_LOCK -> toggleQuickTap()
            OverlayTileType.BOOST_RAM -> purgeRamNow()
            OverlayTileType.CACHED_FREEZER -> toggleCachedFreezer()
            OverlayTileType.WIFI_LOW_LATENCY -> toggleWifiLowLatency()
            OverlayTileType.WIFI_SUPPRESS_SCANS -> toggleWifiSuppressScans()
            OverlayTileType.WIFI_LOCK_SUFFICIENCY -> toggleWifiLockSufficiency()
            OverlayTileType.WIFI_PREVENT_ARP -> toggleWifiPreventArp()
            OverlayTileType.PRIVATE_DNS_BYPASS -> togglePrivateDnsBypass()
            OverlayTileType.PAUSE_AUTO_SYNC -> togglePauseAutoSync()
            OverlayTileType.BLOCK_BATTERY_SAVER -> toggleBlockBatterySaver()
            OverlayTileType.SUSPEND_APPS -> toggleAppSuspension()
        }
    }

    private var ramBoostResetJob: Job? = null

    fun purgeRamNow() {
        _uiState.update { it.copy(ramBoostActive = true) }
        ramBoostResetJob?.cancel()
        ramBoostResetJob = scope.launch {
            delay(900L)
            _uiState.update { it.copy(ramBoostActive = false) }
        }
        scope.launch(Dispatchers.IO) {
            GameModeManager.purgeRamNow(context)
        }
    }

    fun toggleCachedFreezer() {
        val target = !_uiState.value.cachedFreezerEnabled
        _uiState.update { it.copy(cachedFreezerEnabled = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setCachedFreezerLive(context, target)
            updatePersistedProfile { it.copy(cachedFreezer = target) }
        }
    }

    fun toggleWifiLowLatency() {
        val target = !_uiState.value.wifiLowLatencyEnabled
        _uiState.update { it.copy(wifiLowLatencyEnabled = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setWifiLowLatencyLive(context, target)
            updatePersistedProfile { it.copy(stabilizeWifi = true, wifiLowLatency = target) }
        }
    }

    fun toggleWifiSuppressScans() {
        val target = !_uiState.value.wifiSuppressScansEnabled
        _uiState.update { it.copy(wifiSuppressScansEnabled = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setWifiSuppressScansLive(context, target)
            updatePersistedProfile { it.copy(stabilizeWifi = true, wifiSuppressScans = target) }
        }
    }

    fun toggleWifiLockSufficiency() {
        val target = !_uiState.value.wifiLockSufficiencyEnabled
        _uiState.update { it.copy(wifiLockSufficiencyEnabled = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setWifiLockSufficiencyLive(context, target)
            updatePersistedProfile { it.copy(stabilizeWifi = true, wifiLockSufficiency = target) }
        }
    }

    fun toggleWifiPreventArp() {
        val target = !_uiState.value.wifiPreventArpEnabled
        _uiState.update { it.copy(wifiPreventArpEnabled = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setWifiPreventArpLive(context, target)
            updatePersistedProfile { it.copy(stabilizeWifi = true, wifiPreventArp = target) }
        }
    }

    fun togglePrivateDnsBypass() {
        val target = !_uiState.value.privateDnsBypassed
        _uiState.update { it.copy(privateDnsBypassed = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setPrivateDnsBypassLive(context, target)
            updatePersistedProfile { it.copy(disablePrivateDns = target) }
        }
    }

    fun togglePauseAutoSync() {
        val target = !_uiState.value.autoSyncPaused
        _uiState.update { it.copy(autoSyncPaused = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setPauseAutoSyncLive(context, target)
            updatePersistedProfile { it.copy(pauseAutoSync = target) }
        }
    }

    fun toggleBlockBatterySaver() {
        val target = !_uiState.value.batterySaverBlocked
        _uiState.update { it.copy(batterySaverBlocked = target) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setBlockBatterySaverLive(context, target)
            updatePersistedProfile { it.copy(blockBatterySaver = target) }
        }
    }

    fun toggleAppSuspension() {
        val target = !_uiState.value.appsSuspended
        _uiState.update { it.copy(appsSuspended = target) }
        scope.launch(Dispatchers.IO) {
            val profile = runCatching { repository.getEffectiveProfile(packageName) }.getOrDefault(repository.createDefaultEffectiveProfile(packageName))
            GameModeManager.setAppSuspensionLive(
                context = context,
                gamePackage = packageName,
                apps = profile.suspendedApps,
                services = repository.suspendedServices.value,
                suspended = target,
            )
        }
    }

    fun onCleared() {
        fpsMonitorJob?.cancel()
        telemetryJob?.cancel()
        TaskFpsMonitor.unregister()
        scope.cancel()
    }

    private suspend fun updatePersistedProfile(transform: (GameProfile) -> GameProfile): GameProfile {
        val current = repository.getEffectiveProfile(packageName)
        val updated = transform(current)
        profilePersistDebounce.value = updated
        return updated
    }

    companion object {
        private const val TAG = "PixelGameBooster:OverlayVM"
        private val WHITESPACE_REGEX = Regex("\\s+")

        @Volatile
        private var cachedBrightnessRange: Pair<Float, Float>? = null

        private fun getBrightnessInfoViaShizuku(): Any? {
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    HiddenApiBypass.addHiddenApiExemptions("")
                }
                val rawDm = SystemServiceHelper.getSystemService("display") ?: return@runCatching null
                val dmBinder = ShizukuBinderWrapper(rawDm)
                val dmStub = Class.forName("android.hardware.display.IDisplayManager\$Stub")
                val idm = dmStub.getMethod("asInterface", IBinder::class.java).invoke(null, dmBinder)
                    ?: return@runCatching null
                idm.javaClass.getMethod("getBrightnessInfo", Int::class.javaPrimitiveType)
                    .invoke(idm, Display.DEFAULT_DISPLAY)
            }.onFailure { e ->
                Log.w(TAG, "getBrightnessInfoViaShizuku failed", e)
            }.getOrNull()
        }

        private fun getBrightnessViaShizuku(): Float? {
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    HiddenApiBypass.addHiddenApiExemptions("")
                }
                val rawDm = SystemServiceHelper.getSystemService("display") ?: return@runCatching null
                val dmBinder = ShizukuBinderWrapper(rawDm)
                val dmStub = Class.forName("android.hardware.display.IDisplayManager\$Stub")
                val idm = dmStub.getMethod("asInterface", IBinder::class.java).invoke(null, dmBinder)
                    ?: return@runCatching null
                idm.javaClass.getMethod("getBrightness", Int::class.javaPrimitiveType)
                    .invoke(idm, Display.DEFAULT_DISPLAY) as? Float
            }.onFailure { e ->
                Log.w(TAG, "getBrightnessViaShizuku failed", e)
            }.getOrNull()
        }
        private fun resolveGameName(context: Context, packageName: String): String {
            return runCatching {
                val pm = context.packageManager
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, 0)
                }
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)
        }

        private fun resolveBatteryLevel(context: Context): Int {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 100
        }

        private fun resolveChargingStatus(context: Context): Boolean {
            return runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }.getOrDefault(false)
        }

        private const val HLG_R = 0.5f
        private const val HLG_A = 0.17883277f
        private const val HLG_B = 0.28466892f
        private const val HLG_C = 0.55991073f

        private fun gammaToLinearBrightness(sliderFraction: Float): Float {
            val normalized = sliderFraction.coerceIn(0f, 1f)
            val ret = if (normalized <= HLG_R) {
                val x = normalized / HLG_R
                x * x
            } else {
                kotlin.math.exp((normalized - HLG_C) / HLG_A) + HLG_B
            }
            return (ret / 12f).coerceIn(0.004f, 1f)
        }

        private fun linearToGammaBrightness(linear: Float): Float {
            val normalized = linear.coerceIn(0f, 1f) * 12f
            val ret = if (normalized <= 1f) {
                kotlin.math.sqrt(normalized) * HLG_R
            } else {
                HLG_A * kotlin.math.ln(normalized - HLG_B) + HLG_C
            }
            return ret.coerceIn(0f, 1f)
        }

        private fun getBrightnessInfo(context: Context): Any? {
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    HiddenApiBypass.addHiddenApiExemptions("")
                }
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return@runCatching null
                val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return@runCatching null
                Display::class.java.getMethod("getBrightnessInfo").invoke(display)
            }.getOrNull()
        }

        private fun resolveBrightnessRange(context: Context): Pair<Float, Float> {
            cachedBrightnessRange?.let { return it }
            val info = getBrightnessInfo(context)
            val min = info?.let { runCatching { it.javaClass.getField("brightnessMinimum").getFloat(it) }.getOrNull() } ?: 0f
            val max = info?.let { runCatching { it.javaClass.getField("brightnessMaximum").getFloat(it) }.getOrNull() } ?: 1f
            return if (max > min && !min.isNaN() && !max.isNaN()) min to max else 0f to 1f
        }

        private fun resolveBrightness(context: Context): Float {
            return runCatching {
                val info = getBrightnessInfo(context)
                val min = info?.let { runCatching { it.javaClass.getField("brightnessMinimum").getFloat(it) }.getOrNull() } ?: 0f
                val max = info?.let { runCatching { it.javaClass.getField("brightnessMaximum").getFloat(it) }.getOrNull() } ?: 1f
                val liveBrightness = info?.let { runCatching { it.javaClass.getField("brightness").getFloat(it) }.getOrNull() }

                val rawLinear = when {
                    liveBrightness != null && !liveBrightness.isNaN() && liveBrightness >= 0f -> liveBrightness
                    else -> {
                        val floatVal = runCatching {
                            Settings.System.getFloat(context.contentResolver, "screen_brightness_float", -1f)
                        }.getOrDefault(-1f)
                        if (floatVal in 0f..1f) {
                            floatVal
                        } else {
                            val cur = Settings.System.getInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                128,
                            )
                            (cur / 255f).coerceIn(0f, 1f)
                        }
                    }
                }
                val normalized = if (max > min && !min.isNaN() && !max.isNaN()) {
                    ((rawLinear - min) / (max - min)).coerceIn(0f, 1f)
                } else {
                    rawLinear.coerceIn(0f, 1f)
                }
                linearToGammaBrightness(normalized)
            }.getOrDefault(0.8f)
        }

        private fun resolveVolume(context: Context): Float {
            return runCatching {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@runCatching 0.5f
                val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                (cur.toFloat() / max).coerceIn(0f, 1f)
            }.getOrDefault(0.5f)
        }

        private fun resolveBlurSupport(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.isCrossWindowBlurEnabled ?: false
            } else {
                false
            }
        }

        private fun resolveBatteryTemperature(context: Context): Int {
            return runCatching {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 380) ?: 380
                (tempRaw / 10f).toInt().coerceIn(20, 75)
            }.getOrDefault(38)
        }

        private fun resolveBatteryWattage(context: Context): Float {
            return runCatching {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val currentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000) ?: 4000
                val currentA = kotlin.math.abs(currentUa) / 1_000_000f
                val voltageV = voltageMv / 1000f
                val calculatedW = currentA * voltageV
                if (calculatedW in 0.5f..45f) {
                    (calculatedW * 10).toInt() / 10f
                } else {
                    4.2f
                }
            }.getOrDefault(4.2f)
        }

        private fun resolveCurrentFps(context: Context): Int {
            return runCatching {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                display?.mode?.refreshRate?.roundToInt() ?: display?.refreshRate?.roundToInt() ?: 60
            }.getOrDefault(60)
        }
    }
}
