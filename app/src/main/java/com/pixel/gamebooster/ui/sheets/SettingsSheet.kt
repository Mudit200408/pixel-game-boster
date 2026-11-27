package com.pixel.gamebooster.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.interruption.DNDManager
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.dialogs.ShizukuPairingDialog
import com.pixel.gamebooster.ui.core.pickers.SegmentedPicker
import com.pixel.gamebooster.ui.theme.GrantedGreen
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
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
    val isTactileHaptics by repo.isTactileHaptics.collectAsState()
    val isGameModeToastEnabled by repo.isGameModeToastEnabled.collectAsState()

    var isShizukuGranted by remember { mutableStateOf(ShellUtils.hasPermission(context)) }
    var isDndGranted by remember { mutableStateOf(PermissionHelper.hasDndAccess(context)) }
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsGranted by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var isWriteSettingsGranted by remember { mutableStateOf(PermissionHelper.hasWriteSettingsPermission(context)) }
    var showPairingDialog by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        isShizukuGranted = ShellUtils.hasPermission(context)
        isDndGranted = PermissionHelper.hasDndAccess(context)
        isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
        isUsageStatsGranted = PermissionHelper.hasUsageStatsPermission(context)
        isNotificationGranted = PermissionHelper.hasNotificationPermission(context)
        isOverlayGranted = PermissionHelper.hasOverlayPermission(context)
        isWriteSettingsGranted = PermissionHelper.hasWriteSettingsPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val shizukuHandle = PermissionHelper.registerShizukuListeners {
            refreshPermissions()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            PermissionHelper.unregisterShizukuListeners(shizukuHandle)
        }
    }

    val permissionItems = listOf(
        PermissionItem(
            id = "shizuku",
            title = stringResource(R.string.settings_perm_shizuku_title),
            description = stringResource(R.string.settings_perm_shizuku_desc),
            isGranted = isShizukuGranted,
            icon = Icons.Rounded.Terminal,
            onFix = {
                if (ShellUtils.isShizukuAvailable()) {
                    PermissionHelper.requestShizukuPermission()
                } else {
                    showPairingDialog = true
                }
            },
        ),
        PermissionItem(
            id = "dnd",
            title = stringResource(R.string.perm_dnd_title),
            description = stringResource(R.string.perm_dnd_desc),
            isGranted = isDndGranted,
            icon = Icons.Rounded.DoNotDisturbOn,
            onFix = {
                if (isShizukuGranted) {
                    PermissionHelper.grantDndAccessViaShizuku(context) { granted ->
                        if (granted) {
                            DNDManager.getOrCreateGamingZenRuleId(context)
                        }
                        refreshPermissions()
                    }
                } else {
                    PermissionHelper.openDndSettings(context)
                }
            },
        ),
        PermissionItem(
            id = "accessibility",
            title = stringResource(R.string.settings_perm_accessibility_title),
            description = stringResource(R.string.settings_perm_accessibility_desc),
            isGranted = isAccessibilityGranted,
            icon = Icons.Rounded.AccessibilityNew,
            onFix = { PermissionHelper.openAccessibilitySettings(context) },
        ),
        PermissionItem(
            id = "usage",
            title = stringResource(R.string.settings_perm_usage_title),
            description = stringResource(R.string.settings_perm_usage_desc),
            isGranted = isUsageStatsGranted,
            icon = Icons.Rounded.QueryStats,
            onFix = { PermissionHelper.openUsageStatsSettings(context) },
        ),
        PermissionItem(
            id = "notification",
            title = stringResource(R.string.settings_perm_notification_title),
            description = stringResource(R.string.settings_perm_notification_desc),
            isGranted = isNotificationGranted,
            icon = Icons.Rounded.Notifications,
            onFix = { PermissionHelper.openNotificationSettings(context) },
        ),
        PermissionItem(
            id = "overlay",
            title = stringResource(R.string.perm_overlay_title),
            description = stringResource(R.string.perm_overlay_desc),
            isGranted = isOverlayGranted,
            icon = Icons.Rounded.Layers,
            onFix = {
                if (isShizukuGranted) {
                    PermissionHelper.grantOverlayPermissionViaShizuku(context) {
                        refreshPermissions()
                    }
                } else {
                    PermissionHelper.openOverlaySettings(context)
                }
            },
        ),
        PermissionItem(
            id = "write_settings",
            title = stringResource(R.string.perm_write_settings_title),
            description = stringResource(R.string.perm_write_settings_desc),
            isGranted = isWriteSettingsGranted,
            icon = Icons.Rounded.Tune,
            onFix = {
                if (isShizukuGranted) {
                    PermissionHelper.grantWriteSettingsViaShizuku(context) {
                        refreshPermissions()
                    }
                } else {
                    PermissionHelper.openWriteSettings(context)
                }
            },
        ),
    )

    val (grantedList, missingList) = permissionItems.partition { it.isGranted }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                // Header - Clean "Settings" title only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onDismissRequest()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.settings_cd_close),
                        )
                    }
                }
            }

            item {
                // Appearance Section
                Text(
                    text = stringResource(R.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_theme_mode),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val lightLabel = stringResource(R.string.settings_theme_light)
                        val darkLabel = stringResource(R.string.settings_theme_dark)
                        val systemLabel = stringResource(R.string.settings_theme_system)
                        SegmentedPicker(
                            items = listOf(0, 1, 2),
                            selectedItem = themeMode,
                            onItemSelected = { repo.setThemeMode(it) },
                            labelProvider = {
                                when (it) {
                                    1 -> lightLabel
                                    2 -> darkLabel
                                    else -> systemLabel
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
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_preferences),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            item {
                RoundedCardContainer {
                    IconToggleItem(
                        title = stringResource(R.string.settings_tactile_haptics_title),
                        subtitle = stringResource(R.string.settings_tactile_haptics_desc),
                        icon = Icons.Rounded.Vibration,
                        checked = isTactileHaptics,
                        onCheckedChange = { repo.setTactileHaptics(it) },
                        index = 0,
                        count = 2,
                    )
                    IconToggleItem(
                        title = stringResource(R.string.settings_game_mode_toast_title),
                        subtitle = stringResource(R.string.settings_game_mode_toast_desc),
                        icon = Icons.Rounded.Notifications,
                        checked = isGameModeToastEnabled,
                        onCheckedChange = { repo.setGameModeToastEnabled(it) },
                        index = 1,
                        count = 2,
                    )
                }
            }

            item {
                // Permissions Section - Clean "Permissions" title only
                Text(
                    text = stringResource(R.string.settings_section_permissions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (missingList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.settings_cd_all_perms_granted),
                                tint = GrantedGreen,
                                modifier = Modifier.size(32.dp),
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_all_perms_granted),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_all_perms_granted_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.settings_action_required, missingList.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                items(missingList, key = { it.id }) { perm ->
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
                                Text(stringResource(R.string.settings_grant_permission))
                            }
                        }
                    }
                }

                if (grantedList.isNotEmpty()) {
                    item {
                        RoundedCardContainer {
                            grantedList.forEachIndexed { index, perm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
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
                                            contentDescription = stringResource(R.string.settings_cd_granted),
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
                    }
                }
            }

            item {
                // About Footer - Clean minimal text without background card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_app_name),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    val fallbackVersion = stringResource(R.string.settings_version_fallback)
                    val appVersion = remember(context, fallbackVersion) {
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull().orEmpty().ifBlank { fallbackVersion }
                    }
                    Text(
                        text = stringResource(R.string.settings_version, appVersion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
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
