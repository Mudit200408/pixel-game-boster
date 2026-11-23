package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.components.LottieHeader
import com.pixel.gamebooster.ui.components.PermissionHealthBanner
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.ui.core.sliders.ConfigSliderItem
import com.pixel.gamebooster.ui.theme.ModeBatteryColor
import com.pixel.gamebooster.ui.theme.ModePerformanceColor
import com.pixel.gamebooster.ui.theme.ModeStandardColor
import com.pixel.gamebooster.utils.GameModeManager
import com.pixel.gamebooster.utils.PermissionHelper

@Composable
fun DashboardScreen(
    repo: SettingsRepository,
    onAddGameClick: () -> Unit = {},
    onFixPermissionsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isEnabled by repo.isBoosterEnabled.collectAsState()
    val mode by repo.interventionMode.collectAsState()
    val delay by repo.restoreDelay.collectAsState()

    val lottieRes = when (mode) {
        2 -> R.raw.lottie_mode_performance
        1 -> R.raw.lottie_mode_standard
        3 -> R.raw.lottie_mode_battery
        else -> R.raw.lottie_mode_performance
    }

    val activeColor = when (mode) {
        2 -> ModePerformanceColor
        1 -> ModeStandardColor
        3 -> ModeBatteryColor
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Permission Health Banner (visible if Shizuku or Accessibility is revoked)
        PermissionHealthBanner(onFixPermissionsClick = onFixPermissionsClick)

        // Dynamic Lottie Header
        LottieHeader(resId = lottieRes)

        // Master Toggle Card
        RoundedCardContainer {
            IconToggleItem(
                title = "Pixel Game Booster",
                description = "Automates device performance optimizations when games launch",
                command = "AppDetectionService & GameModeManager",
                isChecked = isEnabled,
                onCheckedChange = { enabled ->
                    repo.setBoosterEnabled(enabled)
                    if (enabled) {
                        if (PermissionHelper.hasUsageStatsPermission(context)) {
                            AppDetectionService.start(context)
                        }
                    } else {
                        AppDetectionService.stop(context)
                        if (GameModeManager.isGameModeActive) {
                            GameModeManager.onGameBackground(context, GameModeManager.activeGamePackage, null, repo)
                        }
                    }
                },
            )
        }

        // My Games Launcher Grid
        com.pixel.gamebooster.ui.components.GameLauncherGrid(
            repo = repo,
            onAddGameClick = onAddGameClick,
        )

        // Section: Profile
        Text(
            text = "Performance Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Game Manager Intervention",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Sets Android Game Manager profile and target parameters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedPicker(
                    items = listOf(2, 1, 3),
                    selectedItem = mode,
                    onItemSelected = { repo.setInterventionMode(it) },
                    labelProvider = {
                        when (it) {
                            2 -> "Performance"
                            1 -> "Standard"
                            3 -> "Battery"
                            else -> ""
                        }
                    },
                    activeColor = activeColor,
                )
            }
        }

        // Section: Restoration
        Text(
            text = "Restoration Delay",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            ConfigSliderItem(
                title = "Restoration Delay",
                subtitle = "Buffer time before reverting optimizations on exit",
                value = delay.toFloat(),
                onValueChange = { repo.setRestoreDelay(it.toInt()) },
                valueRange = 0f..120f,
                steps = 23,
                icon = Icons.Rounded.Timer,
                valueFormatter = { if (it.toInt() == 0) "Immediate" else "${it.toInt()}s" },
            )
        }
    }
}
