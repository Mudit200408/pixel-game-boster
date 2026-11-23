package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.components.GameLauncherGrid
import com.pixel.gamebooster.ui.components.PermissionHealthBanner
import com.pixel.gamebooster.ui.components.PixelGamingHeroCard
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
    val mode by repo.interventionMode.collectAsState()
    val delay by repo.restoreDelay.collectAsState()

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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Permission Health Banner (visible if Shizuku or Accessibility is revoked)
        PermissionHealthBanner(onFixPermissionsClick = onFixPermissionsClick)

        // Prominent Master Pixel Game Booster Switch Hero Card
        PixelGamingHeroCard(
            repo = repo,
            onToggleMaster = { enabled ->
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

        // Section: Performance Profile
        Text(
            text = "Performance Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Game Manager Intervention",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Sets Android Game Manager profile and target governor parameters",
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
                    iconProvider = {
                        when (it) {
                            2 -> Icons.Rounded.Speed
                            1 -> Icons.Rounded.Tune
                            3 -> Icons.Rounded.BatteryChargingFull
                            else -> null
                        }
                    },
                    activeColor = activeColor,
                )
            }
        }

        // Section: Restoration Delay
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

        // Section: My Games Launcher Grid (repositioned to bottom per user feedback)
        GameLauncherGrid(
            repo = repo,
            onAddGameClick = onAddGameClick,
        )
    }
}
