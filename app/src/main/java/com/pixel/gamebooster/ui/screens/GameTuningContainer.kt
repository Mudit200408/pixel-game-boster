package com.pixel.gamebooster.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.navigation.NavDestination
import com.pixel.gamebooster.ui.navigation.PixelFloatingToolbar
import com.pixel.gamebooster.ui.sheets.AppSelectionSheet
import com.pixel.gamebooster.ui.sheets.SystemServicesSheet
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import kotlinx.coroutines.launch

/**
 * Game Tuning Container hosting scoped feature sub-screens (Dashboard, Performance,
 * App Freezer, Network Sync) for a specific game profile (REQ-02, D-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTuningContainer(
    packageName: String,
    repository: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adaptive = rememberAdaptiveLayoutInfo()
    val layoutDirection = LocalLayoutDirection.current
    val pm = context.packageManager

    // Reactive observation of effective profile
    val initialProfile = remember(packageName) {
        repository.createDefaultEffectiveProfile(packageName)
    }
    val profile by repository.observeEffectiveProfile(packageName).collectAsState(initial = initialProfile)

    val appInfo = remember(packageName) {
        runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(info).toString()
            val icon = pm.getApplicationIcon(info)
            label to icon
        }.getOrNull()
    }
    val appName = appInfo?.first ?: packageName
    val appIcon = appInfo?.second

    var currentSubTab by rememberSaveable { mutableStateOf(NavDestination.DASHBOARD) }
    var showAppSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showServicesSheet by rememberSaveable { mutableStateOf(false) }

    if (showAppSelectionSheet) {
        AppSelectionSheet(
            repo = repository,
            onDismissRequest = { showAppSelectionSheet = false },
            title = stringResource(R.string.app_select_freeze_title),
            isGamePicker = false,
            selectedAppsOverride = profile.suspendedApps,
            excludedPackages = setOf(packageName),
            onToggleAppOverride = { pkg ->
                val newSet = if (profile.suspendedApps.contains(pkg)) {
                    profile.suspendedApps - pkg
                } else {
                    profile.suspendedApps + pkg
                }
                coroutineScope.launch {
                    repository.saveProfile(profile.copy(customized = true, suspendedApps = newSet))
                }
            },
        )
    }

    if (showServicesSheet) {
        SystemServicesSheet(
            repo = repository,
            onDismissRequest = { showServicesSheet = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (appIcon != null) {
                            val bitmap = remember(packageName) {
                                runCatching { appIcon.toBitmap(96, 96).asImageBitmap() }.getOrNull()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.game_tuning_cd_icon),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Gamepad,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = appName,
                                style = TextStyle(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = packageName,
                                style = TextStyle(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.game_tuning_cd_back),
                        )
                    }
                },
                actions = {
                    val launchIntent = remember(packageName) { pm.getLaunchIntentForPackage(packageName) }
                    if (launchIntent != null) {
                        IconButton(
                            onClick = {
                                context.startActivity(launchIntent)
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Gamepad,
                                contentDescription = stringResource(R.string.game_library_launch_game),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = if (adaptive.isCompactHeight) 2.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PixelFloatingToolbar(
                    destinations = listOf(
                        NavDestination.DASHBOARD,
                        NavDestination.PERFORMANCE,
                        NavDestination.FREEZER,
                        NavDestination.NETWORK,
                    ),
                    currentDestination = currentSubTab,
                    onDestinationSelected = { currentSubTab = it },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
        ) {
            // Sub-Screen Content Area
            AnimatedContent(
                targetState = currentSubTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tuningSubTabTransition",
                modifier = Modifier.fillMaxSize(),
            ) { subTab ->
                when (subTab) {
                    NavDestination.DASHBOARD -> DashboardScreen(
                        repo = repository,
                        scopedPackage = packageName,
                        scopedProfile = profile,
                    )
                    NavDestination.PERFORMANCE -> DisplayScreen(
                        repo = repository,
                        scopedPackage = packageName,
                        scopedProfile = profile,
                    )
                    NavDestination.FREEZER -> AppFreezerScreen(
                        repo = repository,
                        scopedPackage = packageName,
                        scopedProfile = profile,
                        onOpenAppSelectionSheet = { showAppSelectionSheet = true },
                        onOpenServicesSheet = { showServicesSheet = true },
                    )
                    NavDestination.NETWORK -> NetworkSyncScreen(
                        repo = repository,
                        scopedPackage = packageName,
                        scopedProfile = profile,
                    )
                }
            }
        }
    }
}
