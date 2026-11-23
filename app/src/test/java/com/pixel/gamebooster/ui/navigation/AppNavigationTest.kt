package com.pixel.gamebooster.ui.navigation

import org.junit.Assert.assertEquals
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
        assertEquals("Performance", NavDestination.PerformanceDisplay.label)
        assertEquals("App Freezer", NavDestination.AppFreezer.label)
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
        assertEquals("performance_display", NavDestination.PERFORMANCE.route)
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
}
