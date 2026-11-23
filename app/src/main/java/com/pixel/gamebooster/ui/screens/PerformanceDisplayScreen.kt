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
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
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
fun PerformanceDisplayScreen(
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

    val globalLockRefresh by repo.isLockRefreshRate.collectAsState()
    val globalBoostRam by repo.isBoostRam.collectAsState()
    val globalCachedFreezer by repo.isCachedFreezer.collectAsState()
    val globalDisableBrightness by repo.isDisableAutoBrightness.collectAsState()

    val lockRefresh = currentProfile?.lockRefreshRate ?: globalLockRefresh
    val boostRam = currentProfile?.boostRam ?: globalBoostRam
    val cachedFreezer = currentProfile?.cachedFreezer ?: globalCachedFreezer
    val disableBrightness = currentProfile?.disableAutoBrightness ?: globalDisableBrightness

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
        // Gestural Pull Title Header
        PullTitleHeader(
            title = stringResource(R.string.perf_header_title),
            state = headerState,
        )

        if (adaptive.isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Column 1: Display Refresh
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DisplayRefreshSection(
                        lockRefresh = lockRefresh,
                        disableBrightness = disableBrightness,
                        onLockRefreshChange = {
                            if (isScoped) updateScoped { p -> p.copy(lockRefreshRate = it) }
                            else repo.setLockRefreshRate(it)
                        },
                        onDisableBrightnessChange = {
                            if (isScoped) updateScoped { p -> p.copy(disableAutoBrightness = it) }
                            else repo.setDisableAutoBrightness(it)
                        },
                    )
                }

                // Column 2: Memory & Kernel
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MemoryKernelSection(
                        boostRam = boostRam,
                        cachedFreezer = cachedFreezer,
                        onBoostRamChange = {
                            if (isScoped) updateScoped { p -> p.copy(boostRam = it) }
                            else repo.setBoostRam(it)
                        },
                        onCachedFreezerChange = {
                            if (isScoped) updateScoped { p -> p.copy(cachedFreezer = it) }
                            else repo.setCachedFreezer(it)
                        },
                    )
                }
            }
        } else {
            // Single-column portrait layout
            DisplayRefreshSection(
                lockRefresh = lockRefresh,
                disableBrightness = disableBrightness,
                onLockRefreshChange = {
                    if (isScoped) updateScoped { p -> p.copy(lockRefreshRate = it) }
                    else repo.setLockRefreshRate(it)
                },
                onDisableBrightnessChange = {
                    if (isScoped) updateScoped { p -> p.copy(disableAutoBrightness = it) }
                    else repo.setDisableAutoBrightness(it)
                },
            )
            MemoryKernelSection(
                boostRam = boostRam,
                cachedFreezer = cachedFreezer,
                onBoostRamChange = {
                    if (isScoped) updateScoped { p -> p.copy(boostRam = it) }
                    else repo.setBoostRam(it)
                },
                onCachedFreezerChange = {
                    if (isScoped) updateScoped { p -> p.copy(cachedFreezer = it) }
                    else repo.setCachedFreezer(it)
                },
            )
        }
    }
}

@Composable
private fun DisplayRefreshSection(
    lockRefresh: Boolean,
    disableBrightness: Boolean,
    onLockRefreshChange: (Boolean) -> Unit,
    onDisableBrightnessChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.perf_section_display_refresh),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.perf_force_120hz_title),
            subtitle = stringResource(R.string.perf_force_120hz_desc),
            command = stringResource(R.string.perf_force_120hz_cmd),
            icon = Icons.Rounded.Speed,
            isChecked = lockRefresh,
            onCheckedChange = onLockRefreshChange,
            index = 0,
            count = 2,
        )
        IconToggleItem(
            title = stringResource(R.string.perf_disable_auto_brightness_title),
            subtitle = stringResource(R.string.perf_disable_auto_brightness_desc),
            command = stringResource(R.string.perf_disable_auto_brightness_cmd),
            icon = Icons.Rounded.BrightnessAuto,
            isChecked = disableBrightness,
            onCheckedChange = onDisableBrightnessChange,
            index = 1,
            count = 2,
        )
    }
}

@Composable
private fun MemoryKernelSection(
    boostRam: Boolean,
    cachedFreezer: Boolean,
    onBoostRamChange: (Boolean) -> Unit,
    onCachedFreezerChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.perf_section_memory_kernel),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.perf_purge_ram_title),
            subtitle = stringResource(R.string.perf_purge_ram_desc),
            command = stringResource(R.string.perf_purge_ram_cmd),
            icon = Icons.Rounded.CleaningServices,
            isChecked = boostRam,
            onCheckedChange = onBoostRamChange,
            index = 0,
            count = 2,
        )
        IconToggleItem(
            title = stringResource(R.string.perf_cached_freezer_title),
            subtitle = stringResource(R.string.perf_cached_freezer_desc),
            command = stringResource(R.string.perf_cached_freezer_cmd),
            icon = Icons.Rounded.Memory,
            isChecked = cachedFreezer,
            onCheckedChange = onCachedFreezerChange,
            index = 1,
            count = 2,
        )
    }
}
