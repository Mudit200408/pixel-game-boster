package com.pixel.gamebooster.interruption

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        DNDManager.snapshottedPolicy = null
        DNDManager.didApplyShizukuFallback = false
    }

    @After
    fun tearDown() {
        DNDManager.clearSnapshot(context)
        DNDManager.snapshottedFilter = null
        DNDManager.snapshottedPolicy = null
        DNDManager.didApplyShizukuFallback = false
    }

    @Test
    fun priorityOnlyMode_createsCorrectPolicyBitmasks() {
        val policy = DNDManager.buildPolicyForMode(1)
        assertNotNull(policy)
        requireNotNull(policy)

        // Visual suppression: Suppress peeking, full screen intent, ambient, lights
        val suppressed = policy.suppressedVisualEffects
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK) != 0)
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT) != 0)
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT) != 0)
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS) != 0)

        // Priority categories: Alarms, Media, and Calls permitted
        val categories = policy.priorityCategories
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0)
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA) != 0)
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) != 0)

        // Senders: Starred contacts allowed for calls, none for messages
        assertEquals(NotificationManager.Policy.PRIORITY_SENDERS_STARRED, policy.priorityCallSenders)
        assertEquals(DNDManager.PRIORITY_SENDERS_NONE, policy.priorityMessageSenders)
    }

    @Test
    fun fullDndMode_createsCorrectPolicyBitmasks() {
        val policy = DNDManager.buildPolicyForMode(2)
        assertNotNull(policy)
        requireNotNull(policy)

        // Visual suppression: Suppress peeking and full screen intent
        val suppressed = policy.suppressedVisualEffects
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK) != 0)
        assertTrue((suppressed and NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT) != 0)

        // Priority categories: Alarms only, no calls or messages
        val categories = policy.priorityCategories
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0)
        assertEquals(0, categories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)
        assertEquals(0, categories and NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES)

        // Senders: None
        assertEquals(DNDManager.PRIORITY_SENDERS_NONE, policy.priorityCallSenders)
        assertEquals(DNDManager.PRIORITY_SENDERS_NONE, policy.priorityMessageSenders)
    }

    @Test
    fun offMode_returnsNullPolicy() {
        assertNull(DNDManager.buildPolicyForMode(0))
        assertNull(DNDManager.buildPolicyForMode(-1))
        assertNull(DNDManager.buildPolicyForMode(99))
    }

    @Test
    fun shizukuFallbackCommands_matchExpectedStrings() {
        assertEquals("cmd notification set_dnd priority", DNDManager.getShizukuCommand(1))
        assertEquals("cmd notification set_dnd none", DNDManager.getShizukuCommand(2))
        assertEquals("cmd notification set_dnd off", DNDManager.getShizukuCommand(0))
    }

    @Test
    fun snapshotPersistence_savesAndRestoresFaithfully() {
        val initialFilter = NotificationManager.INTERRUPTION_FILTER_ALL
        val initialPolicy = NotificationManager.Policy(
            NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS,
            NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE,
        )

        DNDManager.persistSnapshot(context, initialFilter, initialPolicy, isShizuku = false)
        assertTrue(DNDManager.hasActiveSnapshot(context))

        val restoredFilter = DNDManager.getFilterFromPrefs(context)
        val restoredPolicy = DNDManager.getPolicyFromPrefs(context)

        assertEquals(initialFilter, restoredFilter)
        assertNotNull(restoredPolicy)
        requireNotNull(restoredPolicy)

        assertEquals(initialPolicy.priorityCategories, restoredPolicy.priorityCategories)
        assertEquals(initialPolicy.priorityCallSenders, restoredPolicy.priorityCallSenders)
        assertEquals(initialPolicy.priorityMessageSenders, restoredPolicy.priorityMessageSenders)
        assertEquals(initialPolicy.suppressedVisualEffects, restoredPolicy.suppressedVisualEffects)

        DNDManager.clearSnapshot(context)
        assertFalse(DNDManager.hasActiveSnapshot(context))
    }

    @Test
    fun recoverLeakedDndState_clearsDanglingState() {
        DNDManager.persistShizukuSnapshot(context)
        assertTrue(DNDManager.hasActiveSnapshot(context))

        DNDManager.recoverLeakedDndState(context)
        assertFalse(DNDManager.hasActiveSnapshot(context))
    }

    @Test
    fun applyAndRestore_withoutPolicyAccess_triggersShizukuFallback() {
        // By default without policy access, Shizuku fallback should be engaged
        DNDManager.applyInterruptionPolicy(context, 1)
        assertTrue(DNDManager.didApplyShizukuFallback)
        assertTrue(DNDManager.hasActiveSnapshot(context))

        DNDManager.restoreInterruptionPolicy(context)
        assertFalse(DNDManager.didApplyShizukuFallback)
        assertFalse(DNDManager.hasActiveSnapshot(context))
    }
}
