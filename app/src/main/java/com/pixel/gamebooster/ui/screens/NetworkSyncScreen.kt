package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTetheringOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo

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
    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 68.dp else 96.dp

    val competitiveLabel = stringResource(R.string.net_profile_competitive)
    val balancedLabel = stringResource(R.string.net_profile_balanced)

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
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gestural Pull Title Header (wraps to 2 lines, left aligned)
        PullTitleHeader(
            title = stringResource(R.string.net_header_title),
            state = headerState,
        )

        if (adaptive.isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Column 1: Quick Profile SegmentedPicker and Background Sync / Battery Saver cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NetworkProfileSection(
                        selectedProfile = selectedProfile,
                        competitiveLabel = competitiveLabel,
                        balancedLabel = balancedLabel,
                        onProfileSelected = { profile ->
                            selectedProfile = profile
                            applyNetworkProfile(profile, repo)
                        },
                    )

                    NetworkBackgroundSyncSection(
                        pauseSync = pauseSync,
                        blockBatterySaver = blockBatterySaver,
                        repo = repo,
                    )
                }

                // Column 2: Wi-Fi Latency & Packet Loss Optimization cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NetworkWifiLatencySection(
                        lowLatency = lowLatency,
                        suppressScans = suppressScans,
                        lockSufficiency = lockSufficiency,
                        preventArp = preventArp,
                        disablePrivateDns = disablePrivateDns,
                        repo = repo,
                    )
                }
            }
        } else {
            // Single-column portrait layout
            NetworkProfileSection(
                selectedProfile = selectedProfile,
                competitiveLabel = competitiveLabel,
                balancedLabel = balancedLabel,
                onProfileSelected = { profile ->
                    selectedProfile = profile
                    applyNetworkProfile(profile, repo)
                },
            )

            NetworkWifiLatencySection(
                lowLatency = lowLatency,
                suppressScans = suppressScans,
                lockSufficiency = lockSufficiency,
                preventArp = preventArp,
                disablePrivateDns = disablePrivateDns,
                repo = repo,
            )

            NetworkBackgroundSyncSection(
                pauseSync = pauseSync,
                blockBatterySaver = blockBatterySaver,
                repo = repo,
            )
        }
    }
}

private fun applyNetworkProfile(profile: String, repo: SettingsRepository) {
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
}

@Composable
private fun NetworkProfileSection(
    selectedProfile: String,
    competitiveLabel: String,
    balancedLabel: String,
    onProfileSelected: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.net_section_profile),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.net_preset_mode_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.net_preset_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedPicker(
                items = listOf("Competitive", "Balanced"),
                selectedItem = selectedProfile,
                onItemSelected = onProfileSelected,
                labelProvider = {
                    if (it == "Competitive") competitiveLabel else balancedLabel
                },
            )
        }
    }
}

@Composable
private fun NetworkWifiLatencySection(
    lowLatency: Boolean,
    suppressScans: Boolean,
    lockSufficiency: Boolean,
    preventArp: Boolean,
    disablePrivateDns: Boolean,
    repo: SettingsRepository,
) {
    Text(
        text = stringResource(R.string.net_section_wifi_latency),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.net_low_latency_title),
            subtitle = stringResource(R.string.net_low_latency_desc),
            command = stringResource(R.string.net_low_latency_cmd),
            icon = Icons.Rounded.Wifi,
            isChecked = lowLatency,
            onCheckedChange = { repo.setWifiLowLatency(it) },
            index = 0,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_suppress_scans_title),
            subtitle = stringResource(R.string.net_suppress_scans_desc),
            command = stringResource(R.string.net_suppress_scans_cmd),
            icon = Icons.Rounded.Radar,
            isChecked = suppressScans,
            onCheckedChange = { repo.setWifiSuppressScans(it) },
            index = 1,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_lock_sufficiency_title),
            subtitle = stringResource(R.string.net_lock_sufficiency_desc),
            command = stringResource(R.string.net_lock_sufficiency_cmd),
            icon = Icons.Rounded.NetworkCheck,
            isChecked = lockSufficiency,
            onCheckedChange = { repo.setWifiLockSufficiency(it) },
            index = 2,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_prevent_arp_title),
            subtitle = stringResource(R.string.net_prevent_arp_desc),
            command = stringResource(R.string.net_prevent_arp_cmd),
            icon = Icons.Rounded.WifiTetheringOff,
            isChecked = preventArp,
            onCheckedChange = { repo.setWifiPreventArp(it) },
            index = 3,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_disable_private_dns_title),
            subtitle = stringResource(R.string.net_disable_private_dns_desc),
            command = stringResource(R.string.net_disable_private_dns_cmd),
            icon = Icons.Rounded.Dns,
            isChecked = disablePrivateDns,
            onCheckedChange = { repo.setDisablePrivateDns(it) },
            index = 4,
            count = 5,
        )
    }
}

@Composable
private fun NetworkBackgroundSyncSection(
    pauseSync: Boolean,
    blockBatterySaver: Boolean,
    repo: SettingsRepository,
) {
    Text(
        text = stringResource(R.string.net_section_sync_power),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.net_pause_sync_title),
            subtitle = stringResource(R.string.net_pause_sync_desc),
            command = stringResource(R.string.net_pause_sync_cmd),
            icon = Icons.Rounded.SyncDisabled,
            isChecked = pauseSync,
            onCheckedChange = { repo.setPauseAutoSync(it) },
            index = 0,
            count = 2,
        )
        IconToggleItem(
            title = stringResource(R.string.net_block_battery_saver_title),
            subtitle = stringResource(R.string.net_block_battery_saver_desc),
            command = stringResource(R.string.net_block_battery_saver_cmd),
            icon = Icons.Rounded.BatteryAlert,
            isChecked = blockBatterySaver,
            onCheckedChange = { repo.setBlockBatterySaver(it) },
            index = 1,
            count = 2,
        )
    }
}
