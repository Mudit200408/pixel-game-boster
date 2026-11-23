package com.pixel.gamebooster.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.dialogs.ShizukuPairingDialog
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.PermissionHelper
import com.pixel.gamebooster.utils.ShellUtils

private data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val icon: ImageVector,
    val onFix: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    repo: SettingsRepository,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val themeMode by repo.themeMode.collectAsState()
    val isPitchBlack by repo.isPitchBlack.collectAsState()

    var isShizukuGranted by remember { mutableStateOf(ShellUtils.hasPermission(context)) }
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsGranted by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var showPairingDialog by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        isShizukuGranted = ShellUtils.hasPermission(context)
        isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
        isUsageStatsGranted = PermissionHelper.hasUsageStatsPermission(context)
        isNotificationGranted = PermissionHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionItems = listOf(
        PermissionItem(
            id = "shizuku",
            title = "Privileged Shell (Shizuku / Root)",
            description = "Enables 120Hz display locking, kernel freezer, and system tuning.",
            isGranted = isShizukuGranted,
            icon = Icons.Rounded.Terminal,
            onFix = {
                if (ShellUtils.isShizukuAvailable()) {
                    showPairingDialog = true
                } else {
                    showPairingDialog = true
                }
            },
        ),
        PermissionItem(
            id = "accessibility",
            title = "Accessibility Window Trigger",
            description = "Detects foreground game switches with zero battery drain.",
            isGranted = isAccessibilityGranted,
            icon = Icons.Rounded.AccessibilityNew,
            onFix = { PermissionHelper.openAccessibilitySettings(context) },
        ),
        PermissionItem(
            id = "usage",
            title = "Usage Statistics Access",
            description = "Monitors game lifecycle to automate booster activation and restore delay.",
            isGranted = isUsageStatsGranted,
            icon = Icons.Rounded.QueryStats,
            onFix = { PermissionHelper.openUsageStatsSettings(context) },
        ),
        PermissionItem(
            id = "notification",
            title = "Notifications Access",
            description = "Displays active game session notifications and quick toggles.",
            isGranted = isNotificationGranted,
            icon = Icons.Rounded.Notifications,
            onFix = { PermissionHelper.openNotificationSettings(context) },
        ),
    )

    val (grantedList, missingList) = permissionItems.partition { it.isGranted }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Settings & Diagnostics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Theme preferences and system permissions health",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                    )
                }
            }

            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )

            RoundedCardContainer {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SegmentedPicker(
                        items = listOf(0, 1, 2),
                        selectedItem = themeMode,
                        onItemSelected = { repo.setThemeMode(it) },
                        labelProvider = {
                            when (it) {
                                1 -> "Light"
                                2 -> "Dark"
                                else -> "System"
                            }
                        },
                        iconProvider = {
                            when (it) {
                                1 -> Icons.Rounded.LightMode
                                2 -> Icons.Rounded.DarkMode
                                else -> Icons.Rounded.BrightnessAuto
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    IconToggleItem(
                        title = "Pitch Black AMOLED",
                        subtitle = "True pitch black background for dark theme on OLED displays",
                        icon = Icons.Rounded.Contrast,
                        isChecked = isPitchBlack,
                        onCheckedChange = { repo.setPitchBlack(it) },
                        index = 0,
                        count = 1,
                    )
                }
            }

            // Permissions Section
            Text(
                text = "Permissions Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )

            if (missingList.isNotEmpty()) {
                Text(
                    text = "Action Required (${missingList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp),
                )

                missingList.forEach { perm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = perm.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = perm.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = perm.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    perm.onFix()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Granted Permissions (${grantedList.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )

            RoundedCardContainer {
                grantedList.forEachIndexed { index, perm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Granted",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = perm.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = perm.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // About Footer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Pixel Game Booster",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Version 1.0.0 · Material 3 Expressive Suite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showPairingDialog) {
        ShizukuPairingDialog(
            onDismissRequest = {
                showPairingDialog = false
                refreshPermissions()
            },
        )
    }
}
