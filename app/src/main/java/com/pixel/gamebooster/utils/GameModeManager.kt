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
import com.pixel.gamebooster.data.repository.SettingsRepository
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

    private var didDisableAutoBrightness = false
    private var didDisableColumbus = false
    private var didLockDisplayPreferredMode = false
    private var didApplyGameIntervention = false
    private var didLockWifiNetworkSelection = false
    private var didDisableIpReachDisconnect = false
    private var didDisablePrivateDns = false

    private var wifiHighPerfLock: WifiManager.WifiLock? = null
    private var wifiLowLatencyLock: WifiManager.WifiLock? = null

    private var batterySaverReceiver: BroadcastReceiver? = null
    private var notifiedBattery20 = false
    private var notifiedBattery10 = false
    private var notifiedBattery5 = false

    private val scope = CoroutineScope(Dispatchers.IO)
    private var pendingRestoreJob: Job? = null

    fun onGameForeground(context: Context, packageName: String, repo: SettingsRepository) {
        if (!repo.isBoosterEnabled.value) return
        if (!GameDetectionUtils.isGame(context, packageName, repo.gameApps.value)) return

        pendingRestoreJob?.cancel()
        pendingRestoreJob = null

        if (isGameModeActive && activeGamePackage == packageName) return

        activeGamePackage = packageName
        isGameModeActive = true
        Log.d(TAG, "Entering Game Mode for $packageName")

        scope.launch {
            // 1. Auto-Brightness: Fast-path ContentResolver write with shell fallback
            if (repo.isDisableAutoBrightness.value) {
                try {
                    val currentMode = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                    )
                    if (currentMode != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                        snapshottedAutoBrightness = currentMode
                        didDisableAutoBrightness = true

                        val wrote = runCatching {
                            Settings.System.putInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                            )
                        }.getOrDefault(false)
                        if (!wrote) {
                            ShellUtils.runCommand(context, "settings put system screen_brightness_mode 0")
                        }
                        Log.d(TAG, "Disabled Auto Brightness (was $currentMode)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed auto-brightness toggle", e)
                }
            }

            // 2. Quick Tap (Columbus): Fast-path ContentResolver write with shell fallback
            if (repo.isDisableQuickTap.value) {
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
            if (repo.isLockRefreshRate.value) {
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
            if (repo.isBoostRam.value) {
                ShellUtils.runCommand(context, "am kill-all")
            }

            // 5. Kernel Cached Apps Freezer
            if (repo.isCachedFreezer.value) {
                ShellUtils.runCommand(context, "settings put global cached_apps_freezer enabled")
            }

            // 6. Freeze user-selected background apps (Custom app suspension)
            val appsToSuspend = repo.suspendedApps.value
            if (appsToSuspend.isNotEmpty()) {
                val suspendCmd = appsToSuspend.joinToString(" && ") { "cmd package suspend --user 0 $it" }
                ShellUtils.runCommand(context, suspendCmd)
                Log.d(TAG, "Suspended ${appsToSuspend.size} user background apps")
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
                sysCmds.add("cmd package suspend --user 0 com.google.android.apps.carrier.carrierwifi")
            }
            if (servicesToSuspend.contains("com.google.android.projection.gearhead")) {
                sysCmds.add("cmd package suspend --user 0 com.google.android.projection.gearhead")
            }
            if (sysCmds.isNotEmpty()) {
                ShellUtils.runCommand(context, sysCmds.joinToString(" && "))
            }

            // 8. Game Manager intervention
            val mode = repo.interventionMode.value
            val modeName = when (mode) {
                1 -> "standard"
                3 -> "battery"
                else -> "performance"
            }
            val fps = repo.fpsOverride.value
            val gameCmd = if (fps > 0) {
                "cmd game set --downscale disable --fps $fps --mode $mode $packageName && cmd game mode $modeName $packageName"
            } else {
                "cmd game set --downscale disable --mode $mode $packageName && cmd game mode $modeName $packageName"
            }
            ShellUtils.runCommand(context, gameCmd)
            didApplyGameIntervention = true

            // 9. Wi-Fi Latency Stabilization & Dual WifiLocks
            if (repo.isStabilizeWifi.value) {
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val wifiCmds = mutableListOf<String>()

                    // 9a. Low-Latency Wi-Fi lock & power save bypass
                    if (repo.isWifiLowLatency.value) {
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
                    if (repo.isWifiSuppressScans.value) {
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
                    if (repo.isWifiLockSufficiency.value) {
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
                    if (repo.isWifiPreventArp.value) {
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
            if (repo.isDisablePrivateDns.value) {
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
            if (repo.isBlockBatterySaver.value) {
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
            if (repo.isPauseAutoSync.value) {
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

            val appLabel = GameDetectionUtils.getAppLabel(context, packageName)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.game_mode_toast_activated) + " ($appLabel)", Toast.LENGTH_SHORT).show()
            }
        }
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

        pendingRestoreJob?.cancel()
        pendingRestoreJob = scope.launch {
            val delaySeconds = repo.restoreDelay.value.coerceAtLeast(0)
            val delayMs = if (delaySeconds > 0) delaySeconds * 1000L else 300L
            Log.d(TAG, "Game backgrounded. Waiting ${delayMs}ms before restoring settings...")
            delay(delayMs)

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
        GameBoosterAccessibilityService.instance?.resetLastPackage()
        AppDetectionService.instance?.resetLastPackage()

        scope.launch {
            // 1. Reset Game Manager intervention
            if (didApplyGameIntervention) {
                ShellUtils.runCommand(context, "cmd game reset $gamePackage && cmd game mode standard $gamePackage")
                didApplyGameIntervention = false
            }

            // 2. Unsuspend user apps
            val suspended = repo.suspendedApps.value
            if (suspended.isNotEmpty()) {
                val unsuspendCmd = suspended.joinToString(" && ") { "cmd package unsuspend --user 0 $it" }
                ShellUtils.runCommand(context, unsuspendCmd)
            }

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
                restoreSysCmds.add("cmd package unsuspend --user 0 com.google.android.apps.carrier.carrierwifi")
            }
            if (services.contains("com.google.android.projection.gearhead")) {
                restoreSysCmds.add("cmd package unsuspend --user 0 com.google.android.projection.gearhead")
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
            try {
                val prevBrightnessMode = snapshottedAutoBrightness
                if (prevBrightnessMode != null && (didDisableAutoBrightness || prevBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)) {
                    Log.d(TAG, "Restoring Auto Brightness to $prevBrightnessMode")
                    val wrote = runCatching {
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            prevBrightnessMode,
                        )
                    }.getOrDefault(false)
                    if (!wrote) {
                        ShellUtils.runCommand(context, "settings put system screen_brightness_mode $prevBrightnessMode")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore auto brightness", e)
            } finally {
                snapshottedAutoBrightness = null
                didDisableAutoBrightness = false
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
                Toast.makeText(context, context.getString(R.string.game_mode_toast_restored), Toast.LENGTH_SHORT).show()
            }
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
}
