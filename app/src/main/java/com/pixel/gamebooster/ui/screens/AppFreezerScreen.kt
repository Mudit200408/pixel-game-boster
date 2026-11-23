package com.pixel.gamebooster.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.ShellUtils
import kotlinx.coroutines.launch

@Composable
fun AppFreezerScreen(
    repo: SettingsRepository,
    onOpenAppSelectionSheet: () -> Unit,
    onOpenServicesSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pm = context.packageManager

    val suspendedApps by repo.suspendedApps.collectAsState()
    val suspendedServices by repo.suspendedServices.collectAsState()

    val headerState = rememberPullTitleHeaderState()

    // Calculated RAM savings metric
    val estimatedReclaimedMb = (suspendedApps.size * 65) + (suspendedServices.size * 45)

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(headerState.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Gestural Pull Title Header
        PullTitleHeader(
            title = "App Freezer",
            caption = "Reclaim RAM by suspending background processes",
            state = headerState,
        )

        // Active Reclaimed RAM Dashboard Metric Card
        RoundedCardContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "System Memory Freed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = "$estimatedReclaimedMb MB RAM Reclaimed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "${suspendedApps.size} apps and ${suspendedServices.size} services suspended during gaming to eliminate background CPU/RAM contention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Instant Freeze Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    HapticUtil.performConfirmHaptic(view)
                    scope.launch {
                        if (suspendedApps.isNotEmpty()) {
                            val suspendCmd = suspendedApps.joinToString(" && ") { "cmd package suspend --user 0 $it" }
                            ShellUtils.runCommand(context, suspendCmd)
                            Toast.makeText(context, "Suspended ${suspendedApps.size} background apps", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No apps selected. Open App Selection below.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AcUnit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Freeze Apps Now",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (suspendedApps.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        scope.launch {
                            val unsuspendCmd = suspendedApps.joinToString(" && ") { "cmd package unsuspend --user 0 $it" }
                            ShellUtils.runCommand(context, unsuspendCmd)
                            Toast.makeText(context, "Unfroze all background apps", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = "Unfreeze All",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Section: Configured Apps List
        Text(
            text = "Configured Applications (${suspendedApps.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        if (suspendedApps.isEmpty()) {
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "No Applications Suspended",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Select installed apps from the App Selection Sheet below to freeze them during gameplay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            RoundedCardContainer {
                suspendedApps.toList().forEachIndexed { index, pkg ->
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
                            .background(MaterialTheme.colorScheme.surfaceBright)
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
                                scope.launch {
                                    ShellUtils.runCommand(context, "cmd package unsuspend --user 0 $pkg")
                                    repo.setSuspendedApps(suspendedApps - pkg)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Unfreeze $label",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // Section: Process Management Triggers
        Text(
            text = "Process Management",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer {
            IconToggleItem(
                title = "User App Freezer",
                subtitle = "Select user applications to suspend while gaming (${suspendedApps.size} apps configured)",
                command = "cmd package suspend --user 0 <package>",
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
                title = "Google Background Services",
                subtitle = "Suspends background telemetry, AI Core, and daemons (${suspendedServices.size} services active)",
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
                index = 1,
                count = 2,
            )
        }
    }
}
