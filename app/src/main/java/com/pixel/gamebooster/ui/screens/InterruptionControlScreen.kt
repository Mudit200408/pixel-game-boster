package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.utils.GameModeManager
import kotlinx.coroutines.launch

/**
 * UI configuration screen for Game Do Not Disturb and Android native Priority rules.
 */
@Composable
fun InterruptionControlScreen(
    repo: SettingsRepository,
    scopedPackage: String? = null,
    scopedProfile: GameProfile? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentProfileState = if (scopedPackage != null) {
        val initial = scopedProfile ?: repo.createDefaultEffectiveProfile(scopedPackage)
        repo.observeEffectiveProfile(scopedPackage).collectAsState(initial = initial)
    } else null
    val currentProfile = currentProfileState?.value

    val isDndEnabled = (currentProfile?.interruptionMode ?: 1) != 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.interruption_screen_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = stringResource(R.string.interruption_dnd_toggle_title),
                subtitle = stringResource(R.string.interruption_dnd_toggle_desc),
                icon = Icons.Rounded.PriorityHigh,
                isChecked = isDndEnabled,
                isSectional = true,
                onClick = {
                    DNDManager.openGameDashboardPrioritiesSettings(context)
                },
                onCheckedChange = { checked ->
                    if (currentProfile != null && scopedPackage != null) {
                        val updated = currentProfile.copy(
                            customized = true,
                            interruptionMode = if (checked) 1 else 0,
                        )
                        coroutineScope.launch {
                            repo.saveProfile(updated)
                        }

                        if (GameModeManager.isGameModeActive && GameModeManager.activeGamePackage == scopedPackage) {
                            if (checked) {
                                DNDManager.applyInterruptionPolicy(context, true)
                            } else {
                                DNDManager.restoreInterruptionPolicy(context)
                            }
                        }
                    }
                },
                index = 0,
                count = 1,
            )
        }
    }
}
