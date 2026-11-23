package com.pixel.gamebooster.interruption

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pixel.gamebooster.utils.ShellUtils

object DNDManager {
    private const val TAG = "PixelGameBooster:DND"
    const val PREFS_NAME = "pixel_game_booster_dnd_snapshot"

    const val PRIORITY_SENDERS_NONE = -1

    const val KEY_SNAPSHOT_ACTIVE = "snapshot_active"
    const val KEY_SNAPSHOT_FILTER = "snapshot_filter"
    const val KEY_SNAPSHOT_CATEGORIES = "snapshot_categories"
    const val KEY_SNAPSHOT_CALL_SENDERS = "snapshot_call_senders"
    const val KEY_SNAPSHOT_MSG_SENDERS = "snapshot_msg_senders"
    const val KEY_SNAPSHOT_SUPPRESSED_EFFECTS = "snapshot_suppressed_effects"
    const val KEY_SHIZUKU_FALLBACK_ACTIVE = "shizuku_fallback_active"

    @Volatile
    var snapshottedFilter: Int? = null
        internal set

    @Volatile
    var snapshottedPolicy: NotificationManager.Policy? = null
        internal set

    @Volatile
    var didApplyShizukuFallback: Boolean = false
        internal set

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasActiveSnapshot(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SNAPSHOT_ACTIVE, false) ||
            snapshottedFilter != null ||
            snapshottedPolicy != null ||
            didApplyShizukuFallback
    }

    /**
     * Build NotificationManager.Policy for the specified interruption mode.
     * Mode 1: Priority Only (Starred contacts, alarms, media, calls, suppressed peeking/FS intent/ambient/lights)
     * Mode 2: Full DND (Alarms only, no callers/messages, suppressed peeking/FS intent/ambient/lights)
     * Mode 0: Off / Inherit (null)
     */
    fun buildPolicyForMode(mode: Int): NotificationManager.Policy? {
        val suppressedEffects = NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS

        return when (mode) {
            1 -> {
                val categories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS

                NotificationManager.Policy(
                    categories,
                    NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                    PRIORITY_SENDERS_NONE,
                    suppressedEffects,
                )
            }
            2 -> {
                val categories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
                NotificationManager.Policy(
                    categories,
                    PRIORITY_SENDERS_NONE,
                    PRIORITY_SENDERS_NONE,
                    suppressedEffects,
                )
            }
            else -> null
        }
    }

    /**
     * Returns Shizuku CLI command for shell DND fallback.
     */
    fun getShizukuCommand(mode: Int): String {
        return when (mode) {
            1 -> "cmd notification set_dnd priority"
            2 -> "cmd notification set_dnd none"
            else -> "cmd notification set_dnd off"
        }
    }

    fun applyInterruptionPolicy(context: Context, mode: Int) {
        if (mode == 0) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm == null || !nm.isNotificationPolicyAccessGranted) {
            applyShizukuFallback(context, mode)
            return
        }

        // 1. Snapshot existing system filter and policy if not already snapshotted
        if (snapshottedFilter == null && !getPrefs(context).getBoolean(KEY_SNAPSHOT_ACTIVE, false)) {
            val currentFilter = nm.currentInterruptionFilter
            val currentPolicy = nm.notificationPolicy
            snapshottedFilter = currentFilter
            snapshottedPolicy = currentPolicy
            persistSnapshot(context, currentFilter, currentPolicy, isShizuku = false)
            Log.d(TAG, "Snapshotted filter: $currentFilter, policy: $currentPolicy")
        }

        // 2. Generate and apply target policy
        val policy = buildPolicyForMode(mode) ?: return
        try {
            nm.notificationPolicy = policy
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "Applied DND mode: $mode with SUPPRESSED_EFFECT_PEEK")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply notification policy", e)
        }
    }

    fun restoreInterruptionPolicy(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val filterToRestore = snapshottedFilter ?: getFilterFromPrefs(context)
            val policyToRestore = snapshottedPolicy ?: getPolicyFromPrefs(context)

            policyToRestore?.let {
                runCatching {
                    nm.notificationPolicy = it
                    Log.d(TAG, "Restored notificationPolicy")
                }.onFailure { Log.e(TAG, "Failed to restore notificationPolicy", it) }
            }

            filterToRestore?.let {
                runCatching {
                    nm.setInterruptionFilter(it)
                    Log.d(TAG, "Restored interruptionFilter: $it")
                }.onFailure { Log.e(TAG, "Failed to restore interruptionFilter", it) }
            }
        } else {
            val cmd = getShizukuCommand(0)
            ShellUtils.runCommand(context, cmd)
            Log.d(TAG, "Restored DND via Shizuku: $cmd")
        }

        snapshottedFilter = null
        snapshottedPolicy = null
        didApplyShizukuFallback = false
        clearSnapshot(context)
    }

    fun recoverLeakedDndState(context: Context) {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_SNAPSHOT_ACTIVE, false)) {
            return
        }

        Log.w(TAG, "Detected dangling DND state from prior crashed/abrupt session. Initiating recovery...")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val policyToRestore = getPolicyFromPrefs(context)
            val filterToRestore = getFilterFromPrefs(context)

            policyToRestore?.let {
                runCatching { nm.notificationPolicy = it }
            }
            filterToRestore?.let {
                runCatching { nm.setInterruptionFilter(it) }
            }
        } else {
            ShellUtils.runCommand(context, getShizukuCommand(0))
        }

        snapshottedFilter = null
        snapshottedPolicy = null
        didApplyShizukuFallback = false
        clearSnapshot(context)
        Log.i(TAG, "Recovered dangling DND state successfully")
    }

    private fun applyShizukuFallback(context: Context, mode: Int) {
        if (!getPrefs(context).getBoolean(KEY_SNAPSHOT_ACTIVE, false)) {
            persistShizukuSnapshot(context)
        }
        didApplyShizukuFallback = true
        val cmd = getShizukuCommand(mode)
        ShellUtils.runCommand(context, cmd)
        Log.d(TAG, "Executed Shizuku fallback DND command: $cmd")
    }

    internal fun persistSnapshot(
        context: Context,
        filter: Int,
        policy: NotificationManager.Policy?,
        isShizuku: Boolean,
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SNAPSHOT_ACTIVE, true)
            putInt(KEY_SNAPSHOT_FILTER, filter)
            putBoolean(KEY_SHIZUKU_FALLBACK_ACTIVE, isShizuku)
            if (policy != null) {
                putInt(KEY_SNAPSHOT_CATEGORIES, policy.priorityCategories)
                putInt(KEY_SNAPSHOT_CALL_SENDERS, policy.priorityCallSenders)
                putInt(KEY_SNAPSHOT_MSG_SENDERS, policy.priorityMessageSenders)
                putInt(KEY_SNAPSHOT_SUPPRESSED_EFFECTS, policy.suppressedVisualEffects)
            }
            apply()
        }
    }

    internal fun persistShizukuSnapshot(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SNAPSHOT_ACTIVE, true)
            putBoolean(KEY_SHIZUKU_FALLBACK_ACTIVE, true)
            apply()
        }
    }

    internal fun getFilterFromPrefs(context: Context): Int? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_SNAPSHOT_FILTER)) {
            prefs.getInt(KEY_SNAPSHOT_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        } else null
    }

    internal fun getPolicyFromPrefs(context: Context): NotificationManager.Policy? {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_SNAPSHOT_CATEGORIES)) return null

        val categories = prefs.getInt(KEY_SNAPSHOT_CATEGORIES, 0)
        val callSenders = prefs.getInt(KEY_SNAPSHOT_CALL_SENDERS, 0)
        val msgSenders = prefs.getInt(KEY_SNAPSHOT_MSG_SENDERS, 0)
        val suppressed = prefs.getInt(KEY_SNAPSHOT_SUPPRESSED_EFFECTS, 0)

        return NotificationManager.Policy(categories, callSenders, msgSenders, suppressed)
    }

    internal fun clearSnapshot(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
