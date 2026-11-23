package com.pixel.gamebooster.utils

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackGestureLockTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GameModeManager.restoreBackGestureLock(context)
    }

    @After
    fun tearDown() {
        GameModeManager.restoreBackGestureLock(context)
    }

    @Test
    fun setBackGestureLock_locksAndRestoresProperly() {
        // Initially not locked
        assertFalse(GameModeManager.isBackGestureSystemLocked(context))

        // Set left/right custom scale
        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_left", "1.0")
        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_right", "1.0")

        // Lock
        GameModeManager.setBackGestureLock(context, true)
        assertTrue(GameModeManager.isBackGestureLocked)
        assertTrue(GameModeManager.isBackGestureSystemLocked(context))

        // Restore
        GameModeManager.setBackGestureLock(context, false)
        assertFalse(GameModeManager.isBackGestureLocked)
    }

    @Test
    fun recoverLeakedBackGestureLock_recoversWhenSystemIsStuckAtMinusOne() {
        // Simulate stuck -1 from a previous crashed/killed session
        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_left", "-1")
        Settings.Secure.putString(context.contentResolver, "back_gesture_inset_scale_right", "-1")

        assertTrue(GameModeManager.isBackGestureSystemLocked(context))

        // Recover leaked lock
        GameModeManager.recoverLeakedBackGestureLock(context)

        // Should no longer be locked in GameModeManager
        assertFalse(GameModeManager.isBackGestureLocked)
    }
}
