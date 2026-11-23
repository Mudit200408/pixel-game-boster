package com.pixel.gamebooster.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.ui.screens.AppFreezerScreen
import com.pixel.gamebooster.ui.screens.GameLibraryScreen
import com.pixel.gamebooster.ui.screens.GameScanningSetupScreen
import com.pixel.gamebooster.ui.screens.GameTuningContainer
import com.pixel.gamebooster.ui.screens.NetworkSyncScreen
import com.pixel.gamebooster.ui.screens.OnboardingScreen
import com.pixel.gamebooster.ui.screens.OverlaySettingsScreen
import com.pixel.gamebooster.ui.screens.PerformanceDisplayScreen
import com.pixel.gamebooster.ui.screens.PermissionsScreen
import com.pixel.gamebooster.ui.sheets.AppSelectionSheet
import com.pixel.gamebooster.ui.sheets.SettingsSheet
import com.pixel.gamebooster.ui.sheets.SystemServicesSheet
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil

enum class AppScreen {
    ONBOARDING,
    PERMISSIONS,
    GAME_SCANNING,
    MAIN;

    companion object {
        val LIBRARY: AppScreen get() = MAIN
    }
}

enum class MainTab {
    GAMES,
    OVERLAY
}

enum class NavDestination(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    val label: String = when (labelRes) {
        R.string.nav_tab_dashboard -> "Dashboard"
        R.string.nav_tab_performance -> "Performance"
        R.string.nav_tab_suspension -> "Suspension"
        R.string.nav_tab_network -> "Network"
        else -> ""
    },
) {
    DASHBOARD(R.string.nav_tab_dashboard, Icons.Rounded.Gamepad, "dashboard"),
    PERFORMANCE(R.string.nav_tab_performance, Icons.Rounded.Speed, "performance_display"),
    FREEZER(R.string.nav_tab_suspension, Icons.Rounded.AcUnit, "app_freezer"),
    NETWORK(R.string.nav_tab_network, Icons.Rounded.Wifi, "network_sync");

    companion object {
        val Dashboard: NavDestination get() = DASHBOARD
        val PerformanceDisplay: NavDestination get() = PERFORMANCE
        val AppFreezer: NavDestination get() = FREEZER
        val NetworkSync: NavDestination get() = NETWORK
    }
}

/**
 * Resolves the primary landing screen when user enters application.
 * Defaults to AppScreen.MAIN when onboarding has completed.
 */
fun resolveLandingScreen(hasCompletedOnboarding: Boolean): AppScreen {
    return if (hasCompletedOnboarding) AppScreen.MAIN else AppScreen.ONBOARDING
}

/**
 * Handles game selection event from the Game Library grid, transitioning to game tuning.
 */
fun handleGameSelection(
    packageName: String,
    onNavigateToTuning: (String) -> Unit,
) {
    if (packageName.isNotBlank()) {
        onNavigateToTuning(packageName)
    }
}

/**
 * Handles back navigation event from the Game Tuning view, resetting selected package.
 */
fun handleBackFromTuning(
    onResetSelection: () -> Unit,
) {
    onResetSelection()
}

/**
 * Encapsulates navigation state logic for AppNavigation to allow deterministic unit testing.
 */
