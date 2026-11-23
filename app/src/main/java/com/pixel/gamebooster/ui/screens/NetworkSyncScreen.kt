package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import kotlinx.coroutines.launch

@Composable
fun NetworkSyncScreen(
    repo: SettingsRepository,
    scopedPackage: String? = null,
    scopedProfile: GameProfile? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val isScoped = scopedPackage != null

    val currentProfileState = if (scopedPackage != null) {
        val initial = scopedProfile ?: repo.createDefaultEffectiveProfile(scopedPackage)
        repo.observeEffectiveProfile(scopedPackage).collectAsState(initial = initial)
    } else null
    val currentProfile = currentProfileState?.value

    val globalLowLatency by repo.isWifiLowLatency.collectAsState()
    val globalSuppressScans by repo.isWifiSuppressScans.collectAsState()
    val globalLockSufficiency by repo.isWifiLockSufficiency.collectAsState()
    val globalPreventArp by repo.isWifiPreventArp.collectAsState()
    val globalDisablePrivateDns by repo.isDisablePrivateDns.collectAsState()
    val globalPauseSync by repo.isPauseAutoSync.collectAsState()
    val globalBlockBatterySaver by repo.isBlockBatterySaver.collectAsState()

    val lowLatency = currentProfile?.wifiLowLatency ?: globalLowLatency
    val suppressScans = currentProfile?.wifiSuppressScans ?: globalSuppressScans
    val lockSufficiency = currentProfile?.wifiLockSufficiency ?: globalLockSufficiency
    val preventArp = currentProfile?.wifiPreventArp ?: globalPreventArp
    val disablePrivateDns = currentProfile?.disablePrivateDns ?: globalDisablePrivateDns
    val pauseSync = currentProfile?.pauseAutoSync ?: globalPauseSync
    val blockBatterySaver = currentProfile?.blockBatterySaver ?: globalBlockBatterySaver

    fun updateScoped(transform: (GameProfile) -> GameProfile) {
        if (isScoped && currentProfile != null) {
            coroutineScope.launch {
                repo.saveProfile(transform(currentProfile).copy(customized = true))
            }
        }
    }

    val headerState = rememberPullTitleHeaderState()
    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 68.dp else 96.dp

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
                // Column 1: Wi-Fi Latency & Packet Loss Optimization cards
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
                        onLowLatencyChange = {
                            if (isScoped) updateScoped { p -> p.copy(wifiLowLatency = it) }
                            else repo.setWifiLowLatency(it)
                        },
                        onSuppressScansChange = {
                            if (isScoped) updateScoped { p -> p.copy(wifiSuppressScans = it) }
                            else repo.setWifiSuppressScans(it)
                        },
                        onLockSufficiencyChange = {
                            if (isScoped) updateScoped { p -> p.copy(wifiLockSufficiency = it) }
                            else repo.setWifiLockSufficiency(it)
                        },
                        onPreventArpChange = {
                            if (isScoped) updateScoped { p -> p.copy(wifiPreventArp = it) }
                            else repo.setWifiPreventArp(it)
                        },
                        onDisablePrivateDnsChange = {
                            if (isScoped) updateScoped { p -> p.copy(disablePrivateDns = it) }
                            else repo.setDisablePrivateDns(it)
                        },
                    )
                }

                // Column 2: Background Sync / Battery Saver cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NetworkBackgroundSyncSection(
                        pauseSync = pauseSync,
                        blockBatterySaver = blockBatterySaver,
                        onPauseSyncChange = {
                            if (isScoped) updateScoped { p -> p.copy(pauseAutoSync = it) }
                            else repo.setPauseAutoSync(it)
                        },
                        onBlockBatterySaverChange = {
                            if (isScoped) updateScoped { p -> p.copy(blockBatterySaver = it) }
                            else repo.setBlockBatterySaver(it)
                        },
                    )
                }
            }
        } else {
            // Single-column portrait layout
            NetworkWifiLatencySection(
                lowLatency = lowLatency,
                suppressScans = suppressScans,
                lockSufficiency = lockSufficiency,
                preventArp = preventArp,
                disablePrivateDns = disablePrivateDns,
                onLowLatencyChange = {
                    if (isScoped) updateScoped { p -> p.copy(wifiLowLatency = it) }
                    else repo.setWifiLowLatency(it)
                },
                onSuppressScansChange = {
                    if (isScoped) updateScoped { p -> p.copy(wifiSuppressScans = it) }
                    else repo.setWifiSuppressScans(it)
                },
                onLockSufficiencyChange = {
                    if (isScoped) updateScoped { p -> p.copy(wifiLockSufficiency = it) }
                    else repo.setWifiLockSufficiency(it)
                },
                onPreventArpChange = {
                    if (isScoped) updateScoped { p -> p.copy(wifiPreventArp = it) }
                    else repo.setWifiPreventArp(it)
                },
                onDisablePrivateDnsChange = {
                    if (isScoped) updateScoped { p -> p.copy(disablePrivateDns = it) }
                    else repo.setDisablePrivateDns(it)
                },
            )

            NetworkBackgroundSyncSection(
                pauseSync = pauseSync,
                blockBatterySaver = blockBatterySaver,
                onPauseSyncChange = {
                    if (isScoped) updateScoped { p -> p.copy(pauseAutoSync = it) }
                    else repo.setPauseAutoSync(it)
                },
                onBlockBatterySaverChange = {
                    if (isScoped) updateScoped { p -> p.copy(blockBatterySaver = it) }
                    else repo.setBlockBatterySaver(it)
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
    onLowLatencyChange: (Boolean) -> Unit,
    onSuppressScansChange: (Boolean) -> Unit,
    onLockSufficiencyChange: (Boolean) -> Unit,
    onPreventArpChange: (Boolean) -> Unit,
    onDisablePrivateDnsChange: (Boolean) -> Unit,
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
            onCheckedChange = onLowLatencyChange,
            index = 0,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_suppress_scans_title),
            subtitle = stringResource(R.string.net_suppress_scans_desc),
            command = stringResource(R.string.net_suppress_scans_cmd),
            icon = Icons.Rounded.Radar,
            isChecked = suppressScans,
            onCheckedChange = onSuppressScansChange,
            index = 1,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_lock_sufficiency_title),
            subtitle = stringResource(R.string.net_lock_sufficiency_desc),
            command = stringResource(R.string.net_lock_sufficiency_cmd),
            icon = Icons.Rounded.NetworkCheck,
            isChecked = lockSufficiency,
            onCheckedChange = onLockSufficiencyChange,
            index = 2,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_prevent_arp_title),
            subtitle = stringResource(R.string.net_prevent_arp_desc),
            command = stringResource(R.string.net_prevent_arp_cmd),
            icon = Icons.Rounded.WifiTetheringOff,
            isChecked = preventArp,
            onCheckedChange = onPreventArpChange,
            index = 3,
            count = 5,
        )
        IconToggleItem(
            title = stringResource(R.string.net_disable_private_dns_title),
            subtitle = stringResource(R.string.net_disable_private_dns_desc),
            command = stringResource(R.string.net_disable_private_dns_cmd),
            icon = Icons.Rounded.Dns,
            isChecked = disablePrivateDns,
            onCheckedChange = onDisablePrivateDnsChange,
            index = 4,
            count = 5,
        )
    }
}

@Composable
private fun NetworkBackgroundSyncSection(
    pauseSync: Boolean,
    blockBatterySaver: Boolean,
    onPauseSyncChange: (Boolean) -> Unit,
    onBlockBatterySaverChange: (Boolean) -> Unit,
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
            onCheckedChange = onPauseSyncChange,
            index = 0,
            count = 2,
        )
        IconToggleItem(
            title = stringResource(R.string.net_block_battery_saver_title),
            subtitle = stringResource(R.string.net_block_battery_saver_desc),
            command = stringResource(R.string.net_block_battery_saver_cmd),
            icon = Icons.Rounded.BatteryAlert,
            isChecked = blockBatterySaver,
            onCheckedChange = onBlockBatterySaverChange,
            index = 1,
            count = 2,
        )
    }
}
