package com.pixel.gamebooster

import android.content.pm.ApplicationInfo
import com.pixel.gamebooster.utils.GameDetectionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameDetectionUtilsTest {

    @Before
    fun setUp() {
        GameDetectionUtils.clearCache()
    }

    @Test
    fun verifyCategoryGameReturnsTrue() {
        assertTrue(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_GAME,
                flags = 0,
            )
        )
    }

    @Test
    fun verifyFlagIsGameReturnsTrue() {
        @Suppress("DEPRECATION")
        assertTrue(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_UNDEFINED,
                flags = ApplicationInfo.FLAG_IS_GAME,
            )
        )
    }

    @Test
    fun verifyNonGameCategoriesReturnFalse() {
        assertFalse(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_AUDIO,
                flags = 0,
            )
        )
        assertFalse(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_MAPS,
                flags = 0,
            )
        )
        assertFalse(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_VIDEO,
                flags = 0,
            )
        )
        assertFalse(
            GameDetectionUtils.isGameApplicationInfo(
                category = ApplicationInfo.CATEGORY_UNDEFINED,
                flags = 0,
            )
        )
    }

    @Test
    fun verifyNullCategoryAndZeroFlagsReturnsFalse() {
        assertFalse(
            GameDetectionUtils.isGameApplicationInfo(
                category = null,
                flags = 0,
            )
        )
    }

    @Test
    fun verifyManualGameSetOverride() {
        val customGamePkg = "com.custom.unclassified.game"
        val manualGames = setOf(customGamePkg)

        // Manual game set should return true even with null context and no PackageManager lookup
        assertTrue(
            GameDetectionUtils.isGame(
                context = null,
                packageName = customGamePkg,
                manualGameApps = manualGames,
            )
        )

        // Non-manual app without context returns false
        assertFalse(
            GameDetectionUtils.isGame(
                context = null,
                packageName = "com.unclassified.otherapp",
                manualGameApps = manualGames,
            )
        )
    }

    @Test
    fun verifyBlankAndEmptyPackageNamesReturnFalse() {
        val manualGames = setOf("com.valid.game")
        assertFalse(GameDetectionUtils.isGame(null, null, manualGames))
        assertFalse(GameDetectionUtils.isGame(null, "", manualGames))
        assertFalse(GameDetectionUtils.isGame(null, "   ", manualGames))
    }

    @Test
    fun verifyCacheClearing() {
        val pkg = "com.test.cached.game"
        GameDetectionUtils.gameCache[pkg] = true
        GameDetectionUtils.labelCache[pkg] = "Cached Game Label"

        assertTrue(GameDetectionUtils.isGame(null, pkg))
        assertEquals("Cached Game Label", GameDetectionUtils.getAppLabel(null, pkg))

        GameDetectionUtils.clearCache()

        assertTrue(GameDetectionUtils.gameCache.isEmpty())
        assertTrue(GameDetectionUtils.labelCache.isEmpty())

        // After clearing cache, lookup with null context will return false and fallback label
        assertFalse(GameDetectionUtils.isGame(null, pkg))
        assertEquals(pkg, GameDetectionUtils.getAppLabel(null, pkg))
    }

    @Test
    fun verifyGetAppLabelFallbacks() {
        assertEquals("", GameDetectionUtils.getAppLabel(null, null))
        assertEquals("", GameDetectionUtils.getAppLabel(null, ""))
        assertEquals("com.example.app", GameDetectionUtils.getAppLabel(null, "com.example.app"))
    }

    @Test
    fun verifyKnownGamePackagesDetected() {
        val cod = "com.activision.callofduty.shooter"
        val pubg = "com.tencent.ig"
        val genshin = "com.miHoYo.GenshinImpact"
        val wildRift = "com.riotgames.league.wildrift"

        assertTrue(GameDetectionUtils.isKnownGamePackage(cod))
        assertTrue(GameDetectionUtils.isKnownGamePackage(pubg))
        assertTrue(GameDetectionUtils.isKnownGamePackage(genshin))
        assertTrue(GameDetectionUtils.isKnownGamePackage(wildRift))

        // Known games return true from isGame even without context or manual registration
        assertTrue(GameDetectionUtils.isGame(null, cod))
        assertTrue(GameDetectionUtils.isGame(null, pubg))
        assertTrue(GameDetectionUtils.isGame(null, genshin))
        assertTrue(GameDetectionUtils.isGame(null, wildRift))

        // Unknown package returns false
        assertFalse(GameDetectionUtils.isKnownGamePackage("com.random.utility.tool"))
        assertFalse(GameDetectionUtils.isKnownGamePackage(null))
        assertFalse(GameDetectionUtils.isKnownGamePackage(""))
    }
}
