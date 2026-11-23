package com.pixel.gamebooster.overlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.overlay.model.OverlayDisplayState
import com.pixel.gamebooster.overlay.model.OverlayUiState
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.RefreshRateUtils
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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
                    backGestureLocked = GameModeManager.isBackGestureLocked,
                    netBoostEnabled = initialProfile.stabilizeWifi,
                    overlayStyle = repository.overlayStyle.value,
                    overlayOpacity = repository.overlayOpacity.value,
                    recordDoubleTapStop = repository.recordDoubleTapStop.value,
                )
            }
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

        repository.recordDoubleTapStop
            .onEach { enabled ->
                _uiState.update { it.copy(recordDoubleTapStop = enabled) }
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

        startTelemetryAndFpsMonitoring()
    }

    private var telemetryJob: Job? = null

    private fun startTelemetryAndFpsMonitoring() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                // 1. Measure active display refresh rate as base
                val displayFps = runCatching {
                    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                    val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                    display?.mode?.refreshRate?.roundToInt() ?: display?.refreshRate?.roundToInt() ?: 60
                }.getOrDefault(60)

                // 2. Query real-time SurfaceFlinger render latency if Shizuku is authorized
                var measuredFps = displayFps
                if (ShellUtils.isShizukuAvailable() && ShellUtils.hasShizukuPermission() && packageName.isNotBlank()) {
                    runCatching {
                        val layerCmd = "dumpsys SurfaceFlinger --list | grep -F \"$packageName\" | head -n 1"
                        val layer = ShellUtils.runCommandWithOutput(context, layerCmd)?.trim()
                        val output = if (!layer.isNullOrBlank()) {
                            ShellUtils.runCommandWithOutput(context, "dumpsys SurfaceFlinger --latency \"$layer\"")
                        } else {
                            ShellUtils.runCommandWithOutput(context, "dumpsys SurfaceFlinger --latency $packageName")
                        }
                        if (!output.isNullOrBlank()) {
                            val lines = output.trim().split("\n")
                            if (lines.size > 2) {
                                val now = System.nanoTime()
                                val oneSecAgo = now - 1_000_000_000L
                                val timestamps = lines.drop(1).mapNotNull { line ->
                                    val parts = line.trim().split(Regex("\\s+"))
                                    if (parts.size >= 2) parts[1].toLongOrNull() else null
                                }.filter { it in oneSecAgo..now }

                                if (timestamps.isNotEmpty()) {
                                    measuredFps = timestamps.size.coerceIn(1, displayFps)
                                }
                            }
                        }
                    }
                }

                // 3. Read live battery, temperature, and current power draw
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
                        currentFps = measuredFps,
                        batteryPercent = batteryLevel,
                        temperatureC = temperature,
                        wattageW = wattage,
                    )
                }

                delay(1000L)
            }
        }
    }

    fun setDisplayState(state: OverlayDisplayState) {
        _uiState.update { it.copy(displayState = state) }
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

    fun setBrightness(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(brightnessPercent = clamped, autoBrightnessLocked = true) }
        scope.launch(Dispatchers.IO) {
            val brightnessInt = (clamped * 255).toInt()
            if (Settings.System.canWrite(context)) {
                runCatching {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    )
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightnessInt
                    )
                }
            } else {
                ShellUtils.runCommand(context, "settings put system screen_brightness_mode 0 && settings put system screen_brightness $brightnessInt")
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

    fun toggleNetBoost() {
        val targetBoost = !_uiState.value.netBoostEnabled
        _uiState.update { it.copy(netBoostEnabled = targetBoost) }
        scope.launch(Dispatchers.IO) {
            updatePersistedProfile {
                it.copy(
                    stabilizeWifi = targetBoost,
                    wifiLowLatency = targetBoost,
                    wifiSuppressScans = targetBoost,
                    wifiLockSufficiency = targetBoost,
                    wifiPreventArp = targetBoost,
                    disablePrivateDns = targetBoost,
                    pauseAutoSync = targetBoost,
                    blockBatterySaver = targetBoost,
                )
            }
        }
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
        val targetLock = !_uiState.value.backGestureLocked
        _uiState.update { it.copy(backGestureLocked = targetLock) }
        scope.launch(Dispatchers.IO) {
            GameModeManager.setBackGestureLock(context, targetLock)
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

    fun onCleared() {
        telemetryJob?.cancel()
        scope.coroutineContext.cancelChildren()
    }

    private suspend fun updatePersistedProfile(transform: (GameProfile) -> GameProfile): GameProfile {
        val current = repository.getEffectiveProfile(packageName)
        val updated = transform(current)
        profilePersistDebounce.value = updated
        return updated
    }

    companion object {
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

        private fun resolveBrightness(context: Context): Float {
            return runCatching {
                val cur = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (cur / 255f).coerceIn(0f, 1f)
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
