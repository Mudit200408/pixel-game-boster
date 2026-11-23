package com.pixel.gamebooster

import android.content.SharedPreferences
import com.pixel.gamebooster.data.repository.SettingsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        repository = SettingsRepository(fakePrefs)
    }

    @Test
    fun defaultOnboardingStateIsFalse() {
        assertFalse(repository.hasCompletedOnboarding.value)
        assertFalse(fakePrefs.getBoolean(SettingsRepository.KEY_HAS_COMPLETED_ONBOARDING, false))
    }

    @Test
    fun setCompletedOnboardingUpdatesStateFlowAndPreferences() {
        repository.setCompletedOnboarding(true)

        assertTrue(repository.hasCompletedOnboarding.value)
        assertTrue(fakePrefs.getBoolean(SettingsRepository.KEY_HAS_COMPLETED_ONBOARDING, false))
    }

    @Test
    fun setCompletedOnboardingCanReset() {
        repository.setCompletedOnboarding(true)
        assertTrue(repository.hasCompletedOnboarding.value)

        repository.setCompletedOnboarding(false)
        assertFalse(repository.hasCompletedOnboarding.value)
        assertFalse(fakePrefs.getBoolean(SettingsRepository.KEY_HAS_COMPLETED_ONBOARDING, true))
    }

    @Test
    fun setHasCompletedOnboardingDelegatesCorrectly() {
        repository.setHasCompletedOnboarding(true)
        assertTrue(repository.hasCompletedOnboarding.value)
        assertTrue(fakePrefs.getBoolean(SettingsRepository.KEY_HAS_COMPLETED_ONBOARDING, false))

        repository.setHasCompletedOnboarding(false)
        assertFalse(repository.hasCompletedOnboarding.value)
    }

    class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

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

        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {}

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {}

        class FakeEditor(private val backingMap: MutableMap<String, Any?>) : SharedPreferences.Editor {
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
                for ((key, value) in tempMap) {
                    if (value === this) {
                        backingMap.remove(key)
                    } else {
                        backingMap[key] = value
                    }
                }
            }
        }
    }
}
