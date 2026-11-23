package com.pixel.gamebooster.interruption

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DNDManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DNDManager.clearSnapshot(context)
        DNDManager.snapshottedFilter = null
        DNDManager.didApplyShizukuFallback = false
    }

    @After
    fun tearDown() {
        DNDManager.clearSnapshot(context)
        DNDManager.snapshottedFilter = null
        DNDManager.didApplyShizukuFallback = false
    }

    @Test
    fun shizukuCommands_matchExpectedStrings() {
        assertEquals("cmd notification set_dnd priority", DNDManager.getShizukuCommand(true))
        assertEquals("cmd notification set_dnd off", DNDManager.getShizukuCommand(false))
        assertEquals("cmd notification set_dnd priority", DNDManager.getShizukuCommand(1))
        assertEquals("cmd notification set_dnd off", DNDManager.getShizukuCommand(0))
    }

    @Test
    fun snapshotPersistence_savesAndRestoresFilterFaithfully() {
        val initialFilter = NotificationManager.INTERRUPTION_FILTER_ALL

        DNDManager.persistSnapshot(context, initialFilter, isShizuku = false)
        assertTrue(DNDManager.hasActiveSnapshot(context))

        val restoredFilter = DNDManager.getFilterFromPrefs(context)
        assertEquals(initialFilter, restoredFilter)

        DNDManager.clearSnapshot(context)
        assertFalse(DNDManager.hasActiveSnapshot(context))
        assertNull(DNDManager.getFilterFromPrefs(context))
    }

    @Test
    fun shizukuSnapshot_setsActiveState() {
        DNDManager.persistShizukuSnapshot(context)
        assertTrue(DNDManager.hasActiveSnapshot(context))
    }
}
