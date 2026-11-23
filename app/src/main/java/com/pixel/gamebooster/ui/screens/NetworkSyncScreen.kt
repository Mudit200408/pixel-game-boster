package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTetheringOff
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
fun NetworkSyncScreen(
    repo: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val lowLatency by repo.isWifiLowLatency.collectAsState()
    val suppressScans by repo.isWifiSuppressScans.collectAsState()
    val lockSufficiency by repo.isWifiLockSufficiency.collectAsState()
    val preventArp by repo.isWifiPreventArp.collectAsState()
    val disablePrivateDns by repo.isDisablePrivateDns.collectAsState()
    val pauseSync by repo.isPauseAutoSync.collectAsState()
    val blockBatterySaver by repo.isBlockBatterySaver.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section Header Lottie
        LottieHeader(resId = R.raw.lottie_section_network)

        Text(
            text = "Wi-Fi & Latency Optimization",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Wi-Fi Low Latency Lock",
                description = "Acquires high-perf Wi-Fi lock and disables Wi-Fi power save throttling.",
                command = "WifiLock(LOW_LATENCY) && settings put global wifi_power_save 0",
                icon = Icons.Rounded.Wifi,
                isChecked = lowLatency,
                onCheckedChange = { repo.setWifiLowLatency(it) },
            )
            IconToggleItem(
                title = "Suppress Network Scans",
                description = "Stops background Wi-Fi and Bluetooth beacon scans causing latency spikes.",
                command = "settings put global wifi_scan_always_enabled 0 && ble_scan_always_enabled 0",
                icon = Icons.Rounded.Radar,
                isChecked = suppressScans,
                onCheckedChange = { repo.setWifiSuppressScans(it) },
            )
            IconToggleItem(
                title = "Lock Network Sufficiency",
                description = "Disables aggressive Wi-Fi hopping and adaptive connectivity handovers.",
                command = "settings put secure adaptive_connectivity_enabled 0 && cmd wifi set-network-selection-config enabled enabled -a 0",
                icon = Icons.Rounded.NetworkCheck,
                isChecked = lockSufficiency,
                onCheckedChange = { repo.setWifiLockSufficiency(it) },
            )
            IconToggleItem(
                title = "Prevent ARP Disconnects",
                description = "Blocks dropping Wi-Fi connection on transient router ARP probe misses.",
                command = "cmd wifi set-ipreach-disconnect disabled",
                icon = Icons.Rounded.WifiTetheringOff,
                isChecked = preventArp,
                onCheckedChange = { repo.setWifiPreventArp(it) },
            )
            IconToggleItem(
                title = "Private DNS Latency Bypass",
                description = "Disables Private DNS (DoT) during gaming to avoid DNS query latency spikes.",
                command = "settings put global private_dns_mode off",
                icon = Icons.Rounded.Dns,
                isChecked = disablePrivateDns,
                onCheckedChange = { repo.setDisablePrivateDns(it) },
            )
        }

        Text(
            text = "Background Sync & Power",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Pause Master Auto-Sync",
                description = "Halts cloud account synchronization to prevent background disk and network I/O.",
                command = "ContentResolver.setMasterSyncAutomatically(false)",
                icon = Icons.Rounded.SyncDisabled,
                isChecked = pauseSync,
                onCheckedChange = { repo.setPauseAutoSync(it) },
            )
            IconToggleItem(
                title = "Suppress Battery Saver",
                description = "Bypasses system CPU throttling and preserves maximum frame stability.",
                command = "settings put global low_power 0 && cmd power set-mode 0",
                icon = Icons.Rounded.BatteryAlert,
                isChecked = blockBatterySaver,
                onCheckedChange = { repo.setBlockBatterySaver(it) },
            )
        }
    }
}
