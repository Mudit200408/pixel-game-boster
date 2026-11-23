package com.pixel.gamebooster.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.screens.AppFreezerScreen
import com.pixel.gamebooster.ui.screens.DashboardScreen
import com.pixel.gamebooster.ui.screens.NetworkSyncScreen
import com.pixel.gamebooster.ui.screens.OnboardingScreen
import com.pixel.gamebooster.ui.screens.PerformanceDisplayScreen
import com.pixel.gamebooster.ui.screens.PermissionsScreen

enum class AppScreen {
    ONBOARDING,
    PERMISSIONS,
    MAIN,
}

enum class NavDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Rounded.Gamepad),
    PERFORMANCE("Performance", Icons.Rounded.Speed),
    FREEZER("App Freezer", Icons.Rounded.AcUnit),
    NETWORK("Network", Icons.Rounded.Wifi),
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
    var currentDestination by remember { mutableStateOf(NavDestination.DASHBOARD) }
    var showAppSelectionSheet by remember { mutableStateOf(false) }
    var showServicesSheet by remember { mutableStateOf(false) }
    var showGamePickerSheet by remember { mutableStateOf(false) }

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
            NavigationBar {
                NavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(text = destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
