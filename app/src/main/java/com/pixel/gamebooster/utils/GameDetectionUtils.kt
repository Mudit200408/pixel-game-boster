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

    /**
     * Common known game package names that may lack CATEGORY_GAME or FLAG_IS_GAME metadata.
     */
    val KNOWN_GAME_PACKAGES = setOf(
        "com.activision.callofduty.shooter",
        "com.garena.game.codm",
        "com.tencent.ig",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.pubg.imobile",
        "com.miHoYo.GenshinImpact",
        "com.mihoyo.genshinimpact",
        "com.hoyoverse.genshinimpact",
        "com.HoYoverse.hkrpgoversea",
        "com.miHoYo.hkrpg",
        "com.miHoYo.bh3global",
        "com.miHoYo.bh3oversea",
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.riotgames.league.wildrift",
        "com.mobile.legends",
        "com.supercell.brawlstars",
        "com.supercell.clashofclans",
        "com.supercell.clashroyale",
        "com.mojang.minecraftpe",
        "com.roblox.client",
        "com.gameloft.android.ANMP.GloftA9HM",
        "com.gameloft.android.ANMP.GloftA8HM",
        "com.epicgames.fortnite",
        "com.epicgames.portal",
        "com.ea.gp.fifamobile",
        "com.ea.gp.apexlegendsmobilefps",
        "com.nianticlabs.pokemongo",
        "jp.pokemon.pokemonunite",
        "com.blizzard.diablo.immortal",
        "com.kiloo.subwaysurf",
        "com.king.candycrushsaga",
        "com.innersloth.spacemafia",
        "com.netease.dbdena",
        "com.ngame.allstar.eu",
        "com.ea.games.r3_row",
        "com.ea.game.nfs14_row",
        "com.nvsgames.snap",
        "com.madfingergames.legends",
    )

    fun isKnownGamePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return KNOWN_GAME_PACKAGES.contains(packageName)
    }

    fun isGame(
        context: Context?,
        packageName: String?,
        manualGameApps: Set<String> = emptySet(),
    ): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (manualGameApps.contains(packageName)) return true
        if (isKnownGamePackage(packageName)) return true

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

    /**
     * Scans installed applications to find games using category, flag, and known game fallbacks.
     * Filters out ignored packages and the app itself.
     */
    fun scanInstalledGames(
        context: Context,
        manualGameApps: Set<String> = emptySet(),
    ): List<ApplicationInfo> {
        val pm = context.packageManager
        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
        }.getOrDefault(emptyList())

        return installed.filter { appInfo ->
            val pkg = appInfo.packageName
            if (pkg == context.packageName) return@filter false
            if (PackageFilterUtils.isPackageIgnored(pkg, context, manualGameApps)) return@filter false

            manualGameApps.contains(pkg) ||
                isKnownGamePackage(pkg) ||
                isGameApplicationInfo(appInfo.category, appInfo.flags)
        }
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
