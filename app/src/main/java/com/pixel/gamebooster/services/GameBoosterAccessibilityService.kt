package com.pixel.gamebooster.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.utils.GameDetectionUtils
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.PackageFilterUtils
import com.pixel.gamebooster.utils.PermissionHelper

class GameBoosterAccessibilityService : AccessibilityService() {
    private lateinit var settingsRepository: SettingsRepository
    private var lastPackage: String? = null

    companion object {
        private const val TAG = "PixelGameBooster:A11y"
        var instance: GameBoosterAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        settingsRepository = SettingsRepository(this)
        GameModeManager.init(this)
        if (settingsRepository.isBoosterEnabled.value &&
            PermissionHelper.hasUsageStatsPermission(this) &&
            settingsRepository.hasCompletedOnboarding.value
        ) {
            AppDetectionService.start(this)
        }
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun resetLastPackage() {
        lastPackage = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val isWindowChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED

        if (!isWindowChange) return

        val className = event.className?.toString() ?: ""
        if (className.contains("Toast") || className.contains("TransientNotification")) return

        val packageName = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: windows?.firstOrNull { it.isFocused }?.root?.packageName?.toString()
            ?: return

        val manualGames = settingsRepository.gameApps.value

        if (PackageFilterUtils.isPackageIgnored(packageName, this, manualGames)) {
            Log.d(TAG, "Ignoring transient/overlay window package: $packageName")
            return
        }

        val boosterEnabled = settingsRepository.isBoosterEnabled.value
        val isGame = GameDetectionUtils.isGame(this, packageName, manualGames)
        val isModeActive = GameModeManager.isGameModeActive

        Log.d(TAG, "Window change: pkg=$packageName, boosterEnabled=$boosterEnabled, isGame=$isGame, isModeActive=$isModeActive")

        if (!boosterEnabled) return

        if (packageName == lastPackage) {
            if (!isModeActive && isGame) {
                GameModeManager.onGameForeground(this, packageName, settingsRepository)
            } else if (isModeActive && !isGame) {
                GameModeManager.onGameBackground(
                    this,
                    GameModeManager.activeGamePackage ?: packageName,
                    packageName,
                    settingsRepository,
                )
            }
            return
        }

        val old = lastPackage
        lastPackage = packageName

        if (isGame) {
            GameModeManager.onGameForeground(this, packageName, settingsRepository)
        } else if (old != null && GameDetectionUtils.isGame(this, old, manualGames)) {
            GameModeManager.onGameBackground(this, old, packageName, settingsRepository)
        } else if (isModeActive && !isGame) {
            GameModeManager.onGameBackground(
                this,
                GameModeManager.activeGamePackage ?: old ?: packageName,
                packageName,
                settingsRepository,
            )
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
}
