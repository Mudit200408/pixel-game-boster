package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.pixel.gamebooster.utils.GameModeManager
import kotlinx.coroutines.launch

@Composable
fun DisplayScreen(
    repo: SettingsRepository,
    scopedPackage: String? = null,
    scopedProfile: GameProfile? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isScoped = scopedPackage != null

    val currentProfileState = if (scopedPackage != null) {
        val initial = scopedProfile ?: repo.createDefaultEffectiveProfile(scopedPackage)
        repo.observeEffectiveProfile(scopedPackage).collectAsState(initial = initial)
    } else null
    val currentProfile = currentProfileState?.value

    val globalLockRefresh by repo.isLockRefreshRate.collectAsState()
    val globalDisableBrightness by repo.isDisableAutoBrightness.collectAsState()
    val globalLockBackGesture by repo.isLockBackGesture.collectAsState()

    val lockRefresh = currentProfile?.lockRefreshRate ?: globalLockRefresh
    val disableBrightness = currentProfile?.disableAutoBrightness ?: globalDisableBrightness
    val lockBackGesture = currentProfile?.lockBackGesture ?: globalLockBackGesture

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
            title = stringResource(R.string.display_header_title),
            state = headerState,
        )

        // Display & Refresh section
        DisplayRefreshSection(
            lockRefresh = lockRefresh,
            disableBrightness = disableBrightness,
            lockBackGesture = lockBackGesture,
            onLockRefreshChange = {
                if (isScoped) updateScoped { p -> p.copy(lockRefreshRate = it) }
                else repo.setLockRefreshRate(it)
            },
            onDisableBrightnessChange = {
                if (isScoped) updateScoped { p -> p.copy(disableAutoBrightness = it) }
                else repo.setDisableAutoBrightness(it)
            },
            onLockBackGestureChange = {
                if (isScoped) {
                    updateScoped { p -> p.copy(lockBackGesture = it) }
                    if (GameModeManager.isGameModeActive && GameModeManager.activeGamePackage == scopedPackage) {
                        GameModeManager.setBackGestureLock(context, it)
                    }
                } else {
                    repo.setLockBackGesture(it)
                    if (GameModeManager.isGameModeActive) {
                        GameModeManager.setBackGestureLock(context, it)
                    }
                }
            },
        )

        // Notifications / DND section (moved from DashboardScreen)
        if (isScoped) {
            InterruptionControlScreen(
                repo = repo,
                scopedPackage = scopedPackage,
                scopedProfile = currentProfile ?: scopedProfile,
            )
        }
    }
}

@Composable
private fun DisplayRefreshSection(
    lockRefresh: Boolean,
    disableBrightness: Boolean,
    lockBackGesture: Boolean,
    onLockRefreshChange: (Boolean) -> Unit,
    onDisableBrightnessChange: (Boolean) -> Unit,
    onLockBackGestureChange: (Boolean) -> Unit,
) {
    androidx.compose.material3.Text(
        text = stringResource(R.string.perf_section_display_refresh),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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

    androidx.compose.material3.Text(
        text = stringResource(R.string.perf_section_display_navigation),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
      
        IconToggleItem(
            title = stringResource(R.string.perf_lock_back_gesture_title),
            subtitle = stringResource(R.string.perf_lock_back_gesture_desc),
            command = stringResource(R.string.perf_lock_back_gesture_cmd),
            icon = Icons.Rounded.TouchApp,
            isChecked = lockBackGesture,
            onCheckedChange = onLockBackGestureChange,
            index = 0,
            count = 1,
        )
    }
}
