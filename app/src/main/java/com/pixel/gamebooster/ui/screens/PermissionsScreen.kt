package com.pixel.gamebooster.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.components.PullTitleHeader
import com.pixel.gamebooster.ui.components.rememberPullTitleHeaderState
import com.pixel.gamebooster.ui.core.cards.PermissionCard
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.dialogs.ShizukuPairingDialog
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil
import com.pixel.gamebooster.utils.PermissionHelper
import com.pixel.gamebooster.utils.ShellUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    repo: SettingsRepository,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isShizukuGranted by remember { mutableStateOf(ShellUtils.hasPermission(context)) }
    var isShizukuAvailable by remember { mutableStateOf(ShellUtils.isShizukuAvailable()) }
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsGranted by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var isWriteSettingsGranted by remember { mutableStateOf(PermissionHelper.hasWriteSettingsPermission(context)) }
    var showPairingDialog by remember { mutableStateOf(false) }

    fun refreshAllPermissions() {
        isShizukuGranted = ShellUtils.hasPermission(context)
        isShizukuAvailable = ShellUtils.isShizukuAvailable()
        isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
        isUsageStatsGranted = PermissionHelper.hasUsageStatsPermission(context)
        isNotificationGranted = PermissionHelper.hasNotificationPermission(context)
        isOverlayGranted = PermissionHelper.hasOverlayPermission(context)
        isWriteSettingsGranted = PermissionHelper.hasWriteSettingsPermission(context)
    }

    // Lifecycle auto-refresh: Re-check permissions immediately upon returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Register Shizuku binder and permission result listeners
    LaunchedEffect(Unit) {
        PermissionHelper.initShizuku {
            refreshAllPermissions()
        }
    }

    // Runtime permission launcher for Notifications (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshAllPermissions()
    }

    val grantedCount = listOf(
        isShizukuGranted,
        isAccessibilityGranted,
        isUsageStatsGranted,
        isNotificationGranted,
        isOverlayGranted,
        isWriteSettingsGranted,
    ).count { it }
    val progress = grantedCount / 6f
    val allGranted = isShizukuGranted && isAccessibilityGranted && isUsageStatsGranted && isNotificationGranted && isOverlayGranted && isWriteSettingsGranted

    if (showPairingDialog) {
        ShizukuPairingDialog(onDismissRequest = { showPairingDialog = false })
    }

    val headerState = rememberPullTitleHeaderState(minHeight = 44.dp, maxHeight = 76.dp)
    val adaptive = rememberAdaptiveLayoutInfo()

    val shizukuCard = @Composable {
        // 1. Shizuku / Root ADB privilege (MANDATORY)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.Terminal,
                title = stringResource(R.string.perm_shizuku_title),
                description = stringResource(R.string.perm_shizuku_desc),
                isGranted = isShizukuGranted,
                isMandatory = true,
                actionLabel = if (isShizukuAvailable) {
                    stringResource(R.string.perm_action_grant)
                } else {
                    stringResource(R.string.perm_action_launch_shizuku)
                },
                onActionClick = {
                    if (isShizukuAvailable) {
                        PermissionHelper.requestShizukuPermission()
                    } else {
                        PermissionHelper.launchShizuku(context)
                    }
                },
                secondaryActionLabel = stringResource(R.string.perm_action_pairing_guide),
                onSecondaryActionClick = { showPairingDialog = true },
                instructions = if (!isShizukuAvailable && !isShizukuGranted) {
                    stringResource(R.string.perm_shizuku_not_running)
                } else null,
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_120hz),
                    stringResource(R.string.perm_feature_app_suspension),
                    stringResource(R.string.perm_feature_ram_cache),
                    stringResource(R.string.perm_feature_wifi_mode),
                ),
            )
        }
    }

    val accessibilityCard = @Composable {
        // 2. Accessibility Service (Recommended for zero-latency detection)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.AccessibilityNew,
                title = stringResource(R.string.perm_accessibility_title),
                description = stringResource(R.string.perm_accessibility_desc),
                isGranted = isAccessibilityGranted,
                isMandatory = false,
                actionLabel = stringResource(R.string.perm_action_open_settings),
                onActionClick = {
                    PermissionHelper.openAccessibilitySettings(context)
                },
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_instant_detection),
                    stringResource(R.string.perm_feature_zero_battery),
                ),
            )
        }
    }

    val usageStatsCard = @Composable {
        // 3. Usage Access (Secondary detection fallback)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.QueryStats,
                title = stringResource(R.string.perm_usage_title),
                description = stringResource(R.string.perm_usage_desc),
                isGranted = isUsageStatsGranted,
                isMandatory = false,
                actionLabel = stringResource(R.string.perm_action_open_settings),
                onActionClick = {
                    PermissionHelper.openUsageStatsSettings(context)
                },
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_secondary_tracking),
                    stringResource(R.string.perm_feature_usage_auditing),
                ),
            )
        }
    }

    val notificationCard = @Composable {
        // 4. Notifications (API 33+)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.perm_notifications_title),
                description = stringResource(R.string.perm_notifications_desc),
                isGranted = isNotificationGranted,
                isMandatory = false,
                actionLabel = stringResource(R.string.perm_action_allow_notifications),
                onActionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionHelper.openNotificationSettings(context)
                    }
                },
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_foreground_service),
                    stringResource(R.string.perm_feature_quick_controls),
                ),
            )
        }
    }

    val overlayCard = @Composable {
        // 5. Display Over Other Apps (Overlay Window)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.perm_overlay_title),
                description = stringResource(R.string.perm_overlay_desc),
                isGranted = isOverlayGranted,
                isMandatory = false,
                actionLabel = if (isShizukuGranted) {
                    stringResource(R.string.perm_action_auto_grant)
                } else {
                    stringResource(R.string.perm_action_open_settings)
                },
                onActionClick = {
                    if (isShizukuGranted) {
                        PermissionHelper.grantOverlayPermissionViaShizuku(context) {
                            refreshAllPermissions()
                        }
                    } else {
                        PermissionHelper.openOverlaySettings(context)
                    }
                },
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_floating_hud),
                    stringResource(R.string.perm_feature_quick_settings_panel),
                ),
            )
        }
    }

    val writeSettingsCard = @Composable {
        // 6. Modify System Settings (Direct Brightness)
        RoundedCardContainer {
            PermissionCard(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.perm_write_settings_title),
                description = stringResource(R.string.perm_write_settings_desc),
                isGranted = isWriteSettingsGranted,
                isMandatory = false,
                actionLabel = if (isShizukuGranted) {
                    stringResource(R.string.perm_action_auto_grant)
                } else {
                    stringResource(R.string.perm_action_open_settings)
                },
                onActionClick = {
                    if (isShizukuGranted) {
                        PermissionHelper.grantWriteSettingsViaShizuku(context) {
                            refreshAllPermissions()
                        }
                    } else {
                        PermissionHelper.openWriteSettings(context)
                    }
                },
                dependentFeatures = listOf(
                    stringResource(R.string.perm_feature_direct_brightness),
                ),
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Collapsible Gestural Header (starts big, scales down as you scroll, no description)
            PullTitleHeader(
                title = stringResource(R.string.perm_title),
                state = headerState,
            )

            // Top Readiness Progress Indicator
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.perm_ready_count, grantedCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (allGranted) {
                                stringResource(R.string.perm_status_all_ready)
                            } else if (isShizukuGranted) {
                                stringResource(R.string.perm_status_optimizations_ready)
                            } else {
                                stringResource(R.string.perm_status_shizuku_required)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isShizukuGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (isShizukuGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }

            if (adaptive.isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        shizukuCard()
                        accessibilityCard()
                        overlayCard()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        usageStatsCard()
                        notificationCard()
                        writeSettingsCard()
                    }
                }
            } else {
                shizukuCard()
                accessibilityCard()
                usageStatsCard()
                notificationCard()
                overlayCard()
                writeSettingsCard()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enter Game Booster Button (revealed at bottom, unlocked only when all granted)
            Button(
                onClick = {
                    HapticUtil.performClick(view)
                    repo.setBoosterEnabled(true)
                    if (PermissionHelper.hasUsageStatsPermission(context)) {
                        AppDetectionService.start(context)
                    }
                    onFinish()
                },
                enabled = allGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    text = if (allGranted) {
                        stringResource(R.string.perm_btn_enter)
                    } else {
                        stringResource(R.string.perm_btn_grant_all_continue, grantedCount)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (!allGranted) {
                TextButton(
                    onClick = {
                        HapticUtil.performClick(view)
                        repo.setBoosterEnabled(true)
                        if (PermissionHelper.hasUsageStatsPermission(context)) {
                            AppDetectionService.start(context)
                        }
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_btn_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
