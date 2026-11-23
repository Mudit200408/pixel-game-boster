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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker

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

    val headerState = rememberPullTitleHeaderState()

    var selectedProfile by remember(
        lowLatency,
        suppressScans,
        lockSufficiency,
        preventArp,
        disablePrivateDns,
        pauseSync,
        blockBatterySaver,
    ) {
        mutableStateOf(
            if (lowLatency && suppressScans && lockSufficiency && preventArp && disablePrivateDns && pauseSync && blockBatterySaver) {
                "Competitive"
            } else {
                "Balanced"
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(headerState.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gestural Pull Title Header (wraps to 2 lines without marquee)
        PullTitleHeader(
            title = "Network & Sync",
            caption = "Wi-Fi low latency, scan suppression & auto-sync",
            state = headerState,
        )

        // Section: Optimization Quick Profile Picker
        Text(
            text = "Optimization Profile",
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
                    text = "Preset Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Select all-out competitive latency tuning or balanced gaming",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SegmentedPicker(
                    items = listOf("Competitive", "Balanced"),
                    selectedItem = selectedProfile,
                    onItemSelected = { profile ->
                        selectedProfile = profile
                        if (profile == "Competitive") {
                            repo.setWifiLowLatency(true)
                            repo.setWifiSuppressScans(true)
                            repo.setWifiLockSufficiency(true)
                            repo.setWifiPreventArp(true)
                            repo.setDisablePrivateDns(true)
                            repo.setPauseAutoSync(true)
                            repo.setBlockBatterySaver(true)
                        } else {
                            repo.setWifiLowLatency(true)
                            repo.setWifiSuppressScans(true)
                            repo.setWifiLockSufficiency(false)
                            repo.setWifiPreventArp(false)
                            repo.setDisablePrivateDns(false)
                            repo.setPauseAutoSync(false)
                            repo.setBlockBatterySaver(false)
                        }
                    },
                    labelProvider = {
                        if (it == "Competitive") "Competitive" else "Balanced"
                    },
                )
            }
        }

        // Section: Wi-Fi & Latency Optimization
        Text(
            text = "Wi-Fi & Latency Optimization",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Wi-Fi Low Latency Lock",
                subtitle = "Acquires high-perf Wi-Fi lock and disables Wi-Fi power save throttling.",
                command = "WifiLock(LOW_LATENCY) && settings put global wifi_power_save 0",
                icon = Icons.Rounded.Wifi,
                isChecked = lowLatency,
                onCheckedChange = { repo.setWifiLowLatency(it) },
                index = 0,
                count = 5,
            )
            IconToggleItem(
                title = "Suppress Network Scans",
                subtitle = "Stops background Wi-Fi and Bluetooth beacon scans causing latency spikes.",
                command = "settings put global wifi_scan_always_enabled 0 && ble_scan_always_enabled 0",
                icon = Icons.Rounded.Radar,
                isChecked = suppressScans,
                onCheckedChange = { repo.setWifiSuppressScans(it) },
                index = 1,
                count = 5,
            )
            IconToggleItem(
                title = "Lock Network Sufficiency",
                subtitle = "Disables aggressive Wi-Fi hopping and adaptive connectivity handovers.",
                command = "settings put secure adaptive_connectivity_enabled 0 && cmd wifi set-network-selection-config enabled enabled -a 0",
                icon = Icons.Rounded.NetworkCheck,
                isChecked = lockSufficiency,
                onCheckedChange = { repo.setWifiLockSufficiency(it) },
                index = 2,
                count = 5,
            )
            IconToggleItem(
                title = "Prevent ARP Disconnects",
                subtitle = "Blocks dropping Wi-Fi connection on transient router ARP probe misses.",
                command = "cmd wifi set-ipreach-disconnect disabled",
                icon = Icons.Rounded.WifiTetheringOff,
                isChecked = preventArp,
                onCheckedChange = { repo.setWifiPreventArp(it) },
                index = 3,
                count = 5,
            )
            IconToggleItem(
                title = "Private DNS Latency Bypass",
                subtitle = "Disables Private DNS (DoT) during gaming to avoid DNS query latency spikes.",
                command = "settings put global private_dns_mode off",
                icon = Icons.Rounded.Dns,
                isChecked = disablePrivateDns,
                onCheckedChange = { repo.setDisablePrivateDns(it) },
                index = 4,
                count = 5,
            )
        }

        // Section: Background Sync & Power
        Text(
            text = "Background Sync & Power",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Pause Master Auto-Sync",
                subtitle = "Halts cloud account synchronization to prevent background disk and network I/O.",
                command = "ContentResolver.setMasterSyncAutomatically(false)",
                icon = Icons.Rounded.SyncDisabled,
                isChecked = pauseSync,
                onCheckedChange = { repo.setPauseAutoSync(it) },
                index = 0,
                count = 2,
            )
            IconToggleItem(
                title = "Suppress Battery Saver",
                subtitle = "Bypasses system CPU throttling and preserves maximum frame stability.",
                command = "settings put global low_power 0 && cmd power set-mode 0",
                icon = Icons.Rounded.BatteryAlert,
                isChecked = blockBatterySaver,
                onCheckedChange = { repo.setBlockBatterySaver(it) },
                index = 1,
                count = 2,
            )
        }
    }
}
