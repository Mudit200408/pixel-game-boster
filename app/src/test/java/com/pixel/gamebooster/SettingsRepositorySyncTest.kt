package com.pixel.gamebooster

import android.content.SharedPreferences
import com.pixel.gamebooster.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositorySyncTest {

    private lateinit var sharedPrefs: SyncingFakeSharedPreferences
    private lateinit var repoA: SettingsRepository
    private lateinit var repoB: SettingsRepository

    @Before
    fun setUp() {
        sharedPrefs = SyncingFakeSharedPreferences()
        repoA = SettingsRepository(sharedPrefs)
        repoB = SettingsRepository(sharedPrefs)
    }

    @Test
    fun testBoosterEnabledSyncAcrossInstances() {
        repoA.setBoosterEnabled(true)
        assertTrue(repoB.isBoosterEnabled.value)
    }

    @Test
    fun testGameAppsSyncAcrossInstances() {
        repoA.addGame("com.activision.callofduty.shooter")
        assertTrue(repoB.gameApps.value.contains("com.activision.callofduty.shooter"))
    }

    @Test
    fun testAddGamesSyncAcrossInstances() {
        repoA.addGames(setOf("com.tencent.ig", "com.miHoYo.GenshinImpact"))
        assertTrue(repoB.gameApps.value.contains("com.tencent.ig"))
        assertTrue(repoB.gameApps.value.contains("com.miHoYo.GenshinImpact"))
    }

    @Test
    fun testInterventionModeSyncAcrossInstances() {
        repoA.setInterventionMode(1)
        assertEquals(1, repoB.interventionMode.value)
    }

    @Test
    fun testDisablePrivateDnsSyncAcrossInstances() {
        repoA.setDisablePrivateDns(false)
        assertEquals(false, repoB.isDisablePrivateDns.value)
    }

    @Test
    fun testTactileHapticsSyncAcrossInstances() {
        repoA.setTactileHaptics(false)
        assertEquals(false, repoB.isTactileHaptics.value)
        repoA.setTactileHaptics(true)
        assertEquals(true, repoB.isTactileHaptics.value)
    }

    @Test
    fun testGameModeToastSyncAcrossInstances() {
        repoA.setGameModeToastEnabled(false)
        assertEquals(false, repoB.isGameModeToastEnabled.value)
        repoA.setGameModeToastEnabled(true)
        assertEquals(true, repoB.isGameModeToastEnabled.value)
    }

    class SyncingFakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): Map<String, *> = HashMap(values)

        override fun getString(key: String, defValue: String?): String? =
            values[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
            (values[key] as? Set<String>) ?: defValues

        override fun getInt(key: String, defValue: Int): Int =
            values[key] as? Int ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            values[key] as? Long ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            values[key] as? Float ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = SyncingEditor(values, listeners, this)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            listeners.remove(listener)
        }

        class SyncingEditor(
            private val backingMap: MutableMap<String, Any?>,
            private val listeners: List<SharedPreferences.OnSharedPreferenceChangeListener>,
            private val sharedPreferences: SharedPreferences,
        ) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()
            private var shouldClear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                tempMap[key] = values?.toSet()
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                tempMap[key] = this // sentinel for remove
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                shouldClear = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (shouldClear) {
                    backingMap.clear()
                }
                val modifiedKeys = mutableListOf<String>()
                for ((key, value) in tempMap) {
                    if (value === this) {
                        backingMap.remove(key)
                    } else {
                        backingMap[key] = value
                    }
                    modifiedKeys.add(key)
                }

                // Notify all listeners
                for (key in modifiedKeys) {
                    for (listener in ArrayList(listeners)) {
                        listener.onSharedPreferenceChanged(sharedPreferences, key)
                    }
                }
            }
        }
    }
}