class AppNavigationState(
    initialScreen: AppScreen = AppScreen.MAIN,
    initialSelectedGame: String? = null,
) {
    var currentScreen: AppScreen = initialScreen
        private set

    var selectedGamePackageForTuning: String? = initialSelectedGame
        private set

    val isShowingLibrary: Boolean
        get() = (currentScreen == AppScreen.MAIN || currentScreen == AppScreen.LIBRARY) &&
            selectedGamePackageForTuning == null

    val isShowingGameTuning: Boolean
        get() = (currentScreen == AppScreen.MAIN || currentScreen == AppScreen.LIBRARY) &&
            selectedGamePackageForTuning != null

    fun selectGameForTuning(packageName: String) {
        selectedGamePackageForTuning = packageName
    }

    fun backFromTuning() {
        selectedGamePackageForTuning = null
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
        if (screen != AppScreen.MAIN && screen != AppScreen.LIBRARY) {
            selectedGamePackageForTuning = null
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigation(
    repo: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val hasCompletedOnboarding by repo.hasCompletedOnboarding.collectAsState()

    var currentScreen by rememberSaveable {
        mutableStateOf(resolveLandingScreen(hasCompletedOnboarding))
    }

    var selectedGamePackageForTuning by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Intercept hardware / gesture back button when in Game Tuning mode
    BackHandler(enabled = selectedGamePackageForTuning != null) {
        selectedGamePackageForTuning = null
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
                onFinish = {
                    if (!hasCompletedOnboarding) {
                        currentScreen = AppScreen.GAME_SCANNING
                    } else {
                        currentScreen = AppScreen.MAIN
                    }
                },
                onBack = if (!hasCompletedOnboarding) {
                    { currentScreen = AppScreen.ONBOARDING }
                } else {
                    { currentScreen = AppScreen.MAIN }
                },
            )
            AppScreen.GAME_SCANNING -> GameScanningSetupScreen(
                repo = repo,
                onComplete = { currentScreen = AppScreen.MAIN },
            )
            AppScreen.MAIN, AppScreen.LIBRARY -> {
                val view = LocalView.current
                val adaptive = rememberAdaptiveLayoutInfo()
                var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
                var activeMainTab by rememberSaveable { mutableStateOf(MainTab.GAMES) }

                if (showSettingsSheet) {
                    SettingsSheet(
                        repo = repo,
                        onDismissRequest = { showSettingsSheet = false },
                    )
                }

                if (selectedGamePackageForTuning != null) {
                    GameTuningContainer(
                        packageName = selectedGamePackageForTuning!!,
                        repository = repo,
                        onBack = { selectedGamePackageForTuning = null },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (activeMainTab) {
                            MainTab.GAMES -> {
                                GameLibraryScreen(
                                    repo = repo,
                                    onGameClick = { selectedGamePackageForTuning = it },
                                    onOpenSettings = { showSettingsSheet = true },
                                    onFixPermissionsClick = { showSettingsSheet = true },
                                )
                            }
                            MainTab.OVERLAY -> {
                                OverlaySettingsScreen(
                                    repo = repo,
                                )
                            }
                        }

                        // Floating Bottom Navigation Bar (Matching In-Game Floating Bottom Toolbar with labels intact)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = if (adaptive.isCompactHeight) 2.dp else 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            HorizontalFloatingToolbar(
                                expanded = true,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = if (adaptive.isCompactHeight) 2.dp else 8.dp,
                                ),
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                                    toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                                    toolbarContainerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                val isGamesSelected = activeMainTab == MainTab.GAMES
                                val totalContentWidth = 170.dp
                                val expandedWidth = 116.dp
                                val collapsedWidth = 48.dp
                                val spacing = 6.dp

                                val gamesSlotWidth by animateDpAsState(
                                    targetValue = if (isGamesSelected) expandedWidth else collapsedWidth,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                    label = "games_slot_width",
                                )
                                val overlaySlotWidth = (totalContentWidth - spacing) - gamesSlotWidth

                                val indicatorOffset by animateDpAsState(
                                    targetValue = if (isGamesSelected) 0.dp else (totalContentWidth - expandedWidth),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                    label = "indicator_offset",
                                )

                                Box(
                                    modifier = Modifier
                                        .width(totalContentWidth)
                                        .height(48.dp),
                                ) {
                                    // 1. Sliding inner selected circle / pill
                                    Box(
                                        modifier = Modifier
                                            .offset(x = indicatorOffset)
                                            .width(expandedWidth)
                                            .height(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.background),
                                    )

                                    // 2. Interactive tab slots
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                    ) {
                                        // Tab 1: Game Library
                                        FloatingNavTabItem(
                                            selected = isGamesSelected,
                                            label = "Games",
                                            icon = Icons.Rounded.SportsEsports,
                                            width = gamesSlotWidth,
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                activeMainTab = MainTab.GAMES
                                            },
                                        )

                                        // Tab 2: In-Game Overlay Page
                                        FloatingNavTabItem(
                                            selected = !isGamesSelected,
                                            label = "Overlay",
                                            icon = Icons.Rounded.Layers,
                                            width = overlaySlotWidth,
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                activeMainTab = MainTab.OVERLAY
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating Navigation Tab Item for the main library and overlay bottom bar.
 * Sits above the sliding selection indicator with a fixed/animated slot width,
 * revealing the label with a bouncy spring animation when active,
 * and collapsing cleanly to an icon-only button when inactive.
 */
@Composable
private fun FloatingNavTabItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 14.dp,
                end = if (selected) 16.dp else 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                modifier = Modifier.size(20.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 20)) +
                        expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            expandFrom = Alignment.Start,
                        ),
                exit = fadeOut(animationSpec = tween(durationMillis = 80)) +
                       shrinkHorizontally(
                           animationSpec = spring(
                               dampingRatio = Spring.DampingRatioNoBouncy,
                               stiffness = Spring.StiffnessMedium,
                           ),
                           shrinkTowards = Alignment.Start,
                       ),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
