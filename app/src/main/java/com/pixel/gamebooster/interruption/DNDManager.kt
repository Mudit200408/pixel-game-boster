package com.pixel.gamebooster.interruption

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import com.pixel.gamebooster.MainActivity
import com.pixel.gamebooster.R
import com.pixel.gamebooster.utils.ShellUtils

object DNDManager {
    private const val TAG = "PixelGameBooster:DND"
    const val PREFS_NAME = "pixel_game_booster_dnd_snapshot"

    const val KEY_SNAPSHOT_ACTIVE = "snapshot_active"
    const val KEY_SNAPSHOT_FILTER = "snapshot_filter"
    const val KEY_GAMING_RULE_ID = "gaming_automatic_zen_rule_id"
    const val KEY_SHIZUKU_FALLBACK_ACTIVE = "shizuku_fallback_active"
    const val KEY_RULE_VERSION = "gaming_rule_version"
    const val CURRENT_RULE_VERSION = 10

    val CONDITION_URI: Uri = Uri.parse("condition://com.pixel.gamebooster/gaming")
    const val RULE_NAME = "Game Booster"

    @Volatile
    var snapshottedFilter: Int? = null
        internal set

    @Volatile
    var cachedRuleId: String? = null
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
            didApplyShizukuFallback
    }

    /**
     * Returns Shizuku CLI command for shell DND fallback.
     */
    fun getShizukuCommand(enabled: Boolean): String {
        return if (enabled) "cmd notification set_dnd priority" else "cmd notification set_dnd off"
    }

    fun getShizukuCommand(mode: Int): String {
        return getShizukuCommand(mode != 0)
    }

    /**
     * Finds or creates our official "Gaming" AutomaticZenRule.
     * When registered, Android adds "Gaming" to the system Modes sheet and Settings > Modes.
     */
    fun getOrCreateGamingZenRuleId(context: Context): String? {
        val prefs = getPrefs(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        Log.d(TAG, "getOrCreateGamingZenRuleId called. nm=${nm != null}, accessGranted=${nm?.isNotificationPolicyAccessGranted}")

        // Migrate/refresh rule if older version lacked system icon metadata or used old name
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val currentVersion = prefs.getInt(KEY_RULE_VERSION, 0)
            if (currentVersion < CURRENT_RULE_VERSION) {
                Log.d(TAG, "Migrating rule version from $currentVersion to $CURRENT_RULE_VERSION")
                cachedRuleId = null
                val savedId = prefs.getString(KEY_GAMING_RULE_ID, null)
                try {
                    val existingRules = nm.automaticZenRules
                    Log.d(TAG, "Existing rules count: ${existingRules.size}, keys: ${existingRules.keys}")
                    existingRules.forEach { (id, r) ->
                        Log.d(TAG, "Checking rule $id: name=${r.name}, conditionId=${r.conditionId}")
                        if (r.conditionId == CONDITION_URI || r.name == "Gaming" || r.name == "Game Booster" || r.name == "Game Booster DND" || id == savedId) {
                            val removed = nm.removeAutomaticZenRule(id)
                            Log.d(TAG, "removeAutomaticZenRule($id) returned $removed")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed removing old rules", e)
                }
                prefs.edit().putInt(KEY_RULE_VERSION, CURRENT_RULE_VERSION).remove(KEY_GAMING_RULE_ID).apply()
            }
        }

        var savedId = prefs.getString(KEY_GAMING_RULE_ID, null)
        if (nm == null || !nm.isNotificationPolicyAccessGranted) {
            return cachedRuleId ?: savedId
        }

        try {
            val rules = nm.automaticZenRules
            cachedRuleId?.let { id ->
                if (rules.containsKey(id) && rules[id]?.name == RULE_NAME) {
                    return id
                } else {
                    cachedRuleId = null
                }
            }

            val zenPolicy = ZenPolicy.Builder()
                .allowAlarms(true)
                .allowMedia(true)
                .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
                .allowRepeatCallers(true)
                .showPeeking(false)
                .showFullScreenIntent(false)
                .build()

            val rule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                AutomaticZenRule.Builder(RULE_NAME, CONDITION_URI)
                    .setType(AutomaticZenRule.TYPE_IMMERSIVE)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
                    .setZenPolicy(zenPolicy)
                    .setManualInvocationAllowed(true)
                    .setIconResId(R.drawable.ic_game_console)
                    .build()
            } else {
                AutomaticZenRule(
                    RULE_NAME,
                    null,
                    ComponentName(context, MainActivity::class.java),
                    CONDITION_URI,
                    zenPolicy,
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                    true,
                )
            }

            // Check if our rule already exists
            if (!savedId.isNullOrBlank() && rules.containsKey(savedId)) {
                val existingRule = rules[savedId]
                if (existingRule?.name == RULE_NAME) {
                    cachedRuleId = savedId
                    runCatching { nm.updateAutomaticZenRule(savedId, rule) }
                    return savedId
                }
            }

            // Look up by condition URI
            val existing = rules.entries.find { it.value.conditionId == CONDITION_URI }
            if (existing != null && existing.value.name == RULE_NAME) {
                cachedRuleId = existing.key
                prefs.edit().putString(KEY_GAMING_RULE_ID, existing.key).apply()
                runCatching { nm.updateAutomaticZenRule(existing.key, rule) }
                return existing.key
            }

            val newRuleId = nm.addAutomaticZenRule(rule)
            if (!newRuleId.isNullOrBlank()) {
                cachedRuleId = newRuleId
                prefs.edit().putString(KEY_GAMING_RULE_ID, newRuleId).apply()
                Log.i(TAG, "Successfully registered Gaming AutomaticZenRule with ID: $newRuleId")
                return newRuleId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or create Gaming AutomaticZenRule", e)
        }

        return savedId
    }

    /**
     * Opens Android's native configuration page for this specific Gaming Mode.
     */
    fun openGameDashboardPrioritiesSettings(context: Context) {
        val ruleId = getOrCreateGamingZenRuleId(context)
        if (!ruleId.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val intent = Intent(Settings.ACTION_AUTOMATIC_ZEN_RULE_SETTINGS).apply {
                putExtra(Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID, ruleId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val launched = runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)

            if (launched) return
        }

        // Fallback to general Zen Priority Settings
        val fallbackIntent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(fallbackIntent)
        }.onFailure {
            val accessIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(accessIntent) }
        }
    }

    fun openSystemPrioritySettings(context: Context) = openGameDashboardPrioritiesSettings(context)

    /**
     * Activates the official Gaming Mode.
     * Android will show "Gaming: On" with the Gamepad icon in the Modes sheet.
     */
    fun applyInterruptionPolicy(context: Context, enabled: Boolean = true) {
        if (!enabled) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm == null || !nm.isNotificationPolicyAccessGranted) {
            applyShizukuFallback(context)
            return
        }

        var ruleId = getOrCreateGamingZenRuleId(context)
        if (!ruleId.isNullOrBlank()) {
            try {
                val condition = Condition(CONDITION_URI, "Gaming active", Condition.STATE_TRUE)
                nm.setAutomaticZenRuleState(ruleId, condition)
                persistActiveState(context, isShizuku = false)
                Log.d(TAG, "Activated Gaming Mode via setAutomaticZenRuleState($ruleId, TRUE)")
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAutomaticZenRuleState failed for $ruleId; recreating rule and retrying", e)
                cachedRuleId = null
                getPrefs(context).edit().remove(KEY_GAMING_RULE_ID).apply()
                ruleId = getOrCreateGamingZenRuleId(context)
                if (!ruleId.isNullOrBlank()) {
                    try {
                        val condition = Condition(CONDITION_URI, "Gaming active", Condition.STATE_TRUE)
                        nm.setAutomaticZenRuleState(ruleId, condition)
                        persistActiveState(context, isShizuku = false)
                        Log.d(TAG, "Activated Gaming Mode on retry via setAutomaticZenRuleState($ruleId, TRUE)")
                        return
                    } catch (retryEx: Exception) {
                        Log.e(TAG, "Retry setAutomaticZenRuleState failed for $ruleId", retryEx)
                    }
                }
            }
        } else {
            Log.e(TAG, "Unable to activate Game Booster mode: ruleId is null")
        }
    }

    fun applyInterruptionPolicy(context: Context, mode: Int) {
        if (mode != 0) {
            applyInterruptionPolicy(context, true)
        }
    }

    /**
     * Deactivates the official Gaming Mode on game exit.
     */
    fun restoreInterruptionPolicy(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            val ruleId = getOrCreateGamingZenRuleId(context)
            if (!ruleId.isNullOrBlank()) {
                runCatching {
                    val condition = Condition(CONDITION_URI, "Gaming ended", Condition.STATE_FALSE)
                    nm.setAutomaticZenRuleState(ruleId, condition)
                    Log.d(TAG, "Deactivated Gaming Mode via setAutomaticZenRuleState($ruleId, FALSE)")
                }.onFailure { Log.e(TAG, "Failed to deactivate Gaming Mode rule", it) }
            }

            snapshottedFilter?.let { filter ->
                runCatching { nm.setInterruptionFilter(filter) }
            }
        } else {
            val cmd = getShizukuCommand(false)
            ShellUtils.runCommand(context, cmd)
            Log.d(TAG, "Restored DND via Shizuku: $cmd")
        }

        snapshottedFilter = null
        didApplyShizukuFallback = false
        clearSnapshot(context)
    }

    fun recoverLeakedDndState(context: Context) {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_SNAPSHOT_ACTIVE, false)) {
            return
        }

        Log.w(TAG, "Detected dangling DND state from prior crashed session. Recovering...")
        restoreInterruptionPolicy(context)
    }

    private fun applyShizukuFallback(context: Context) {
        if (!getPrefs(context).getBoolean(KEY_SNAPSHOT_ACTIVE, false)) {
            persistShizukuSnapshot(context)
        }
        didApplyShizukuFallback = true
        val cmd = getShizukuCommand(true)
        ShellUtils.runCommand(context, cmd)
        Log.d(TAG, "Executed Shizuku fallback DND command: $cmd")
    }

    internal fun persistActiveState(context: Context, isShizuku: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SNAPSHOT_ACTIVE, true)
            putBoolean(KEY_SHIZUKU_FALLBACK_ACTIVE, isShizuku)
            apply()
        }
    }

    internal fun persistSnapshot(context: Context, filter: Int, isShizuku: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_SNAPSHOT_ACTIVE, true)
            putInt(KEY_SNAPSHOT_FILTER, filter)
            putBoolean(KEY_SHIZUKU_FALLBACK_ACTIVE, isShizuku)
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

    internal fun clearSnapshot(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_SNAPSHOT_ACTIVE)
            remove(KEY_SNAPSHOT_FILTER)
            remove(KEY_SHIZUKU_FALLBACK_ACTIVE)
            apply()
        }
    }
}
