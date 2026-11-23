package com.pixel.gamebooster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Icon
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
fun AppFreezerScreen(
    repo: SettingsRepository,
    onOpenAppSelectionSheet: () -> Unit,
    onOpenServicesSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val suspendedApps by repo.suspendedApps.collectAsState()
    val suspendedServices by repo.suspendedServices.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section Header Lottie
        LottieHeader(resId = R.raw.lottie_section_freezer)

        Text(
            text = "Background App Freezing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "User App Freezer",
                description = "Suspends chosen user applications while gaming to prevent background CPU/RAM contention. (${suspendedApps.size} apps configured)",
                command = "cmd package suspend --user 0 <package>",
                icon = Icons.Rounded.AcUnit,
                showToggle = false,
                onClick = onOpenAppSelectionSheet,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        Text(
            text = "System Services Suspension",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "Google Background Services",
                description = "Suspends background telemetry, AI Core, and Google daemons during gaming. (${suspendedServices.size} services active)",
                command = "pm disable-user --user 0 / cmd package suspend",
                icon = Icons.Rounded.SettingsSuggest,
                showToggle = false,
                onClick = onOpenServicesSheet,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}
