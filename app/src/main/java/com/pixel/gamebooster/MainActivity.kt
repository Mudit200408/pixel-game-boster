package com.pixel.gamebooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.navigation.AppNavigation
import com.pixel.gamebooster.ui.theme.PixelGameBoosterTheme
import com.pixel.gamebooster.utils.PermissionHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repo = remember { SettingsRepository(applicationContext) }
            val isBoosterEnabled by repo.isBoosterEnabled.collectAsState()

            LaunchedEffect(Unit) {
                PermissionHelper.initShizuku {
                    // Shizuku state updated
                }
                DNDManager.getOrCreateGamingZenRuleId(applicationContext)
            }

            LaunchedEffect(isBoosterEnabled) {
                if (repo.hasCompletedOnboarding.value) {
                    if (isBoosterEnabled && PermissionHelper.hasUsageStatsPermission(applicationContext)) {
                        AppDetectionService.start(applicationContext)
                    } else if (!isBoosterEnabled) {
                        AppDetectionService.stop(applicationContext)
                    }
                }
            }

            val themeMode by repo.themeMode.collectAsState()
            val isPitchBlack by repo.isPitchBlack.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            PixelGameBoosterTheme(
                darkTheme = isDark,
                pitchBlackTheme = isPitchBlack,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(repo = repo)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DNDManager.getOrCreateGamingZenRuleId(applicationContext)
    }
}
