package com.pixel.gamebooster.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppFreezerScreen(
    repo: SettingsRepository,
    onOpenAppSelectionSheet: () -> Unit,
    onOpenServicesSheet: () -> Unit,
    scopedPackage: String? = null,
    scopedProfile: GameProfile? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pm = context.packageManager

    val isScoped = scopedPackage != null
    val currentProfileState = if (scopedPackage != null) {
        val initial = scopedProfile ?: repo.createDefaultEffectiveProfile(scopedPackage)
        repo.observeEffectiveProfile(scopedPackage).collectAsState(initial = initial)
    } else null
    val currentProfile = currentProfileState?.value

    val globalSuspendedApps by repo.suspendedApps.collectAsState()
    val suspendedServices by repo.suspendedServices.collectAsState()
    val globalDisableQuickTap by repo.isDisableQuickTap.collectAsState()

    val suspendedApps = currentProfile?.suspendedApps ?: globalSuspendedApps
    val disableQuickTap = currentProfile?.disableQuickTap ?: globalDisableQuickTap

    val onRemoveApp: (String) -> Unit = { pkg ->
        if (isScoped && currentProfile != null) {
            val updated = currentProfile.copy(
                customized = true,
                suspendedApps = currentProfile.suspendedApps - pkg,
            )
            scope.launch {
                ShellUtils.runCommand(context, "cmd package unsuspend --user 0 $pkg")
                repo.saveProfile(updated)
            }
        } else {
            scope.launch {
                ShellUtils.runCommand(context, "cmd package unsuspend --user 0 $pkg")
                repo.setSuspendedApps(suspendedApps - pkg)
            }
        }
    }

    val onDisableQuickTapChange: (Boolean) -> Unit = { value ->
        if (isScoped && currentProfile != null) {
            scope.launch {
                repo.saveProfile(currentProfile.copy(customized = true, disableQuickTap = value))
            }
        } else {
            repo.setDisableQuickTap(value)
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
            title = stringResource(R.string.freezer_header_title),
            state = headerState,
        )

        if (adaptive.isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Column 1: Configured Apps List & RAM savings metric / warning info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreezerConfiguredAppsSection(
                        suspendedApps = suspendedApps,
                        pm = pm,
                        view = view,
                        onRemoveApp = onRemoveApp,
                    )
                    FreezerWarningNote()
                }

                // Column 2: Process Management Triggers & Quick Tap sensor toggle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FreezerProcessManagementSection(
                        suspendedAppsCount = suspendedApps.size,
                        suspendedServicesCount = suspendedServices.size,
                        onOpenAppSelectionSheet = onOpenAppSelectionSheet,
                        onOpenServicesSheet = onOpenServicesSheet,
                        disableQuickTap = disableQuickTap,
                        onDisableQuickTapChange = onDisableQuickTapChange,
                    )
                }
            }
        } else {
            // Single-column portrait layout
            FreezerConfiguredAppsSection(
                suspendedApps = suspendedApps,
                pm = pm,
                view = view,
                onRemoveApp = onRemoveApp,
            )

            FreezerProcessManagementSection(
                suspendedAppsCount = suspendedApps.size,
                suspendedServicesCount = suspendedServices.size,
                onOpenAppSelectionSheet = onOpenAppSelectionSheet,
                onOpenServicesSheet = onOpenServicesSheet,
                disableQuickTap = disableQuickTap,
                onDisableQuickTapChange = onDisableQuickTapChange,
            )

            // Suspension Disclaimer / Warning Note at end of screen
            FreezerWarningNote()
        }
    }
}

@Composable
private fun FreezerConfiguredAppsSection(
    suspendedApps: Set<String>,
    pm: PackageManager,
    view: View,
    onRemoveApp: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.freezer_section_configured_apps, suspendedApps.size),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    if (suspendedApps.isEmpty()) {
        RoundedCardContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.freezer_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.freezer_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        RoundedCardContainer {
            suspendedApps.toList().forEach { pkg ->
                val appInfo = remember(pkg) {
                    runCatching {
                        val info = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(info).toString()
                        val icon = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                        label to icon
                    }.getOrNull()
                }
                val label = appInfo?.first ?: pkg
                val iconBitmap = appInfo?.second

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = label,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = pkg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onRemoveApp(pkg)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.freezer_cd_unfreeze, label),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezerProcessManagementSection(
    suspendedAppsCount: Int,
    suspendedServicesCount: Int,
    onOpenAppSelectionSheet: () -> Unit,
    onOpenServicesSheet: () -> Unit,
    disableQuickTap: Boolean,
    onDisableQuickTapChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.freezer_section_process_management),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.freezer_user_apps_title),
            subtitle = stringResource(R.string.freezer_user_apps_desc, suspendedAppsCount),
            command = stringResource(R.string.freezer_user_apps_cmd),
            icon = Icons.Rounded.Apps,
            showToggle = false,
            onClick = onOpenAppSelectionSheet,
            trailingContent = {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            index = 0,
            count = 2,
        )
        IconToggleItem(
            title = stringResource(R.string.freezer_google_services_title),
            subtitle = stringResource(R.string.freezer_google_services_desc, suspendedServicesCount),
            command = stringResource(R.string.freezer_google_services_cmd),
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
            index = 1,
            count = 2,
        )
    }

    RoundedCardContainer {
        IconToggleItem(
            title = stringResource(R.string.freezer_disable_quick_tap_title),
            subtitle = stringResource(R.string.freezer_disable_quick_tap_desc),
            command = stringResource(R.string.freezer_disable_quick_tap_cmd),
            icon = Icons.Rounded.TouchApp,
            isChecked = disableQuickTap,
            onCheckedChange = onDisableQuickTapChange,
            index = 0,
            count = 1,
        )
    }
}

@Composable
private fun FreezerWarningNote() {
    val warningDesc = stringResource(R.string.freezer_warning_desc)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp),
        )
        val warningPrefix = stringResource(R.string.freezer_warning_prefix)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(warningPrefix)
                }
                append(warningDesc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
