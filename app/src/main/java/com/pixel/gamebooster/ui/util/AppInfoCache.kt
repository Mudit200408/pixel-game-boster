package com.pixel.gamebooster.ui.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

/**
 * In-memory LRU cache for resolved app labels and icon bitmaps.
 * Keeps PackageManager IPC and Drawable.toBitmap() off the main thread during composition.
 */
object AppInfoCache {
    private val cache = LruCache<String, CachedAppInfo>(128)

    private fun key(packageName: String, iconSizePx: Int): String = "$packageName@$iconSizePx"

    fun peek(packageName: String, iconSizePx: Int = 96): CachedAppInfo? {
        return cache.get(key(packageName, iconSizePx))
    }

    suspend fun get(context: Context, packageName: String, iconSizePx: Int = 96): CachedAppInfo? {
        peek(packageName, iconSizePx)?.let { return it }
        return withContext(Dispatchers.IO) {
            peek(packageName, iconSizePx)?.let { return@withContext it }
            runCatching {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                loadFromApplicationInfo(pm, appInfo, iconSizePx)
            }.getOrNull()
        }
    }

    suspend fun getAll(
        context: Context,
        packageNames: Collection<String>,
        iconSizePx: Int = 96,
    ): List<CachedAppInfo> {
        if (packageNames.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            packageNames.mapNotNull { pkg ->
                peek(pkg, iconSizePx) ?: runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    loadFromApplicationInfo(pm, appInfo, iconSizePx)
                }.getOrNull()
            }
        }
    }

    fun loadFromApplicationInfo(
        pm: PackageManager,
        appInfo: ApplicationInfo,
        iconSizePx: Int = 96,
    ): CachedAppInfo {
        val k = key(appInfo.packageName, iconSizePx)
        cache.get(k)?.let { return it }
        val label = pm.getApplicationLabel(appInfo).toString()
        val icon = runCatching {
            pm.getApplicationIcon(appInfo).toBitmap(iconSizePx, iconSizePx).asImageBitmap()
        }.getOrNull()
        val result = CachedAppInfo(
            packageName = appInfo.packageName,
            label = label,
            icon = icon,
        )
        cache.put(k, result)
        return result
    }
}

@Composable
fun rememberAppInfo(packageName: String, iconSizePx: Int = 96): CachedAppInfo? {
    val context = LocalContext.current
    val info by produceState(
        initialValue = AppInfoCache.peek(packageName, iconSizePx),
        packageName,
        iconSizePx,
    ) {
        value = AppInfoCache.get(context, packageName, iconSizePx)
    }
    return info
}
