package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.LottieHeader
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer

@Composable
fun PerformanceDisplayScreen(
    repo: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val lockRefresh by repo.isLockRefreshRate.collectAsState()
    val boostRam by repo.isBoostRam.collectAsState()
    val cachedFreezer by repo.isCachedFreezer.collectAsState()
    val disableBrightness by repo.isDisableAutoBrightness.collectAsState()
    val disableQuickTap by repo.isDisableQuickTap.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section Header Lottie
        LottieHeader(resId = R.raw.lottie_section_display)

        Text(
            text = "Display & Refresh Rate",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Force 120Hz Panel Refresh Rate",
                description = "Forces physical panel to 120Hz fixed to eliminate 60Hz/30Hz LTPO drops. Note: does not spoof game engine FPS caps.",
                command = "cmd display set-user-preferred-display-mode <W> <H> 120 0",
                icon = Icons.Rounded.Speed,
                isChecked = lockRefresh,
                onCheckedChange = { repo.setLockRefreshRate(it) },
            )
        }

        Text(
            text = "Memory & Kernel Tuning",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Purge RAM & ZRAM",
                description = "Evicts background cached processes to prevent memory pressure.",
                command = "am kill-all",
                icon = Icons.Rounded.CleaningServices,
                isChecked = boostRam,
                onCheckedChange = { repo.setBoostRam(it) },
            )
            IconToggleItem(
                title = "Kernel Cached Apps Freezer",
                description = "Freezes background app cgroups to preserve CPU cycles for game threads.",
                command = "settings put global cached_apps_freezer enabled",
                icon = Icons.Rounded.Memory,
                isChecked = cachedFreezer,
                onCheckedChange = { repo.setCachedFreezer(it) },
            )
        }

        Text(
            text = "Sensors & Input Latency",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Disable Auto-Brightness",
                description = "Locks current brightness to prevent sudden light sensor dips mid-game.",
                command = "settings put system screen_brightness_mode 0",
                icon = Icons.Rounded.BrightnessAuto,
                isChecked = disableBrightness,
                onCheckedChange = { repo.setDisableAutoBrightness(it) },
            )
            IconToggleItem(
                title = "Disable Quick Tap",
                description = "Suspends rear-tap sensor monitor to eliminate touch latency and input jank.",
                command = "settings put secure columbus_enabled 0",
                icon = Icons.Rounded.TouchApp,
                isChecked = disableQuickTap,
                onCheckedChange = { repo.setDisableQuickTap(it) },
            )
        }
    }
}
