package com.pixel.gamebooster.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {

    @Test
    fun appNavigationDestinationsExposeAllFourTabs() {
        val destinations = NavDestination.values()
        assertEquals(4, destinations.size)
        assertEquals(NavDestination.DASHBOARD, NavDestination.Dashboard)
        assertEquals(NavDestination.PERFORMANCE, NavDestination.PerformanceDisplay)
        assertEquals(NavDestination.FREEZER, NavDestination.AppFreezer)
        assertEquals(NavDestination.NETWORK, NavDestination.NetworkSync)

        assertEquals("Dashboard", NavDestination.Dashboard.label)
        assertEquals("Display", NavDestination.PerformanceDisplay.label)
        assertEquals("Suspension", NavDestination.AppFreezer.label)
        assertEquals("Network", NavDestination.NetworkSync.label)
    }

    @Test
    fun destinationRoutesMapDeterministicallyToUniqueStrings() {
        val destinations = NavDestination.values()
        val routes = destinations.map { it.route }

        // All routes must be non-blank
        routes.forEach { route ->
            assertTrue("Route should not be blank", route.isNotBlank())
        }

        // All routes must be unique
        assertEquals("Route strings must be unique", destinations.size, routes.toSet().size)

        // Specific deterministic routes
        assertEquals("dashboard", NavDestination.DASHBOARD.route)
        assertEquals("display", NavDestination.PERFORMANCE.route)
        assertEquals("app_freezer", NavDestination.FREEZER.route)
        assertEquals("network_sync", NavDestination.NETWORK.route)
    }

    @Test
    fun fabClickCallbackTriggersRegisteredGameLaunchOrPickerIntent() {
        var launchedGame: String? = null
        var openedPicker = false

        val onLaunchGame: (String) -> Unit = { pkg -> launchedGame = pkg }
        val onOpenPicker: () -> Unit = { openedPicker = true }

        // When active game is present, triggers game launch
        handleFabClick(
            activeGamePackage = "com.example.game",
            onLaunchGame = onLaunchGame,
            onOpenPicker = onOpenPicker,
        )
        assertEquals("com.example.game", launchedGame)
        assertEquals(false, openedPicker)

        // Reset
        launchedGame = null
        openedPicker = false

        // When active game is null or empty, triggers picker
        handleFabClick(
            activeGamePackage = null,
            onLaunchGame = onLaunchGame,
            onOpenPicker = onOpenPicker,
        )
        assertEquals(null, launchedGame)
        assertEquals(true, openedPicker)

        // When active game is blank string, triggers picker
        openedPicker = false
        handleFabClick(
            activeGamePackage = "   ",
            onLaunchGame = onLaunchGame,
            onOpenPicker = onOpenPicker,
        )
        assertEquals(null, launchedGame)
        assertEquals(true, openedPicker)
    }

    @Test
    fun resolveLandingScreenResolvesToMainWhenOnboardingComplete() {
        assertEquals(AppScreen.MAIN, resolveLandingScreen(hasCompletedOnboarding = true))
        assertEquals(AppScreen.ONBOARDING, resolveLandingScreen(hasCompletedOnboarding = false))
    }

    @Test
    fun appNavigationStateDefaultsToGameLibraryLanding() {
        val navState = AppNavigationState(initialScreen = AppScreen.MAIN)
        assertTrue("AppScreen.MAIN should show library by default", navState.isShowingLibrary)
        assertFalse("AppScreen.MAIN should not show game tuning by default", navState.isShowingGameTuning)
        assertNull("Selected game package should be null initially", navState.selectedGamePackageForTuning)

        val libraryState = AppNavigationState(initialScreen = AppScreen.LIBRARY)
        assertTrue("AppScreen.LIBRARY should show library by default", libraryState.isShowingLibrary)
    }

    @Test
    fun gameSelectionTransitionsStateToGameTuningMode() {
        val navState = AppNavigationState(initialScreen = AppScreen.MAIN)
        val testPackage = "com.activision.callofduty.shooter"

        navState.selectGameForTuning(testPackage)

        assertFalse("Library grid should be hidden when tuning game", navState.isShowingLibrary)
        assertTrue("Game tuning mode should be active", navState.isShowingGameTuning)
        assertEquals(testPackage, navState.selectedGamePackageForTuning)
    }

    @Test
    fun backNavigationFromTuningResetsSelectedGamePackage() {
        val testPackage = "com.epicgames.portal"
        val navState = AppNavigationState(
            initialScreen = AppScreen.MAIN,
            initialSelectedGame = testPackage,
        )

        assertTrue(navState.isShowingGameTuning)
        assertEquals(testPackage, navState.selectedGamePackageForTuning)

        navState.backFromTuning()

        assertNull("Selected package must be cleared on back navigation", navState.selectedGamePackageForTuning)
        assertTrue("Library view must be restored after popping tuning", navState.isShowingLibrary)
        assertFalse(navState.isShowingGameTuning)
    }

    @Test
    fun handleGameSelectionAndBackCallbacksDispatchCorrectly() {
        var tuningGame: String? = null

        handleGameSelection("com.mojang.minecraftpe") { selected ->
            tuningGame = selected
        }
        assertEquals("com.mojang.minecraftpe", tuningGame)

        // Blank package should not trigger navigation
        handleGameSelection("   ") { selected ->
            tuningGame = selected
        }
        assertEquals("com.mojang.minecraftpe", tuningGame)

        handleBackFromTuning {
            tuningGame = null
        }
        assertNull(tuningGame)
    }

    @Test
    fun screenRecordingNavigationStateTransitionsCorrectly() {
        val navState = AppNavigationState(initialScreen = AppScreen.MAIN)
        assertFalse(navState.isShowingScreenRecording)

        navState.openScreenRecording()
        assertTrue("Screen recording sub-page should be showing", navState.isShowingScreenRecording)

        navState.backFromScreenRecording()
        assertFalse("Screen recording sub-page should no longer be showing", navState.isShowingScreenRecording)
    }

    @Test
    fun navigatingAwayFromMainResetsScreenRecordingState() {
        val navState = AppNavigationState(initialScreen = AppScreen.MAIN)
        navState.openScreenRecording()
        assertTrue(navState.isShowingScreenRecording)

        navState.navigateTo(AppScreen.PERMISSIONS)
        assertFalse("Navigating away from main should reset screen recording state", navState.isShowingScreenRecording)
    }

    @Test
    fun mainTabValuesContainGamesAndOverlay() {
        val tabs = MainTab.values()
        assertEquals(2, tabs.size)
        assertEquals(MainTab.GAMES, tabs[0])
        assertEquals(MainTab.OVERLAY, tabs[1])
    }
}
