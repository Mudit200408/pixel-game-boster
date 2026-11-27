package com.pixel.gamebooster.utils

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.pixel.gamebooster.services.GameBoosterAccessibilityService
import rikka.shizuku.Shizuku

object PermissionHelper {
    const val SHIZUKU_REQUEST_CODE = 1001
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val SHIZUKU_URL = "https://shizuku.rikka.app"

    data class ShizukuListenerHandle(
        val binderReceived: Shizuku.OnBinderReceivedListener,
        val binderDead: Shizuku.OnBinderDeadListener,
        val permissionResult: Shizuku.OnRequestPermissionResultListener,
    )

    fun initShizuku(onStateChanged: () -> Unit) {
        registerShizukuListeners(onStateChanged)
    }

    fun registerShizukuListeners(onStateChanged: () -> Unit): ShizukuListenerHandle? {
        return runCatching {
            val binderReceived = Shizuku.OnBinderReceivedListener { onStateChanged() }
            val binderDead = Shizuku.OnBinderDeadListener { onStateChanged() }
            val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> onStateChanged() }
            Shizuku.addBinderReceivedListener(binderReceived)
            Shizuku.addBinderDeadListener(binderDead)
            Shizuku.addRequestPermissionResultListener(permissionResult)
            ShizukuListenerHandle(binderReceived, binderDead, permissionResult)
        }.getOrNull()
    }

    fun unregisterShizukuListeners(handle: ShizukuListenerHandle?) {
        if (handle == null) return
        runCatching {
            Shizuku.removeBinderReceivedListener(handle.binderReceived)
            Shizuku.removeBinderDeadListener(handle.binderDead)
            Shizuku.removeRequestPermissionResultListener(handle.permissionResult)
        }
    }

    fun requestShizukuPermission() {
        if (ShellUtils.isShizukuAvailable()) {
            runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST_CODE) }
        }
    }

    private fun findShizukuPackage(context: Context): String? {
        val pm = context.packageManager
        if (pm.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null) {
            return SHIZUKU_PACKAGE
        }
        // Many Shizuku forks keep the standard provider authority even with a different package name
        val providerPkg = runCatching {
            pm.resolveContentProvider("$SHIZUKU_PACKAGE.shizuku", 0)?.packageName
        }.getOrNull()
        if (!providerPkg.isNullOrBlank() && pm.getLaunchIntentForPackage(providerPkg) != null) {
            return providerPkg
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val candidates = runCatching {
            pm.queryIntentActivities(launcherIntent, 0)
        }.getOrDefault(emptyList())
        return candidates
            .mapNotNull { it.activityInfo?.packageName }
            .firstOrNull { pkg ->
                pkg != context.packageName && pkg.contains("shizuku", ignoreCase = true)
            }
    }

    fun launchShizuku(context: Context) {
        val targetPackage = findShizukuPackage(context)
        val launchIntent = targetPackage?.let {
            context.packageManager.getLaunchIntentForPackage(it)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_URL)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (GameBoosterAccessibilityService.instance != null) return true
        val expectedComponentName = ComponentName(context, GameBoosterAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val componentName = ComponentName(context, GameBoosterAccessibilityService::class.java).flattenToString()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
                putExtra("android.provider.extra.ACCESSIBILITY_COMPONENT_NAME", componentName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        runCatching { context.startActivity(intent) }
    }

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun getDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun openDndSettings(context: Context) {
        runCatching {
            context.startActivity(getDndSettingsIntent())
        }
    }

    fun grantDndAccessViaShizuku(context: Context, onResult: (Boolean) -> Unit = {}) {
        val cmd = "cmd notification allow_dnd ${context.packageName}"
        ShellUtils.runCommand(context, cmd) { success ->
            onResult(success && hasDndAccess(context))
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun grantOverlayPermissionViaShizuku(context: Context, onResult: (Boolean) -> Unit) {
        val cmd = "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow"
        ShellUtils.runCommand(context, cmd) { success ->
            onResult(success && hasOverlayPermission(context))
        }
    }

    fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun openWriteSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun grantWriteSettingsViaShizuku(context: Context, onResult: (Boolean) -> Unit) {
        val cmd = "appops set ${context.packageName} WRITE_SETTINGS allow"
        ShellUtils.runCommand(context, cmd) { success ->
            onResult(success && hasWriteSettingsPermission(context))
        }
    }
}
