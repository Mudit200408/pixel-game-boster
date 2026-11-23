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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
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

    val headerState = rememberPullTitleHeaderState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(headerState.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gestural Pull Title Header
        PullTitleHeader(
            title = "Performance & Display",
            caption = "120Hz lock, RAM purge & kernel tuning",
            state = headerState,
        )

        // Section: Display & Refresh Rate
        Text(
            text = "Display & Refresh Rate",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Force 120Hz Physical Display Mode",
                subtitle = "Forces physical panel to 120Hz fixed to eliminate 60Hz/30Hz LTPO drops.",
                command = "cmd display set-user-preferred-display-mode <W> <H> 120 0",
                icon = Icons.Rounded.Speed,
                isChecked = lockRefresh,
                onCheckedChange = { repo.setLockRefreshRate(it) },
                index = 0,
                count = 1,
            )
        }

        // Section: Memory & Kernel Tuning
        Text(
            text = "Memory & Kernel Tuning",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Purge RAM & ZRAM",
                subtitle = "Evicts background cached processes to prevent memory pressure.",
                command = "am kill-all",
                icon = Icons.Rounded.CleaningServices,
                isChecked = boostRam,
                onCheckedChange = { repo.setBoostRam(it) },
                index = 0,
                count = 2,
            )
            IconToggleItem(
                title = "Kernel Cached Apps Freezer",
                subtitle = "Freezes background app cgroups to preserve CPU cycles for game threads.",
                command = "settings put global cached_apps_freezer enabled",
                icon = Icons.Rounded.Memory,
                isChecked = cachedFreezer,
                onCheckedChange = { repo.setCachedFreezer(it) },
                index = 1,
                count = 2,
            )
        }

        // Section: Sensors & Input Latency
        Text(
            text = "Sensors & Input Latency",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Disable Auto-Brightness",
                subtitle = "Locks current brightness to prevent sudden light sensor dips mid-game.",
                command = "settings put system screen_brightness_mode 0",
                icon = Icons.Rounded.BrightnessAuto,
                isChecked = disableBrightness,
                onCheckedChange = { repo.setDisableAutoBrightness(it) },
                index = 0,
                count = 2,
            )
            IconToggleItem(
                title = "Disable Quick Tap",
                subtitle = "Suspends rear-tap sensor monitor to eliminate touch latency and input jank.",
                command = "settings put secure columbus_enabled 0",
                icon = Icons.Rounded.TouchApp,
                isChecked = disableQuickTap,
                onCheckedChange = { repo.setDisableQuickTap(it) },
                index = 1,
                count = 2,
            )
        }
    }
}
