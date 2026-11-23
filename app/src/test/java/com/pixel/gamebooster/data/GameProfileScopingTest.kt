package com.pixel.gamebooster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixel.gamebooster.SettingsRepositoryTest
import com.pixel.gamebooster.data.db.AppDatabase
import com.pixel.gamebooster.data.db.dao.GameProfileDao
import com.pixel.gamebooster.data.model.GameProfile
import com.pixel.gamebooster.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit test suite verifying per-game profile scoping and isolation (REQ-02, D-02, D-03).
 * Ensures that per-game profile mutations do not pollute global defaults or other game profiles.
 */
@RunWith(RobolectricTestRunner::class)
class GameProfileScopingTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GameProfileDao
    private lateinit var fakePrefs: SettingsRepositoryTest.FakeSharedPreferences
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.gameProfileDao()
        fakePrefs = SettingsRepositoryTest.FakeSharedPreferences()
        repository = SettingsRepository(fakePrefs, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mutatingSettingOnGameUpdatesOnlyThatGameAndLeavesGlobalDefaultsUntouched() = runTest {
        val packageA = "com.game.alpha"
        val packageB = "com.game.beta"

        // Baseline: global lockRefreshRate is true, fpsOverride is 0
        assertTrue("Global lockRefreshRate should default to true", repository.isLockRefreshRate.value)
        assertEquals("Global fpsOverride should default to 0", 0, repository.fpsOverride.value)

        // Mutate Game A profile
        val profileA = repository.getEffectiveProfile(packageA).copy(
            customized = true,
            lockRefreshRate = false,
            fpsOverride = 120,
        )
        repository.saveProfile(profileA)

        // Verify Game A persists overrides
        val savedA = repository.getEffectiveProfile(packageA)
        assertTrue(savedA.customized)
        assertFalse("Game A lockRefreshRate should be false", savedA.lockRefreshRate)
        assertEquals("Game A fpsOverride should be 120", 120, savedA.fpsOverride)

        // Verify Global defaults are completely unaffected
        assertTrue("Global lockRefreshRate must remain true", repository.isLockRefreshRate.value)
        assertEquals("Global fpsOverride must remain 0", 0, repository.fpsOverride.value)

        // Verify Game B remains unaffected and inherits global defaults
        val savedB = repository.getEffectiveProfile(packageB)
        assertFalse(savedB.customized)
        assertTrue("Game B must inherit global lockRefreshRate (true)", savedB.lockRefreshRate)
        assertEquals("Game B must inherit global fpsOverride (0)", 0, savedB.fpsOverride)
    }

    @Test
    fun modifyingSuspendedAppsOnGameDoesNotAffectGlobalOrOtherGames() = runTest {
        val packageA = "com.game.with.freezer"
        val packageB = "com.game.standard"
        val globalFrozen = setOf("com.global.background.sync", "com.global.analytics")

        // Set global suspended apps
        repository.setSuspendedApps(globalFrozen)
        assertEquals(globalFrozen, repository.suspendedApps.value)

        // Game A scopes a custom list of suspended apps
        val customSuspendedA = setOf("com.social.app", "com.streaming.service")
        val profileA = repository.getEffectiveProfile(packageA).copy(
            customized = true,
            suspendedApps = customSuspendedA,
        )
        repository.saveProfile(profileA)

        // Verify Game A has its custom suspended apps
        val savedA = repository.getEffectiveProfile(packageA)
        assertTrue(savedA.customized)
        assertEquals(customSuspendedA, savedA.suspendedApps)

        // Verify global suspended apps remain unchanged
        assertEquals("Global suspended apps must remain intact", globalFrozen, repository.suspendedApps.value)

        // Verify Game B inherits global suspended apps
        val savedB = repository.getEffectiveProfile(packageB)
        assertFalse(savedB.customized)
        assertEquals("Game B must inherit global suspended apps", globalFrozen, savedB.suspendedApps)
    }

    @Test
    fun uncustomizedGameTransitionsToCustomizedWhenOverrideSavedPreservingInheritedDefaults() = runTest {
        val testPackage = "com.game.autocustomize"

        // Initial state: uncustomized
        val initialProfile = repository.getEffectiveProfile(testPackage)
        assertFalse("Profile must initially be uncustomized", initialProfile.customized)
        assertTrue(initialProfile.lockRefreshRate)
        assertTrue(initialProfile.boostRam)
        assertTrue(initialProfile.wifiLowLatency)
        assertEquals(2, initialProfile.interventionMode)

        // User changes a single setting (boostRam = false) triggering auto-customization transition
        val updatedProfile = initialProfile.copy(
            customized = true,
            boostRam = false,
        )
        repository.saveProfile(updatedProfile)

        // Verify profile is now customized
        val effective = repository.getEffectiveProfile(testPackage)
        assertTrue("Profile should now have customized = true", effective.customized)
        assertFalse("boostRam override must be false", effective.boostRam)

        // Verify all other settings preserve their inherited default values
        assertTrue("lockRefreshRate must remain true", effective.lockRefreshRate)
        assertTrue("wifiLowLatency must remain true", effective.wifiLowLatency)
        assertEquals(2, effective.interventionMode)

        // Verify DAO holds customized entity
        val entity = dao.getProfile(testPackage)
        assertTrue(entity?.customized == true)
        assertFalse(entity?.boostRam == true)
    }

    @Test
    fun customizedGameIsolationAcrossAllOptimizationCategories() = runTest {
        val pkg = "com.game.deep.test"

        val customProfile = GameProfile(
            packageName = pkg,
            customized = true,
            lockRefreshRate = false,
            disableAutoBrightness = false,
            boostRam = false,
            cachedFreezer = false,
            disableQuickTap = false,
            interventionMode = 3, // Battery mode
            stabilizeWifi = false,
            wifiLowLatency = false,
            wifiSuppressScans = false,
            wifiLockSufficiency = false,
            wifiPreventArp = false,
            blockBatterySaver = false,
            pauseAutoSync = false,
            disablePrivateDns = false,
            suspendedApps = setOf("com.isolated.target"),
        )
        repository.saveProfile(customProfile)

        val retrieved = repository.getEffectiveProfile(pkg)
        assertTrue(retrieved.customized)
        assertFalse(retrieved.lockRefreshRate)
        assertFalse(retrieved.disableAutoBrightness)
        assertFalse(retrieved.boostRam)
        assertFalse(retrieved.cachedFreezer)
        assertFalse(retrieved.disableQuickTap)
        assertEquals(3, retrieved.interventionMode)
        assertFalse(retrieved.stabilizeWifi)
        assertFalse(retrieved.wifiLowLatency)
        assertFalse(retrieved.wifiSuppressScans)
        assertFalse(retrieved.wifiLockSufficiency)
        assertFalse(retrieved.wifiPreventArp)
        assertFalse(retrieved.blockBatterySaver)
        assertFalse(retrieved.pauseAutoSync)
        assertFalse(retrieved.disablePrivateDns)
        assertEquals(setOf("com.isolated.target"), retrieved.suspendedApps)

        // All global repository settings remain in their default states
        assertTrue(repository.isLockRefreshRate.value)
        assertTrue(repository.isDisableAutoBrightness.value)
        assertTrue(repository.isBoostRam.value)
        assertTrue(repository.isCachedFreezer.value)
        assertTrue(repository.isDisableQuickTap.value)
        assertEquals(2, repository.interventionMode.value)
        assertTrue(repository.isStabilizeWifi.value)
        assertTrue(repository.isWifiLowLatency.value)
        assertTrue(repository.isWifiSuppressScans.value)
        assertTrue(repository.isWifiLockSufficiency.value)
        assertTrue(repository.isWifiPreventArp.value)
        assertTrue(repository.isBlockBatterySaver.value)
        assertTrue(repository.isPauseAutoSync.value)
        assertTrue(repository.isDisablePrivateDns.value)
    }

    @Test
    fun resettingCustomizedGameRestoresInheritanceFromGlobalDefaults() = runTest {
        val pkg = "com.game.reset"

        // Customize game with overrides
        val custom = GameProfile(
            packageName = pkg,
            customized = true,
            lockRefreshRate = false,
            boostRam = false,
            fpsOverride = 60,
        )
        repository.saveProfile(custom)

        var loaded = repository.getEffectiveProfile(pkg)
        assertTrue(loaded.customized)
        assertFalse(loaded.lockRefreshRate)

        // Reset to global defaults
        repository.saveProfile(repository.createDefaultEffectiveProfile(pkg))
        repository.setProfileCustomized(pkg, false)

        loaded = repository.getEffectiveProfile(pkg)
        assertFalse("Profile must be uncustomized after reset", loaded.customized)
        assertTrue("Must inherit global lockRefreshRate", loaded.lockRefreshRate)
        assertTrue("Must inherit global boostRam", loaded.boostRam)

        // Global change dynamically flows to the resetted game again
        repository.setLockRefreshRate(false)
        val dynamicCheck = repository.getEffectiveProfile(pkg)
        assertFalse("Must dynamically inherit changed global setting", dynamicCheck.lockRefreshRate)
    }

    @Test
    fun observeEffectiveProfileFlowEmitsScopedOverridesAccurately() = runTest {
        val pkg = "com.game.reactive"

        // Collect initial uncustomized flow
        val initial = repository.observeEffectiveProfile(pkg).first()
        assertFalse(initial.customized)
        assertTrue(initial.lockRefreshRate)

        // Save customized override
        repository.saveProfile(initial.copy(customized = true, lockRefreshRate = false))

        // Flow emits updated customized profile
        val updated = repository.observeEffectiveProfile(pkg).first()
        assertTrue(updated.customized)
        assertFalse(updated.lockRefreshRate)
    }
}
