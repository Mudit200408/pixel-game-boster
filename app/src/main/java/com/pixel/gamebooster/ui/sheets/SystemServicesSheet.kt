package com.pixel.gamebooster.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo

data class SystemServiceDef(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val command: String,
)

val SYSTEM_SERVICES_DEFINITIONS = listOf(
    SystemServiceDef(
        id = "com.google.android.aicore",
        titleRes = R.string.services_aicore_title,
        descriptionRes = R.string.services_aicore_desc,
        command = "pm disable-user --user 0 com.google.android.aicore",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.turbo",
        titleRes = R.string.services_turbo_title,
        descriptionRes = R.string.services_turbo_desc,
        command = "pm disable-user --user 0 com.google.android.apps.turbo",
    ),
    SystemServiceDef(
        id = "com.google.android.settings.intelligence",
        titleRes = R.string.services_intelligence_title,
        descriptionRes = R.string.services_intelligence_desc,
        command = "pm disable-user --user 0 com.google.android.settings.intelligence",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.scone",
        titleRes = R.string.services_scone_title,
        descriptionRes = R.string.services_scone_desc,
        command = "pm disable-user --user 0 com.google.android.apps.scone",
    ),
    SystemServiceDef(
        id = "com.google.android.apps.carrier.carrierwifi",
        titleRes = R.string.services_carrier_wifi_title,
        descriptionRes = R.string.services_carrier_wifi_desc,
        command = "cmd package suspend --user 0 com.google.android.apps.carrier.carrierwifi",
    ),
    SystemServiceDef(
        id = "com.google.android.projection.gearhead",
        titleRes = R.string.services_gearhead_title,
        descriptionRes = R.string.services_gearhead_desc,
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
    val configuration = LocalConfiguration.current
    val adaptive = rememberAdaptiveLayoutInfo()
    val maxHeight = when {
        adaptive.isCompactHeight -> 260.dp
        adaptive.isTablet -> (configuration.screenHeightDp * 0.70f).dp
        else -> (configuration.screenHeightDp * 0.85f).dp
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = 600.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.services_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.services_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RoundedCardContainer {
                SYSTEM_SERVICES_DEFINITIONS.forEach { service ->
                    val isChecked = suspendedServices.contains(service.id)
                    IconToggleItem(
                        title = stringResource(service.titleRes),
                        description = stringResource(service.descriptionRes),
                        command = service.command,
                        isChecked = isChecked,
                        onCheckedChange = { repo.toggleSuspendedService(service.id) },
                    )
                }
            }
        }
    }
}
