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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameProfileRepositoryTest {

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
    fun uncustomizedProfileInheritsGlobalSettings() = runTest {
        val effective = repository.getEffectiveProfile("com.example.uncustomized")

        assertFalse(effective.customized)
        assertEquals("com.example.uncustomized", effective.packageName)
        assertEquals(repository.isLockRefreshRate.value, effective.lockRefreshRate)
        assertEquals(repository.isBoostRam.value, effective.boostRam)
        assertEquals(repository.isCachedFreezer.value, effective.cachedFreezer)
        assertEquals(repository.isDisableAutoBrightness.value, effective.disableAutoBrightness)
        assertEquals(repository.isDisableQuickTap.value, effective.disableQuickTap)
        assertEquals(repository.isStabilizeWifi.value, effective.stabilizeWifi)
        assertEquals(repository.isWifiLowLatency.value, effective.wifiLowLatency)
        assertEquals(repository.fpsOverride.value, effective.fpsOverride)
        assertEquals(repository.interventionMode.value, effective.interventionMode)
        assertEquals(repository.suspendedApps.value, effective.suspendedApps)
    }

    @Test
    fun mutatingGlobalSettingsImmediatelyUpdatesEffectiveProfileForUncustomizedGames() = runTest {
        // Initial state check
        assertTrue(repository.isLockRefreshRate.value)
        var effective = repository.getEffectiveProfile("com.example.dynamic")
        assertTrue(effective.lockRefreshRate)

        // Mutate global settings
        repository.setLockRefreshRate(false)
        repository.setBoostRam(false)
        repository.setFpsOverride(90)

        // Verify effective profile dynamically reflects new global values
        effective = repository.getEffectiveProfile("com.example.dynamic")
        assertFalse(effective.lockRefreshRate)
        assertFalse(effective.boostRam)
        assertEquals(90, effective.fpsOverride)

        // Verify reactive Flow also emits the new effective profile
        val flowValue = repository.observeEffectiveProfile("com.example.dynamic").first()
        assertFalse(flowValue.lockRefreshRate)
        assertFalse(flowValue.boostRam)
        assertEquals(90, flowValue.fpsOverride)
    }

    @Test
    fun settingCustomizedTruePersistsOverridesThatDoNotChangeWhenGlobalSettingsChange() = runTest {
        // Create custom profile overriding defaults
        val customProfile = GameProfile(
            packageName = "com.example.customized",
            customized = true,
            fpsOverride = 120,
            lockRefreshRate = false,
            boostRam = false,
            stabilizeWifi = false,
        )
        repository.saveGameProfile(customProfile)

        // Mutate global settings to opposite values
        repository.setLockRefreshRate(true)
        repository.setBoostRam(true)
        repository.setStabilizeWifi(true)
        repository.setFpsOverride(60)

        // Verify custom profile retains its overrides
        val effective = repository.getEffectiveProfile("com.example.customized")
        assertTrue(effective.customized)
        assertEquals(120, effective.fpsOverride)
        assertFalse(effective.lockRefreshRate)
        assertFalse(effective.boostRam)
        assertFalse(effective.stabilizeWifi)

        // Verify Flow emits custom overrides
        val flowValue = repository.observeEffectiveProfile("com.example.customized").first()
        assertTrue(flowValue.customized)
        assertEquals(120, flowValue.fpsOverride)
        assertFalse(flowValue.lockRefreshRate)
    }

    @Test
    fun resettingCustomizedToFalseRestoresInheritanceFromGlobalSettings() = runTest {
        val customProfile = GameProfile(
            packageName = "com.example.reset",
            customized = true,
            fpsOverride = 120,
            lockRefreshRate = false,
        )
        repository.saveGameProfile(customProfile)

        // Set global lock refresh rate to true
        repository.setLockRefreshRate(true)
        repository.setFpsOverride(0)

        // Reset customization flag
        repository.setProfileCustomized("com.example.reset", false)

        val effective = repository.getEffectiveProfile("com.example.reset")
        assertFalse(effective.customized)
        assertTrue(effective.lockRefreshRate) // inherited from global
        assertEquals(0, effective.fpsOverride) // inherited from global
    }

    @Test
    fun deletingProfileResetsBackToDefaultFallbackBehavior() = runTest {
        val customProfile = GameProfile(
            packageName = "com.example.deleted",
            customized = true,
            fpsOverride = 144,
        )
        repository.saveGameProfile(customProfile)
        assertNotNull(repository.getGameProfile("com.example.deleted"))

        repository.deleteGameProfile("com.example.deleted")
        assertNull(repository.getGameProfile("com.example.deleted"))

        // Still resolvable as uncustomized fallback
        val effective = repository.getEffectiveProfile("com.example.deleted")
        assertFalse(effective.customized)
        assertEquals(repository.fpsOverride.value, effective.fpsOverride)
    }

    @Test
    fun updateLastPlayedPersistsTimestamp() = runTest {
        val timestamp = 1700000000000L
        repository.updateLastPlayed("com.example.played", timestamp)

        val profile = repository.getGameProfile("com.example.played")
        assertNotNull(profile)
        assertEquals(timestamp, profile?.lastPlayedTimestamp)

        val effective = repository.getEffectiveProfile("com.example.played")
        assertEquals(timestamp, effective.lastPlayedTimestamp)
    }
}
