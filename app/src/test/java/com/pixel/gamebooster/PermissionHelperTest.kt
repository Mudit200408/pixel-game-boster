package com.pixel.gamebooster

import com.pixel.gamebooster.ui.navigation.AppScreen
import com.pixel.gamebooster.ui.navigation.NavDestination
import com.pixel.gamebooster.utils.PermissionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionHelperTest {

    @Test
    fun verifyShizukuConstants() {
        assertEquals(1001, PermissionHelper.SHIZUKU_REQUEST_CODE)
        assertEquals("moe.shizuku.privileged.api", PermissionHelper.SHIZUKU_PACKAGE)
        assertEquals("https://shizuku.rikka.app", PermissionHelper.SHIZUKU_URL)
    }

    @Test
    fun verifyAppScreenEnumRoutes() {
        val screens = AppScreen.values()
        assertEquals(4, screens.size)
        assertTrue(screens.contains(AppScreen.ONBOARDING))
        assertTrue(screens.contains(AppScreen.PERMISSIONS))
        assertTrue(screens.contains(AppScreen.GAME_SCANNING))
        assertTrue(screens.contains(AppScreen.MAIN))
    }

    @Test
    fun verifyNavDestinations() {
        val destinations = NavDestination.values()
        assertEquals(4, destinations.size)
        assertEquals("Dashboard", NavDestination.DASHBOARD.label)
        assertEquals("Performance", NavDestination.PERFORMANCE.label)
        assertEquals("Suspension", NavDestination.FREEZER.label)
        assertEquals("Network", NavDestination.NETWORK.label)
    }
}
