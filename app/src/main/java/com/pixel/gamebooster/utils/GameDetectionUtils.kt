package com.pixel.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap

object GameDetectionUtils {

    @VisibleForTesting
    internal val gameCache = ConcurrentHashMap<String, Boolean>()

    @VisibleForTesting
    internal val labelCache = ConcurrentHashMap<String, String>()

    fun isGame(
        context: Context?,
        packageName: String?,
        manualGameApps: Set<String> = emptySet(),
    ): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (manualGameApps.contains(packageName)) return true

        gameCache[packageName]?.let { return it }

        if (context == null) return false

        val detected = queryIsGameFromPackageManager(context, packageName)
        gameCache[packageName] = detected
        return detected
    }

    fun isGameApplicationInfo(category: Int?, flags: Int): Boolean {
        val isCategoryGame = (Build.VERSION.SDK_INT == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
            category == ApplicationInfo.CATEGORY_GAME

        @Suppress("DEPRECATION")
        val isFlagGame = (flags and ApplicationInfo.FLAG_IS_GAME) != 0

        return isCategoryGame || isFlagGame
    }

    private fun queryIsGameFromPackageManager(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            isGameApplicationInfo(appInfo.category, appInfo.flags)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun getAppLabel(context: Context?, packageName: String?): String {
        if (packageName.isNullOrBlank()) return ""
        labelCache[packageName]?.let { return it }

        if (context == null) return packageName

        val label = try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        labelCache[packageName] = label
        return label
    }

    fun clearCache() {
        gameCache.clear()
        labelCache.clear()
    }
}
