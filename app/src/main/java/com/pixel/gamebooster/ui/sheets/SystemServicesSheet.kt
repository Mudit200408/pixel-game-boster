package com.pixel.gamebooster.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer

data class SystemServiceDef(
    val id: String,
    val title: String,
    val description: String,
    val command: String,
)

val SYSTEM_SERVICES_DEFINITIONS = listOf(
    SystemServiceDef(
        id = "com.google.android.aicore",
        title = "Google AICore",
        description = "Suspends on-device Gemini Nano inference worker to free up RAM & NPU.",
        command = "pm disable-user --user 0 com.google.android.aicore",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.turbo",
        title = "Device Health Services (Turbo)",
        description = "Suspends background battery prediction and usage telemetry worker.",
        command = "pm disable-user --user 0 com.google.android.apps.turbo",
    ),
    SystemServiceDef(
        id = "com.google.android.settings.intelligence",
        title = "Settings Intelligence",
        description = "Suspends background settings suggestions and search index indexing.",
        command = "pm disable-user --user 0 com.google.android.settings.intelligence",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.scone",
        title = "Adaptive Connectivity (Scone)",
        description = "Suspends adaptive network state evaluator to eliminate Wi-Fi ping spikes.",
        command = "pm disable-user --user 0 com.google.android.apps.scone",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.carrier.carrierwifi",
        title = "Carrier Wi-Fi",
        description = "Suspends background carrier network auto-discovery.",
        command = "cmd package suspend --user 0 com.google.android.apps.carrier.carrierwifi",
    ),
    SystemServiceDef(
        id = "com.google.android.projection.gearhead",
        title = "Android Auto (Gearhead)",
        description = "Suspends projection services and car connectivity daemon.",
        command = "cmd package suspend --user 0 com.google.android.projection.gearhead",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemServicesSheet(
    repo: SettingsRepository,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val suspendedServices by repo.suspendedServices.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "System Services Suspension",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Select which background Google services are suspended during gameplay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RoundedCardContainer {
                SYSTEM_SERVICES_DEFINITIONS.forEach { service ->
                    val isChecked = suspendedServices.contains(service.id)
                    IconToggleItem(
                        title = service.title,
                        description = service.description,
                        command = service.command,
                        isChecked = isChecked,
                        onCheckedChange = { repo.toggleSuspendedService(service.id) },
                    )
                }
            }
        }
    }
}
