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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.services.AppDetectionService
import com.pixel.gamebooster.ui.core.cards.PermissionCard
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.core.dialogs.ShizukuPairingDialog
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

    var isShizukuOrRootGranted by remember { mutableStateOf(ShellUtils.hasPermission(context)) }
    var isShizukuAvailable by remember { mutableStateOf(ShellUtils.isShizukuAvailable()) }
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsGranted by remember { mutableStateOf(PermissionHelper.hasUsageStatsPermission(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var showPairingDialog by remember { mutableStateOf(false) }

    fun refreshAllPermissions() {
        isShizukuOrRootGranted = ShellUtils.hasPermission(context)
        isShizukuAvailable = ShellUtils.isShizukuAvailable()
        isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
        isUsageStatsGranted = PermissionHelper.hasUsageStatsPermission(context)
        isNotificationGranted = PermissionHelper.hasNotificationPermission(context)
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
        isShizukuOrRootGranted,
        isAccessibilityGranted,
        isUsageStatsGranted,
        isNotificationGranted,
    ).count { it }
    val progress = grantedCount / 4f

    if (showPairingDialog) {
        ShizukuPairingDialog(onDismissRequest = { showPairingDialog = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Permission Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Configure device privileges for optimizations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = {
                                HapticUtil.performClick(view)
                                onBack()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!isShizukuOrRootGranted) {
                    Text(
                        text = "Privileged ADB access via Shizuku or Root is strictly required to execute system performance tweaks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Button(
                    onClick = {
                        HapticUtil.performClick(view)
                        repo.setBoosterEnabled(true)
                        repo.setCompletedOnboarding(true)
                        if (PermissionHelper.hasUsageStatsPermission(context)) {
                            AppDetectionService.start(context)
                        }
                        onFinish()
                    },
                    enabled = isShizukuOrRootGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text(
                        text = "Enter Game Booster",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Readiness Progress Indicator
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Permissions Ready: $grantedCount / 4",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (grantedCount == 4) "All Ready" else if (isShizukuOrRootGranted) "Optimizations Ready" else "Shizuku Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isShizukuOrRootGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (isShizukuOrRootGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }

            // 1. Shizuku / Root ADB privilege (MANDATORY)
            RoundedCardContainer {
                PermissionCard(
                    icon = Icons.Rounded.Terminal,
                    title = "Shizuku / Root ADB",
                    description = "Required to execute privileged system commands for refresh rate locking, app freezing, and RAM optimization.",
                    isGranted = isShizukuOrRootGranted,
                    isMandatory = true,
                    actionLabel = if (isShizukuAvailable) "Grant Permission" else "Launch Shizuku",
                    onActionClick = {
                        if (isShizukuAvailable) {
                            PermissionHelper.requestShizukuPermission()
                        } else {
                            PermissionHelper.launchShizuku(context)
                        }
                    },
                    secondaryActionLabel = "Pairing Guide",
                    onSecondaryActionClick = { showPairingDialog = true },
                    instructions = if (!isShizukuAvailable && !isShizukuOrRootGranted) {
                        "Shizuku service is not running. Launch Shizuku to start it via Wireless Debugging, or open Pairing Guide."
                    } else null,
                    dependentFeatures = listOf(
                        "120Hz Hardware Panel Lock",
                        "Background App Suspension",
                        "RAM & Cache Optimization",
                        "Low-Latency Wi-Fi Mode",
                    ),
                )
            }

            // 2. Accessibility Service (Recommended for zero-latency detection)
            RoundedCardContainer {
                PermissionCard(
                    icon = Icons.Rounded.AccessibilityNew,
                    title = "Accessibility Service",
                    description = "Enables instant foreground game detection with zero background polling or battery drain.",
                    isGranted = isAccessibilityGranted,
                    isMandatory = false,
                    actionLabel = "Open Settings",
                    onActionClick = {
                        PermissionHelper.openAccessibilitySettings(context)
                    },
                    dependentFeatures = listOf(
                        "Instant Game Launch Detection",
                        "Zero Battery Background Monitoring",
                    ),
                )
            }

            // 3. Usage Access (Secondary detection fallback)
            RoundedCardContainer {
                PermissionCard(
                    icon = Icons.Rounded.QueryStats,
                    title = "Usage Access",
                    description = "Secondary fallback detection to verify running game sessions and optimize system background processes.",
                    isGranted = isUsageStatsGranted,
                    isMandatory = false,
                    actionLabel = "Open Settings",
                    onActionClick = {
                        PermissionHelper.openUsageStatsSettings(context)
                    },
                    dependentFeatures = listOf(
                        "Secondary Session Tracking",
                        "Background App Usage Auditing",
                    ),
                )
            }

            // 4. Notifications (API 33+)
            RoundedCardContainer {
                PermissionCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    description = "Allows the Game Booster foreground service to display active optimization status and quick controls.",
                    isGranted = isNotificationGranted,
                    isMandatory = false,
                    actionLabel = "Allow Notifications",
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionHelper.openNotificationSettings(context)
                        }
                    },
                    dependentFeatures = listOf(
                        "Foreground Service Indicator",
                        "Quick Optimization Tile Controls",
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
