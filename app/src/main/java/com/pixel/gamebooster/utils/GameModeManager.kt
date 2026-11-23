package com.pixel.gamebooster.utils

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.overlay.GameOverlayManager
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.services.GameBoosterAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object GameModeManager {
    private const val TAG = "PixelGameBooster:Manager"

    @Volatile
    var isGameModeActive: Boolean = false
        private set

    @Volatile
    var activeGamePackage: String? = null
        private set

    private var snapshottedRefreshRate: RefreshRateUtils.RefreshRateSnapshot? = null
    private var snapshottedAutoBrightness: Int? = null
    private var snapshottedColumbusEnabled: Int? = null
    private var snapshottedAutoSync: Boolean? = null
    private var snapshottedWifiScanAlwaysEnabled: Int? = null
    private var snapshottedBleScanAlwaysEnabled: Int? = null
    private var snapshottedAdaptiveConnectivity: Int? = null
    private var snapshottedNetworkRecommendations: String? = null
    private var snapshottedNetworkAvoidBadWifi: String? = null
    private var snapshottedPrivateDnsMode: String? = null
    private var snapshottedPrivateDnsSpecifier: String? = null
    private var snapshottedWifiWakeup: Int? = null
    private var snapshottedWifiNotification: Int? = null
    private var snapshottedWifiPowerSave: String? = null
    private var snapshottedLowPowerTriggerLevel: Int? = null
    private var snapshottedLowPower: Int? = null
    private var snapshottedBackScaleLeft: String? = null
    private var snapshottedBackScaleRight: String? = null

    @Volatile
    var isBackGestureLocked: Boolean = false
        private set

    private var didDisableAutoBrightness = false
    private var didDisableColumbus = false
    private var didLockDisplayPreferredMode = false
    private var didApplyGameIntervention = false
    private var didLockWifiNetworkSelection = false
    private var didDisableIpReachDisconnect = false
    private var didDisablePrivateDns = false

    private var activeGameSuspendedApps: Set<String> = emptySet()

    private var wifiHighPerfLock: WifiManager.WifiLock? = null
    private var wifiLowLatencyLock: WifiManager.WifiLock? = null

    private var batterySaverReceiver: BroadcastReceiver? = null
    private var notifiedBattery20 = false
    private var notifiedBattery10 = false
    private var notifiedBattery5 = false

    private val scope = CoroutineScope(Dispatchers.IO)
    private var pendingRestoreJob: Job? = null
    private var gameWakeLock: PowerManager.WakeLock? = null

    fun init(context: Context) {
        DNDManager.recoverLeakedDndState(context)
    }

    fun onGameForeground(context: Context, packageName: String, repo: SettingsRepository) {
        if (!repo.isBoosterEnabled.value) return
        if (!GameDetectionUtils.isGame(context, packageName, repo.gameApps.value)) return

        if (repo.isOverlayEnabled.value && PermissionHelper.hasOverlayPermission(context)) {
            ContextCompat.getMainExecutor(context).execute {
                GameOverlayManager.show(context, packageName, repo)
            }
        }

        pendingRestoreJob?.cancel()
        pendingRestoreJob = null

        if (isGameModeActive && activeGamePackage == packageName) return

        activeGamePackage = packageName
        isGameModeActive = true
        Log.d(TAG, "Entering Game Mode for $packageName")

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (gameWakeLock == null && pm != null) {
            runCatching {
                @Suppress("DEPRECATION")
                gameWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "pixelgamebooster:game_mode_wake_lock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(2 * 60 * 60 * 1000L) // Safety timeout: 2 hours max
                }
                Log.d(TAG, "Acquired game mode screen wake lock")
            }
        }

        scope.launch {
            val profile = try {
                repo.getEffectiveProfile(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load effective profile for $packageName", e)
                repo.createDefaultEffectiveProfile(packageName)
            }

            // 0. Interruption Policy (DND)
            if (profile.interruptionMode != 0) {
                try {
                    DNDManager.applyInterruptionPolicy(context, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply interruption policy for $packageName", e)
                }
            }

            // 1. Auto-Brightness: Fast-path ContentResolver write with shell fallback
            if (profile.disableAutoBrightness) {
                try {
                    val currentMode = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                    )
                    didDisableAutoBrightness = true
                    snapshottedAutoBrightness = if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    } else {
                        currentMode
                    }

                    runCatching {
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                        )
                    }
                    ShellUtils.runCommand(context, "settings put system screen_brightness_mode 0")
                    Log.d(TAG, "Disabled Auto Brightness (was $currentMode, restore target: $snapshottedAutoBrightness)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed auto-brightness toggle", e)
                }
            }

            // 2. Quick Tap (Columbus): Fast-path ContentResolver write with shell fallback
            if (profile.disableQuickTap) {
                try {
                    val currentColumbus = Settings.Secure.getInt(
                        context.contentResolver,
                        "columbus_enabled",
                        -1,
                    )
                    if (currentColumbus == 1) {
                        snapshottedColumbusEnabled = 1
                        didDisableColumbus = true

                        val wrote = runCatching {
                            Settings.Secure.putInt(
                                context.contentResolver,
                                "columbus_enabled",
                                0,
                            )
                        }.getOrDefault(false)
                        if (!wrote) {
                            ShellUtils.runCommand(context, "settings put secure columbus_enabled 0")
                        }
                        Log.d(TAG, "Disabled Quick Tap (columbus_enabled=0)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed quick tap disable", e)
                }
            }

            // 3. Fixed 120Hz Refresh Rate & Preferred Mode
            if (profile.lockRefreshRate) {
                try {
                    if (snapshottedRefreshRate == null) {
                        snapshottedRefreshRate = RefreshRateUtils.getCurrentSnapshot(context)
                    }
                    RefreshRateUtils.applyFixedRefreshRate(context, 120f)
                    RefreshRateUtils.lockPreferredDisplayMode(context, 120)
                    didLockDisplayPreferredMode = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed refresh rate lock", e)
                }
            }

            // 4. Memory Purge
            if (profile.boostRam) {
                ShellUtils.runCommand(context, "am kill-all")
            }

            // 5. Kernel Cached Apps Freezer
            if (profile.cachedFreezer) {
                ShellUtils.runCommand(context, "settings put global cached_apps_freezer enabled")
            }

            // 6. Freeze user-selected background apps (Custom app suspension)
            val appsToSuspend = profile.suspendedApps
            activeGameSuspendedApps = appsToSuspend
            if (appsToSuspend.isNotEmpty()) {
                val suspendCmd = appsToSuspend.joinToString(" && ") { "cmd package suspend --user 0 $it && am force-stop $it" }
                ShellUtils.runCommand(context, suspendCmd)
                Log.d(TAG, "Suspended ${appsToSuspend.size} user background apps: $appsToSuspend")
            }

            // 7. Freeze system services
            val servicesToSuspend = repo.suspendedServices.value
            val sysCmds = mutableListOf<String>()
            if (servicesToSuspend.contains("com.google.android.aicore")) {
                sysCmds.add("pm disable-user --user 0 com.google.android.aicore")
            }
            if (servicesToSuspend.contains("com.google.android.apps.turbo")) {
                sysCmds.add("pm disable-user --user 0 com.google.android.apps.turbo && pm disable-user --user 0 com.google.android.turboadapter")
            }
            if (servicesToSuspend.contains("com.google.android.settings.intelligence")) {
                sysCmds.add("pm disable-user --user 0 com.google.android.settings.intelligence")
            }
            if (servicesToSuspend.contains("com.google.android.apps.scone")) {
                sysCmds.add("pm disable-user --user 0 com.google.android.apps.scone")
            }
            if (servicesToSuspend.contains("com.google.android.apps.carrier.carrierwifi")) {
                sysCmds.add("cmd package suspend --user 0 com.google.android.apps.carrier.carrierwifi && am force-stop com.google.android.apps.carrier.carrierwifi")
            }
            if (servicesToSuspend.contains("com.google.android.projection.gearhead")) {
                sysCmds.add("cmd package suspend --user 0 com.google.android.projection.gearhead && am force-stop com.google.android.projection.gearhead")
            }
            if (servicesToSuspend.contains("com.google.android.as")) {
                sysCmds.add("cmd package suspend --user 0 com.google.android.as && am force-stop com.google.android.as")
            }
            if (sysCmds.isNotEmpty()) {
                ShellUtils.runCommand(context, sysCmds.joinToString(" && "))
            }

            // 8. Game Manager intervention
            val mode = profile.interventionMode
            val modeName = when (mode) {
                1 -> "standard"
                3 -> "battery"
                else -> "performance"
            }
            val fps = profile.fpsOverride
            val gameCmd = if (fps > 0) {
                "cmd game set --downscale disable --fps $fps --mode $mode $packageName && cmd game mode $modeName $packageName"
            } else {
                "cmd game set --downscale disable --mode $mode $packageName && cmd game mode $modeName $packageName"
            }
            ShellUtils.runCommand(context, gameCmd)
            didApplyGameIntervention = true

            // 9. Wi-Fi Latency Stabilization & Dual WifiLocks
            if (profile.stabilizeWifi) {
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val wifiCmds = mutableListOf<String>()

                    // 9a. Low-Latency Wi-Fi lock & power save bypass
                    if (profile.wifiLowLatency) {
                        if (wifiHighPerfLock == null) {
                            @Suppress("DEPRECATION")
                            val highPerf = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PixelGameBooster:HighPerf")
                            highPerf?.setReferenceCounted(false)
                            highPerf?.acquire()
                            wifiHighPerfLock = highPerf
                        }
                        if (wifiLowLatencyLock == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val lowLatency = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "PixelGameBooster:LowLatency")
                            lowLatency?.setReferenceCounted(false)
                            lowLatency?.acquire()
                            wifiLowLatencyLock = lowLatency
                        }
                        if (snapshottedWifiPowerSave == null) {
                            snapshottedWifiPowerSave = Settings.Global.getString(
                                context.contentResolver,
                                "wifi_power_save",
                            ) ?: "UNSET"
                        }
                        wifiCmds.add("settings put global wifi_power_save 0")
                    }

                    // 9b. Suppress background Wi-Fi and BLE scans
                    if (profile.wifiSuppressScans) {
                        if (snapshottedWifiScanAlwaysEnabled == null) {
                            snapshottedWifiScanAlwaysEnabled = Settings.Global.getInt(
                                context.contentResolver,
                                "wifi_scan_always_enabled",
                                0,
                            )
                        }
                        if (snapshottedBleScanAlwaysEnabled == null) {
                            snapshottedBleScanAlwaysEnabled = Settings.Global.getInt(
                                context.contentResolver,
                                "ble_scan_always_enabled",
                                0,
                            )
                        }
                        if (snapshottedWifiWakeup == null) {
                            snapshottedWifiWakeup = runCatching {
                                Settings.Global.getInt(
                                    context.contentResolver,
                                    "wifi_wakeup_enabled",
                                    1,
                                )
                            }.getOrDefault(1)
                        }
                        if (snapshottedWifiNotification == null) {
                            snapshottedWifiNotification = runCatching {
                                Settings.Global.getInt(
                                    context.contentResolver,
                                    "wifi_networks_available_notification_on",
                                    1,
                                )
                            }.getOrDefault(1)
                        }
                        wifiCmds.add("settings put global wifi_scan_always_enabled 0")
                        wifiCmds.add("settings put global ble_scan_always_enabled 0")
                        wifiCmds.add("settings put global wifi_scan_throttle_enabled 1")
                        wifiCmds.add("settings put global wifi_wakeup_enabled 0")
                        wifiCmds.add("settings put global wifi_networks_available_notification_on 0")
                        wifiCmds.add("cmd wifi set-scan-always-available disabled")
                    }

                    // 9c. Lock network sufficiency & disable network scoring ping churn
                    if (profile.wifiLockSufficiency) {
                        if (snapshottedAdaptiveConnectivity == null) {
                            snapshottedAdaptiveConnectivity = runCatching {
                                Settings.Secure.getInt(
                                    context.contentResolver,
                                    "adaptive_connectivity_enabled",
                                    1,
                                )
                            }.getOrDefault(1)
                        }
                        if (snapshottedNetworkRecommendations == null) {
                            snapshottedNetworkRecommendations = Settings.Global.getString(
                                context.contentResolver,
                                "network_recommendations_enabled",
                            ) ?: "UNSET"
                        }
                        if (snapshottedNetworkAvoidBadWifi == null) {
                            snapshottedNetworkAvoidBadWifi = Settings.Global.getString(
                                context.contentResolver,
                                "network_avoid_bad_wifi",
                            ) ?: "UNSET"
                        }
                        wifiCmds.add("settings put secure adaptive_connectivity_enabled 0")
                        wifiCmds.add("settings put global network_recommendations_enabled 0")
                        wifiCmds.add("settings put global network_avoid_bad_wifi 0")
                        wifiCmds.add("cmd wifi set-network-selection-config enabled enabled -a 0")
                        didLockWifiNetworkSelection = true
                    }

                    // 9d. Prevent ARP disconnects
                    if (profile.wifiPreventArp) {
                        wifiCmds.add("cmd wifi set-ipreach-disconnect disabled")
                        didDisableIpReachDisconnect = true
                    }

                    if (wifiCmds.isNotEmpty()) {
                        ShellUtils.runCommand(context, wifiCmds.joinToString(" && "))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed Wi-Fi stabilization", e)
                }
            }

            // 10. Disable Private DNS (DoT) while gaming to avoid DNS query latency stalls
            if (profile.disablePrivateDns) {
                try {
                    val currentMode = Settings.Global.getString(
                        context.contentResolver,
                        "private_dns_mode",
                    )
                    val currentSpecifier = Settings.Global.getString(
                        context.contentResolver,
                        "private_dns_specifier",
                    )

                    if (!currentMode.isNullOrEmpty() && currentMode != "off") {
                        snapshottedPrivateDnsMode = currentMode
                        snapshottedPrivateDnsSpecifier = currentSpecifier
                        didDisablePrivateDns = true

                        runCatching {
                            Settings.Global.putString(context.contentResolver, "private_dns_mode", "off")
                        }
                        ShellUtils.runCommand(context, "settings put global private_dns_mode off")
                        Log.d(TAG, "Disabled Private DNS (was mode=$currentMode, specifier=$currentSpecifier)")
                    } else if (!currentSpecifier.isNullOrEmpty()) {
                        snapshottedPrivateDnsMode = "hostname"
                        snapshottedPrivateDnsSpecifier = currentSpecifier
                        didDisablePrivateDns = true

                        runCatching {
                            Settings.Global.putString(context.contentResolver, "private_dns_mode", "off")
                        }
                        ShellUtils.runCommand(context, "settings put global private_dns_mode off")
                        Log.d(TAG, "Disabled Private DNS (was hostname with specifier=$currentSpecifier)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to disable private DNS", e)
                }
            }

            // 11. Battery Saver Suppress & Continuous Defensive Receiver
            if (profile.blockBatterySaver) {
                try {
                    notifiedBattery20 = false
                    notifiedBattery10 = false
                    notifiedBattery5 = false

                    if (snapshottedLowPowerTriggerLevel == null) {
                        snapshottedLowPowerTriggerLevel = Settings.Global.getInt(
                            context.contentResolver,
                            "low_power_trigger_level",
                            0,
                        )
                    }
                    if (snapshottedLowPower == null) {
                        snapshottedLowPower = Settings.Global.getInt(
                            context.contentResolver,
                            "low_power",
                            0,
                        )
                    }

                    ShellUtils.runCommand(
                        context,
                        "settings put global low_power_trigger_level 0 && settings put global low_power 0 && cmd power set-mode 0",
                    )

                    if (batterySaverReceiver == null) {
                        val filter = IntentFilter().apply {
                            addAction(Intent.ACTION_BATTERY_CHANGED)
                            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                        }
                        val receiver = object : BroadcastReceiver() {
                            override fun onReceive(c: Context?, intent: Intent?) {
                                if (!isGameModeActive) return
                                when (intent?.action) {
                                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                                        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                                        if (pm?.isPowerSaveMode == true) {
                                            ShellUtils.runCommand(
                                                context,
                                                "settings put global low_power 0 && cmd power set-mode 0",
                                            )
                                        }
                                    }
                                    Intent.ACTION_BATTERY_CHANGED -> {
                                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                            status == BatteryManager.BATTERY_STATUS_FULL
                                        if (isCharging) {
                                            notifiedBattery20 = false
                                            notifiedBattery10 = false
                                            notifiedBattery5 = false
                                        } else if (level >= 0 && scale > 0) {
                                            val pct = (level * 100) / scale
                                            if (pct > 20) {
                                                notifiedBattery20 = false
                                                notifiedBattery10 = false
                                                notifiedBattery5 = false
                                            } else if (pct in 11..20 && !notifiedBattery20) {
                                                notifiedBattery20 = true
                                                showToast(context, context.getString(R.string.game_mode_battery_low_20))
                                            } else if (pct in 6..10 && !notifiedBattery10) {
                                                notifiedBattery10 = true
                                                showToast(context, context.getString(R.string.game_mode_battery_low_10))
                                            } else if (pct in 1..5 && !notifiedBattery5) {
                                                notifiedBattery5 = true
                                                showToast(context, context.getString(R.string.game_mode_battery_low_5))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ContextCompat.registerReceiver(
                            context.applicationContext,
                            receiver,
                            filter,
                            ContextCompat.RECEIVER_NOT_EXPORTED,
                        )
                        batterySaverReceiver = receiver
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to suppress battery saver", e)
                }
            }

            // 12. Pause Master Auto-Sync
            if (profile.pauseAutoSync) {
                try {
                    val sync = ContentResolver.getMasterSyncAutomatically()
                    if (snapshottedAutoSync == null) snapshottedAutoSync = sync
                    if (sync) ContentResolver.setMasterSyncAutomatically(false)
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException pausing auto-sync", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed auto-sync pause", e)
                }
            }

            withContext(Dispatchers.Main) {
                if (repo.isGameModeToastEnabled.value) {
                    Toast.makeText(context, context.getString(R.string.game_mode_toast_activated), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun applyGameIntervention(context: Context, packageName: String, mode: Int, fps: Int = 0) {
        val modeName = when (mode) {
            1 -> "standard"
            3 -> "battery"
            else -> "performance"
        }
        val gameCmd = if (fps > 0) {
            "cmd game set --downscale disable --fps $fps --mode $mode $packageName && cmd game mode $modeName $packageName"
        } else {
            "cmd game set --downscale disable --mode $mode $packageName && cmd game mode $modeName $packageName"
        }
        ShellUtils.runCommand(context, gameCmd)
    }

    private fun isAppInForeground(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val processes = am?.runningAppProcesses ?: return false
            for (proc in processes) {
                if (proc.processName == packageName) {
                    return proc.importance <= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check process importance for $packageName", e)
            false
        }
    }

    fun onGameBackground(context: Context) {
        DNDManager.restoreInterruptionPolicy(context)
        ContextCompat.getMainExecutor(context).execute {
            GameOverlayManager.hide()
        }
    }

    fun onGameBackground(
        context: Context,
        oldPackage: String?,
        newPackage: String?,
        repo: SettingsRepository,
    ) {
        Log.d(TAG, "onGameBackground: old=$oldPackage, new=$newPackage, active=$isGameModeActive, activePkg=$activeGamePackage")
        if (!isGameModeActive) return
        if (newPackage == context.packageName) return
        if (newPackage == "com.google.android.gms" ||
            newPackage == "com.android.vending" ||
            newPackage == "android" ||
            newPackage?.startsWith("android.service.games") == true
        ) return

        val gameApps = repo.gameApps.value
        if (newPackage != null && GameDetectionUtils.isGame(context, newPackage, gameApps)) {
            // Switched directly between two games in the game list
            return
        }

        if (pendingRestoreJob?.isActive == true) {
            return
        }

        pendingRestoreJob?.cancel()
        pendingRestoreJob = scope.launch {
            val currentPkg = activeGamePackage ?: oldPackage
            if (currentPkg != null) {
                // 1. Verify with topResumedActivity and mCurrentFocus in shell
                val topResumed = ShellUtils.runCommandWithOutput(context, "dumpsys activity activities | grep topResumedActivity", notifyOnError = false)
                val currentFocus = if (topResumed.isNullOrBlank() || !topResumed.contains(currentPkg)) {
                    ShellUtils.runCommandWithOutput(context, "dumpsys window | grep mCurrentFocus", notifyOnError = false)
                } else null

                if ((!topResumed.isNullOrBlank() && (topResumed.contains(currentPkg) || gameApps.any { topResumed.contains(it) })) ||
                    (!currentFocus.isNullOrBlank() && (currentFocus.contains(currentPkg) || gameApps.any { currentFocus.contains(it) }))
                ) {
                    Log.d(TAG, "Restore aborted: Shell indicates game is still topResumed/focused: topResumed=$topResumed, focus=$currentFocus")
                    return@launch
                }

                // 2. Secondary fallback check via ActivityManager process importance
                if (isAppInForeground(context, currentPkg)) {
                    Log.d(TAG, "Restore aborted: $currentPkg is still in foreground")
                    return@launch
                }

                restoreGameMode(context, currentPkg, repo)
            }
        }
    }

    fun restoreGameMode(context: Context, gamePackage: String, repo: SettingsRepository) {
        if (!isGameModeActive) return
        Log.d(TAG, "Exiting Game Mode for $gamePackage")
        isGameModeActive = false
        activeGamePackage = null
        pendingRestoreJob = null
        GameBoosterAccessibilityService.instance?.resetLastPackage()
        AppDetectionService.instance?.resetLastPackage()

        gameWakeLock?.let { lock ->
            runCatching {
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "Released game mode screen wake lock")
                }
            }
        }
        gameWakeLock = null

        ContextCompat.getMainExecutor(context).execute {
            GameOverlayManager.hide()
        }

        scope.launch {
            // 0. Restore DND interruption policy
            try {
                DNDManager.restoreInterruptionPolicy(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore DND interruption policy", e)
            }

            // 1. Reset Game Manager intervention
            if (didApplyGameIntervention) {
                ShellUtils.runCommand(context, "cmd game reset $gamePackage && cmd game mode standard $gamePackage")
                didApplyGameIntervention = false
            }

            // 2. Unsuspend user apps
            val suspended = if (activeGameSuspendedApps.isNotEmpty()) {
                activeGameSuspendedApps
            } else {
                try {
                    repo.getEffectiveProfile(gamePackage).suspendedApps
                } catch (e: Exception) {
                    repo.suspendedApps.value
                }
            }
            if (suspended.isNotEmpty()) {
                val unsuspendCmd = suspended.joinToString(" && ") { "cmd package unsuspend --user 0 $it && cmd package unstop --user 0 $it" }
                ShellUtils.runCommand(context, unsuspendCmd)
            }
            activeGameSuspendedApps = emptySet()

            // 3. Re-enable system services
            val restoreSysCmds = mutableListOf<String>()
            val services = repo.suspendedServices.value
            if (services.contains("com.google.android.aicore")) {
                restoreSysCmds.add("pm enable com.google.android.aicore")
            }
            if (services.contains("com.google.android.apps.turbo")) {
                restoreSysCmds.add("pm enable com.google.android.apps.turbo && pm enable com.google.android.turboadapter")
            }
            if (services.contains("com.google.android.settings.intelligence")) {
                restoreSysCmds.add("pm enable com.google.android.settings.intelligence")
            }
            if (services.contains("com.google.android.apps.scone")) {
                restoreSysCmds.add("pm enable com.google.android.apps.scone")
            }
            if (services.contains("com.google.android.apps.carrier.carrierwifi")) {
                restoreSysCmds.add("cmd package unsuspend --user 0 com.google.android.apps.carrier.carrierwifi && cmd package unstop --user 0 com.google.android.apps.carrier.carrierwifi")
            }
            if (services.contains("com.google.android.projection.gearhead")) {
                restoreSysCmds.add("cmd package unsuspend --user 0 com.google.android.projection.gearhead && cmd package unstop --user 0 com.google.android.projection.gearhead")
            }
            if (services.contains("com.google.android.as")) {
                restoreSysCmds.add("cmd package unsuspend --user 0 com.google.android.as && cmd package unstop --user 0 com.google.android.as")
            }
            if (restoreSysCmds.isNotEmpty()) {
                ShellUtils.runCommand(context, restoreSysCmds.joinToString(" && "))
            }

            // 4. Restore Display Refresh Rate & Mode
            if (didLockDisplayPreferredMode) {
                RefreshRateUtils.resetPreferredDisplayMode(context)
                didLockDisplayPreferredMode = false
            }
            RefreshRateUtils.restoreRefreshRate(context, snapshottedRefreshRate)
            snapshottedRefreshRate = null

            // 5. Restore Auto-Brightness: in-process putInt first, fallback shell
            if (repo.isDisableAutoBrightness.value || didDisableAutoBrightness) {
                try {
                    val targetMode = snapshottedAutoBrightness ?: Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    Log.d(TAG, "Restoring Auto Brightness to $targetMode")
                    runCatching {
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            targetMode,
                        )
                    }
                    ShellUtils.runCommand(context, "settings put system screen_brightness_mode $targetMode")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore auto brightness", e)
                } finally {
                    snapshottedAutoBrightness = null
                    didDisableAutoBrightness = false
                }
            }

            // 6. Restore Quick Tap: in-process putInt first, fallback shell
            try {
                val prevColumbus = snapshottedColumbusEnabled
                if (prevColumbus != null && (didDisableColumbus || prevColumbus == 1)) {
                    val wrote = runCatching {
                        Settings.Secure.putInt(
                            context.contentResolver,
                            "columbus_enabled",
                            prevColumbus,
                        )
                    }.getOrDefault(false)
                    if (!wrote) {
                        ShellUtils.runCommand(context, "settings put secure columbus_enabled $prevColumbus")
                    }
                    Log.d(TAG, "Restored Quick Tap to $prevColumbus")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore Quick Tap", e)
            } finally {
                snapshottedColumbusEnabled = null
                didDisableColumbus = false
            }

            // 6b. Restore Edge Back-Gesture Lock
            restoreBackGestureLock(context)

            // 7. Restore Wi-Fi Locks & Settings
            wifiHighPerfLock?.let {
                try {
                    if (it.isHeld) it.release()
                } catch (_: Exception) { }
            }
            wifiHighPerfLock = null

            wifiLowLatencyLock?.let {
                try {
                    if (it.isHeld) it.release()
                } catch (_: Exception) { }
            }
            wifiLowLatencyLock = null

            snapshottedWifiScanAlwaysEnabled?.let { prevScan ->
                ShellUtils.runCommand(context, "settings put global wifi_scan_always_enabled $prevScan && cmd wifi set-scan-always-available enabled")
                snapshottedWifiScanAlwaysEnabled = null
            }

            if (didLockWifiNetworkSelection) {
                ShellUtils.runCommand(context, "cmd wifi set-network-selection-config enabled enabled -a 0")
                didLockWifiNetworkSelection = false
            }

            snapshottedWifiWakeup?.let { prevWakeup ->
                ShellUtils.runCommand(context, "settings put global wifi_wakeup_enabled $prevWakeup")
                snapshottedWifiWakeup = null
            }

            snapshottedWifiNotification?.let { prevNotif ->
                ShellUtils.runCommand(context, "settings put global wifi_networks_available_notification_on $prevNotif")
                snapshottedWifiNotification = null
            }

            snapshottedWifiPowerSave?.let { prevPowerSave ->
                val cmd = if (prevPowerSave == "UNSET") {
                    "settings delete global wifi_power_save"
                } else {
                    "settings put global wifi_power_save $prevPowerSave"
                }
                ShellUtils.runCommand(context, cmd)
                snapshottedWifiPowerSave = null
            }

            if (didDisableIpReachDisconnect) {
                ShellUtils.runCommand(context, "cmd wifi set-ipreach-disconnect enabled")
                didDisableIpReachDisconnect = false
            }

            snapshottedBleScanAlwaysEnabled?.let { prevBle ->
                ShellUtils.runCommand(context, "settings put global ble_scan_always_enabled $prevBle")
                snapshottedBleScanAlwaysEnabled = null
            }

            snapshottedAdaptiveConnectivity?.let { prevAdaptive ->
                ShellUtils.runCommand(context, "settings put secure adaptive_connectivity_enabled $prevAdaptive")
                snapshottedAdaptiveConnectivity = null
            }

            snapshottedNetworkRecommendations?.let { prev ->
                val cmd = if (prev == "UNSET") {
                    "settings delete global network_recommendations_enabled"
                } else {
                    "settings put global network_recommendations_enabled $prev"
                }
                ShellUtils.runCommand(context, cmd)
                snapshottedNetworkRecommendations = null
            }

            snapshottedNetworkAvoidBadWifi?.let { prev ->
                val cmd = if (prev == "UNSET") {
                    "settings delete global network_avoid_bad_wifi"
                } else {
                    "settings put global network_avoid_bad_wifi $prev"
                }
                ShellUtils.runCommand(context, cmd)
                snapshottedNetworkAvoidBadWifi = null
            }

            // 8. Restore Private DNS
            try {
                val prevMode = snapshottedPrivateDnsMode
                val prevSpecifier = snapshottedPrivateDnsSpecifier
                val shouldRestore = didDisablePrivateDns || !prevSpecifier.isNullOrEmpty()

                val targetMode = if (!prevMode.isNullOrEmpty() && prevMode != "off") {
                    prevMode
                } else if (!prevSpecifier.isNullOrEmpty()) {
                    "hostname"
                } else null

                if (shouldRestore && targetMode != null) {
                    Log.d(TAG, "Restoring Private DNS to mode=$targetMode, specifier=$prevSpecifier")
                    runCatching {
                        if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                            Settings.Global.putString(context.contentResolver, "private_dns_specifier", prevSpecifier)
                        }
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", targetMode)
                    }
                    val shellCmd = if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                        "settings put global private_dns_specifier $prevSpecifier && settings put global private_dns_mode $targetMode"
                    } else {
                        "settings put global private_dns_mode $targetMode"
                    }
                    ShellUtils.runCommand(context, shellCmd)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore Private DNS", e)
            } finally {
                snapshottedPrivateDnsMode = null
                snapshottedPrivateDnsSpecifier = null
                didDisablePrivateDns = false
            }

            // 9. Restore Battery Saver & Low Power trigger level
            batterySaverReceiver?.let {
                runCatching { context.applicationContext.unregisterReceiver(it) }
            }
            batterySaverReceiver = null
            notifiedBattery20 = false
            notifiedBattery10 = false
            notifiedBattery5 = false

            snapshottedLowPowerTriggerLevel?.let { prevTrigger ->
                ShellUtils.runCommand(context, "settings put global low_power_trigger_level $prevTrigger")
                snapshottedLowPowerTriggerLevel = null
            }

            snapshottedLowPower?.let { prevLowPower ->
                if (prevLowPower == 1) {
                    ShellUtils.runCommand(context, "settings put global low_power 1 && cmd power set-mode 1")
                }
                snapshottedLowPower = null
            }

            // 10. Restore Master Auto-Sync
            snapshottedAutoSync?.let { wasActive ->
                if (wasActive) {
                    try {
                        ContentResolver.setMasterSyncAutomatically(true)
                        Log.d(TAG, "Master auto-sync restored")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "SecurityException restoring auto-sync", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore auto-sync", e)
                    }
                }
                snapshottedAutoSync = null
            }

            withContext(Dispatchers.Main) {
                if (repo.isGameModeToastEnabled.value) {
                    Toast.makeText(context, context.getString(R.string.game_mode_toast_deactivated), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setBackGestureLock(context: Context, locked: Boolean) {
        if (locked) {
            try {
                if (!isBackGestureLocked) {
                    snapshottedBackScaleLeft = Settings.Secure.getString(
                        context.contentResolver,
                        "back_gesture_inset_scale_left",
                    ) ?: "UNSET"
                    snapshottedBackScaleRight = Settings.Secure.getString(
                        context.contentResolver,
                        "back_gesture_inset_scale_right",
                    ) ?: "UNSET"
                }
                isBackGestureLocked = true
                runCatching {
                    Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_left", "-1")
                    Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_right", "-1")
                }
                ShellUtils.runCommand(
                    context,
                    "settings put secure back_gesture_inset_scale_left -1 && settings put secure back_gesture_inset_scale_right -1",
                )
                Log.d(
                    TAG,
                    "Locked back gestures (-1) (prev left=$snapshottedBackScaleLeft, right=$snapshottedBackScaleRight)",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to lock back gestures", e)
            }
        } else {
            restoreBackGestureLock(context)
        }
    }

    fun restoreBackGestureLock(context: Context) {
        if (!isBackGestureLocked && snapshottedBackScaleLeft == null && snapshottedBackScaleRight == null) return
        try {
            val prevLeft = snapshottedBackScaleLeft
            val prevRight = snapshottedBackScaleRight
            val cmds = mutableListOf<String>()

            if (prevLeft != null) {
                if (prevLeft == "UNSET") {
                    cmds.add("settings delete secure back_gesture_inset_scale_left")
                } else {
                    runCatching {
                        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_left", prevLeft)
                    }
                    cmds.add("settings put secure back_gesture_inset_scale_left $prevLeft")
                }
            }
            if (prevRight != null) {
                if (prevRight == "UNSET") {
                    cmds.add("settings delete secure back_gesture_inset_scale_right")
                } else {
                    runCatching {
                        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_right", prevRight)
                    }
                    cmds.add("settings put secure back_gesture_inset_scale_right $prevRight")
                }
            }

            if (cmds.isNotEmpty()) {
                ShellUtils.runCommand(context, cmds.joinToString(" && "))
            }
            Log.d(TAG, "Restored back gestures (left=$prevLeft, right=$prevRight)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore back gestures", e)
        } finally {
            snapshottedBackScaleLeft = null
            snapshottedBackScaleRight = null
            isBackGestureLocked = false
        }
    }

    private fun showToast(context: Context, text: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun restorePrivateDnsIfDisabled(context: Context) {
        scope.launch {
            try {
                val prevMode = snapshottedPrivateDnsMode
                val prevSpecifier = snapshottedPrivateDnsSpecifier
                val shouldRestore = didDisablePrivateDns || !prevSpecifier.isNullOrEmpty()

                val targetMode = if (!prevMode.isNullOrEmpty() && prevMode != "off") {
                    prevMode
                } else if (!prevSpecifier.isNullOrEmpty()) {
                    "hostname"
                } else null

                if (shouldRestore && targetMode != null) {
                    Log.d(TAG, "Restoring Private DNS early to mode=$targetMode, specifier=$prevSpecifier")
                    runCatching {
                        if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                            Settings.Global.putString(context.contentResolver, "private_dns_specifier", prevSpecifier)
                        }
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", targetMode)
                    }
                    val shellCmd = if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                        "settings put global private_dns_specifier $prevSpecifier && settings put global private_dns_mode $targetMode"
                    } else {
                        "settings put global private_dns_mode $targetMode"
                    }
                    ShellUtils.runCommand(context, shellCmd)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore Private DNS early", e)
            } finally {
                snapshottedPrivateDnsMode = null
                snapshottedPrivateDnsSpecifier = null
                didDisablePrivateDns = false
            }
        }
    }

    fun applyNetworkStabilization(context: Context, profile: GameProfile) {
        if (!profile.stabilizeWifi) return

        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiCmds = mutableListOf<String>()

            // 9a. Low-Latency Wi-Fi lock & power save bypass
            if (profile.wifiLowLatency) {
                if (wifiHighPerfLock == null) {
                    @Suppress("DEPRECATION")
                    val highPerf = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PixelGameBooster:HighPerf")
                    highPerf?.setReferenceCounted(false)
                    highPerf?.acquire()
                    wifiHighPerfLock = highPerf
                }
                if (wifiLowLatencyLock == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val lowLatency = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "PixelGameBooster:LowLatency")
                    lowLatency?.setReferenceCounted(false)
                    lowLatency?.acquire()
                    wifiLowLatencyLock = lowLatency
                }
                if (snapshottedWifiPowerSave == null) {
                    snapshottedWifiPowerSave = Settings.Global.getString(
                        context.contentResolver,
                        "wifi_power_save",
                    ) ?: "UNSET"
                }
                wifiCmds.add("settings put global wifi_power_save 0")
            }

            // 9b. Suppress background Wi-Fi and BLE scans
            if (profile.wifiSuppressScans) {
                if (snapshottedWifiScanAlwaysEnabled == null) {
                    snapshottedWifiScanAlwaysEnabled = Settings.Global.getInt(
                        context.contentResolver,
                        "wifi_scan_always_enabled",
                        0,
                    )
                }
                if (snapshottedBleScanAlwaysEnabled == null) {
                    snapshottedBleScanAlwaysEnabled = Settings.Global.getInt(
                        context.contentResolver,
                        "ble_scan_always_enabled",
                        0,
                    )
                }
                if (snapshottedWifiWakeup == null) {
                    snapshottedWifiWakeup = runCatching {
                        Settings.Global.getInt(
                            context.contentResolver,
                            "wifi_wakeup_enabled",
                            1,
                        )
                    }.getOrDefault(1)
                }
                if (snapshottedWifiNotification == null) {
                    snapshottedWifiNotification = runCatching {
                        Settings.Global.getInt(
                            context.contentResolver,
                            "wifi_networks_available_notification_on",
                            1,
                        )
                    }.getOrDefault(1)
                }
                wifiCmds.add("settings put global wifi_scan_always_enabled 0")
                wifiCmds.add("settings put global ble_scan_always_enabled 0")
                wifiCmds.add("settings put global wifi_scan_throttle_enabled 1")
                wifiCmds.add("settings put global wifi_wakeup_enabled 0")
                wifiCmds.add("settings put global wifi_networks_available_notification_on 0")
                wifiCmds.add("cmd wifi set-scan-always-available disabled")
            }

            // 9c. Lock network sufficiency & disable network scoring ping churn
            if (profile.wifiLockSufficiency) {
                if (snapshottedAdaptiveConnectivity == null) {
                    snapshottedAdaptiveConnectivity = runCatching {
                        Settings.Secure.getInt(
                            context.contentResolver,
                            "adaptive_connectivity_enabled",
                            1,
                        )
                    }.getOrDefault(1)
                }
                if (snapshottedNetworkRecommendations == null) {
                    snapshottedNetworkRecommendations = Settings.Global.getString(
                        context.contentResolver,
                        "network_recommendations_enabled",
                    ) ?: "UNSET"
                }
                if (snapshottedNetworkAvoidBadWifi == null) {
                    snapshottedNetworkAvoidBadWifi = Settings.Global.getString(
                        context.contentResolver,
                        "network_avoid_bad_wifi",
                    ) ?: "UNSET"
                }
                wifiCmds.add("settings put secure adaptive_connectivity_enabled 0")
                wifiCmds.add("settings put global network_recommendations_enabled 0")
                wifiCmds.add("settings put global network_avoid_bad_wifi 0")
                wifiCmds.add("cmd wifi set-network-selection-config enabled enabled -a 0")
                didLockWifiNetworkSelection = true
            }

            // 9d. Prevent ARP disconnects
            if (profile.wifiPreventArp) {
                wifiCmds.add("cmd wifi set-ipreach-disconnect disabled")
                didDisableIpReachDisconnect = true
            }

            if (wifiCmds.isNotEmpty()) {
                ShellUtils.runCommand(context, wifiCmds.joinToString(" && "))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed Wi-Fi stabilization", e)
        }

        // 10. Disable Private DNS (DoT)
        if (profile.disablePrivateDns) {
            try {
                val currentMode = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_mode",
                )
                val currentSpecifier = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_specifier",
                )

                if (!currentMode.isNullOrEmpty() && currentMode != "off") {
                    snapshottedPrivateDnsMode = currentMode
                    snapshottedPrivateDnsSpecifier = currentSpecifier
                    didDisablePrivateDns = true

                    runCatching {
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", "off")
                    }
                    ShellUtils.runCommand(context, "settings put global private_dns_mode off")
                    Log.d(TAG, "Disabled Private DNS (was mode=$currentMode, specifier=$currentSpecifier)")
                } else if (!currentSpecifier.isNullOrEmpty()) {
                    snapshottedPrivateDnsMode = "hostname"
                    snapshottedPrivateDnsSpecifier = currentSpecifier
                    didDisablePrivateDns = true

                    runCatching {
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", "off")
                    }
                    ShellUtils.runCommand(context, "settings put global private_dns_mode off")
                    Log.d(TAG, "Disabled Private DNS (was hostname with specifier=$currentSpecifier)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable private DNS", e)
            }
        }

        // 11. Battery Saver Suppress & Continuous Defensive Receiver
        if (profile.blockBatterySaver) {
            try {
                notifiedBattery20 = false
                notifiedBattery10 = false
                notifiedBattery5 = false

                if (snapshottedLowPowerTriggerLevel == null) {
                    snapshottedLowPowerTriggerLevel = Settings.Global.getInt(
                        context.contentResolver,
                        "low_power_trigger_level",
                        0,
                    )
                }
                if (snapshottedLowPower == null) {
                    snapshottedLowPower = Settings.Global.getInt(
                        context.contentResolver,
                        "low_power",
                        0,
                    )
                }

                ShellUtils.runCommand(
                    context,
                    "settings put global low_power_trigger_level 0 && settings put global low_power 0 && cmd power set-mode 0",
                )

                if (batterySaverReceiver == null) {
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_BATTERY_CHANGED)
                        addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                    }
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(c: Context?, intent: Intent?) {
                            if (!isGameModeActive) return
                            when (intent?.action) {
                                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                                    if (pm?.isPowerSaveMode == true) {
                                        ShellUtils.runCommand(
                                            context,
                                            "settings put global low_power 0 && cmd power set-mode 0",
                                        )
                                    }
                                }
                                Intent.ACTION_BATTERY_CHANGED -> {
                                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                        status == BatteryManager.BATTERY_STATUS_FULL
                                    if (isCharging) {
                                        notifiedBattery20 = false
                                        notifiedBattery10 = false
                                        notifiedBattery5 = false
                                    } else if (level >= 0 && scale > 0) {
                                        val pct = (level * 100) / scale
                                        if (pct > 20) {
                                            notifiedBattery20 = false
                                            notifiedBattery10 = false
                                            notifiedBattery5 = false
                                        } else if (pct in 11..20 && !notifiedBattery20) {
                                            notifiedBattery20 = true
                                            showToast(context, context.getString(R.string.game_mode_battery_low_20))
                                        } else if (pct in 6..10 && !notifiedBattery10) {
                                            notifiedBattery10 = true
                                            showToast(context, context.getString(R.string.game_mode_battery_low_10))
                                        } else if (pct in 1..5 && !notifiedBattery5) {
                                            notifiedBattery5 = true
                                            showToast(context, context.getString(R.string.game_mode_battery_low_5))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ContextCompat.registerReceiver(
                        context.applicationContext,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    batterySaverReceiver = receiver
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to suppress battery saver", e)
            }
        }

        // 12. Pause Master Auto-Sync
        if (profile.pauseAutoSync) {
            try {
                val sync = ContentResolver.getMasterSyncAutomatically()
                if (snapshottedAutoSync == null) snapshottedAutoSync = sync
                if (sync) ContentResolver.setMasterSyncAutomatically(false)
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException pausing auto-sync", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed auto-sync pause", e)
            }
        }
    }

    fun restoreNetworkStabilization(context: Context) {
        wifiHighPerfLock?.let {
            try {
                if (it.isHeld) it.release()
            } catch (_: Exception) { }
        }
        wifiHighPerfLock = null

        wifiLowLatencyLock?.let {
            try {
                if (it.isHeld) it.release()
            } catch (_: Exception) { }
        }
        wifiLowLatencyLock = null

        snapshottedWifiScanAlwaysEnabled?.let { prevScan ->
            ShellUtils.runCommand(context, "settings put global wifi_scan_always_enabled $prevScan && cmd wifi set-scan-always-available enabled")
            snapshottedWifiScanAlwaysEnabled = null
        }

        if (didLockWifiNetworkSelection) {
            ShellUtils.runCommand(context, "cmd wifi set-network-selection-config enabled enabled -a 0")
            didLockWifiNetworkSelection = false
        }

        snapshottedWifiWakeup?.let { prevWakeup ->
            ShellUtils.runCommand(context, "settings put global wifi_wakeup_enabled $prevWakeup")
            snapshottedWifiWakeup = null
        }

        snapshottedWifiNotification?.let { prevNotif ->
            ShellUtils.runCommand(context, "settings put global wifi_networks_available_notification_on $prevNotif")
            snapshottedWifiNotification = null
        }

        snapshottedWifiPowerSave?.let { prevPowerSave ->
            val cmd = if (prevPowerSave == "UNSET") {
                "settings delete global wifi_power_save"
            } else {
                "settings put global wifi_power_save $prevPowerSave"
            }
            ShellUtils.runCommand(context, cmd)
            snapshottedWifiPowerSave = null
        }

        if (didDisableIpReachDisconnect) {
            ShellUtils.runCommand(context, "cmd wifi set-ipreach-disconnect enabled")
            didDisableIpReachDisconnect = false
        }

        snapshottedBleScanAlwaysEnabled?.let { prevBle ->
            ShellUtils.runCommand(context, "settings put global ble_scan_always_enabled $prevBle")
            snapshottedBleScanAlwaysEnabled = null
        }

        snapshottedAdaptiveConnectivity?.let { prevAdaptive ->
            ShellUtils.runCommand(context, "settings put secure adaptive_connectivity_enabled $prevAdaptive")
            snapshottedAdaptiveConnectivity = null
        }

        snapshottedNetworkRecommendations?.let { prev ->
            val cmd = if (prev == "UNSET") {
                "settings delete global network_recommendations_enabled"
            } else {
                "settings put global network_recommendations_enabled $prev"
            }
            ShellUtils.runCommand(context, cmd)
            snapshottedNetworkRecommendations = null
        }

        snapshottedNetworkAvoidBadWifi?.let { prev ->
            val cmd = if (prev == "UNSET") {
                "settings delete global network_avoid_bad_wifi"
            } else {
                "settings put global network_avoid_bad_wifi $prev"
            }
            ShellUtils.runCommand(context, cmd)
            snapshottedNetworkAvoidBadWifi = null
        }

        try {
            val prevMode = snapshottedPrivateDnsMode
            val prevSpecifier = snapshottedPrivateDnsSpecifier
            val shouldRestore = didDisablePrivateDns || !prevSpecifier.isNullOrEmpty()

            val targetMode = if (!prevMode.isNullOrEmpty() && prevMode != "off") {
                prevMode
            } else if (!prevSpecifier.isNullOrEmpty()) {
                "hostname"
            } else null

            if (shouldRestore && targetMode != null) {
                Log.d(TAG, "Restoring Private DNS to mode=$targetMode, specifier=$prevSpecifier")
                runCatching {
                    if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                        Settings.Global.putString(context.contentResolver, "private_dns_specifier", prevSpecifier)
                    }
                    Settings.Global.putString(context.contentResolver, "private_dns_mode", targetMode)
                }
                val shellCmd = if (targetMode == "hostname" && !prevSpecifier.isNullOrEmpty()) {
                    "settings put global private_dns_specifier $prevSpecifier && settings put global private_dns_mode $targetMode"
                } else {
                    "settings put global private_dns_mode $targetMode"
                }
                ShellUtils.runCommand(context, shellCmd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore Private DNS", e)
        } finally {
            snapshottedPrivateDnsMode = null
            snapshottedPrivateDnsSpecifier = null
            didDisablePrivateDns = false
        }

        batterySaverReceiver?.let {
            runCatching { context.applicationContext.unregisterReceiver(it) }
        }
        batterySaverReceiver = null
        notifiedBattery20 = false
        notifiedBattery10 = false
        notifiedBattery5 = false

        snapshottedLowPowerTriggerLevel?.let { prevTrigger ->
            ShellUtils.runCommand(context, "settings put global low_power_trigger_level $prevTrigger")
            snapshottedLowPowerTriggerLevel = null
        }

        snapshottedLowPower?.let { prevLowPower ->
            if (prevLowPower == 1) {
                ShellUtils.runCommand(context, "settings put global low_power 1 && cmd power set-mode 1")
            }
            snapshottedLowPower = null
        }

        snapshottedAutoSync?.let { wasActive ->
            if (wasActive) {
                try {
                    ContentResolver.setMasterSyncAutomatically(true)
                    Log.d(TAG, "Master auto-sync restored")
                } catch (e: SecurityException) {
                    Log.w(TAG, "SecurityException restoring auto-sync", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore auto-sync", e)
                }
            }
            snapshottedAutoSync = null
        }
    }
}
