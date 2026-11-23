package com.pixel.gamebooster.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.screens.AppFreezerScreen
import com.pixel.gamebooster.ui.screens.DashboardScreen
import com.pixel.gamebooster.ui.screens.NetworkSyncScreen
import com.pixel.gamebooster.ui.screens.OnboardingScreen
import com.pixel.gamebooster.ui.screens.PerformanceDisplayScreen
import com.pixel.gamebooster.ui.screens.PermissionsScreen
import com.pixel.gamebooster.ui.sheets.SettingsSheet
import com.pixel.gamebooster.utils.GameModeManager

enum class AppScreen {
    ONBOARDING,
    PERMISSIONS,
    MAIN,
}

enum class NavDestination(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    DASHBOARD("Dashboard", Icons.Rounded.Gamepad, "dashboard"),
    PERFORMANCE("Performance", Icons.Rounded.Speed, "performance_display"),
    FREEZER("App Freezer", Icons.Rounded.AcUnit, "app_freezer"),
    NETWORK("Network", Icons.Rounded.Wifi, "network_sync");

    companion object {
        val Dashboard: NavDestination get() = DASHBOARD
        val PerformanceDisplay: NavDestination get() = PERFORMANCE
        val AppFreezer: NavDestination get() = FREEZER
        val NetworkSync: NavDestination get() = NETWORK
    }
}

/**
 * Dispatches FAB click events: launches active game if running, otherwise opens game picker.
 */
fun handleFabClick(
    activeGamePackage: String?,
    onLaunchGame: (String) -> Unit,
    onOpenPicker: () -> Unit,
) {
    if (!activeGamePackage.isNullOrBlank()) {
        onLaunchGame(activeGamePackage)
    } else {
        onOpenPicker()
    }
}

@Composable
fun AppNavigation(
    repo: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val hasCompletedOnboarding by repo.hasCompletedOnboarding.collectAsState()

    var currentScreen by remember(hasCompletedOnboarding) {
        mutableStateOf(if (hasCompletedOnboarding) AppScreen.MAIN else AppScreen.ONBOARDING)
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "rootScreenTransition",
        modifier = modifier.fillMaxSize(),
    ) { screen ->
        when (screen) {
            AppScreen.ONBOARDING -> OnboardingScreen(
                onNavigateToPermissions = { currentScreen = AppScreen.PERMISSIONS },
            )
            AppScreen.PERMISSIONS -> PermissionsScreen(
                repo = repo,
                onFinish = { currentScreen = AppScreen.MAIN },
                onBack = if (!hasCompletedOnboarding) {
                    { currentScreen = AppScreen.ONBOARDING }
                } else {
                    { currentScreen = AppScreen.MAIN }
                },
            )
            AppScreen.MAIN -> MainScaffold(
                repo = repo,
                onNavigateToPermissions = { currentScreen = AppScreen.PERMISSIONS },
            )
        }
    }
}

@Composable
private fun MainScaffold(
    repo: SettingsRepository,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(NavDestination.DASHBOARD) }
    var showAppSelectionSheet by remember { mutableStateOf(false) }
    var showServicesSheet by remember { mutableStateOf(false) }
    var showGamePickerSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    if (showSettingsSheet) {
        SettingsSheet(
            repo = repo,
            onDismissRequest = { showSettingsSheet = false },
        )
    }

    if (showAppSelectionSheet) {
        com.pixel.gamebooster.ui.sheets.AppSelectionSheet(
            repo = repo,
            onDismissRequest = { showAppSelectionSheet = false },
            title = "Select Apps to Freeze",
            isGamePicker = false,
        )
    }

    if (showServicesSheet) {
        com.pixel.gamebooster.ui.sheets.SystemServicesSheet(
            repo = repo,
            onDismissRequest = { showServicesSheet = false },
        )
    }

    if (showGamePickerSheet) {
        com.pixel.gamebooster.ui.sheets.AppSelectionSheet(
            repo = repo,
            onDismissRequest = { showGamePickerSheet = false },
            title = "Add Game to Launcher",
            isGamePicker = true,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PixelFloatingToolbar(
                    destinations = NavDestination.values().toList(),
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it },
                    onSettingsClick = { showSettingsSheet = true },
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) { destination ->
            when (destination) {
                NavDestination.DASHBOARD -> DashboardScreen(
                    repo = repo,
                    onAddGameClick = { showGamePickerSheet = true },
                    onFixPermissionsClick = onNavigateToPermissions,
                )
                NavDestination.PERFORMANCE -> PerformanceDisplayScreen(repo = repo)
                NavDestination.FREEZER -> AppFreezerScreen(
                    repo = repo,
                    onOpenAppSelectionSheet = { showAppSelectionSheet = true },
                    onOpenServicesSheet = { showServicesSheet = true },
                )
                NavDestination.NETWORK -> NetworkSyncScreen(repo = repo)
            }
        }
    }
}
