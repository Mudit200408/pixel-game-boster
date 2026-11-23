package com.pixel.gamebooster

import com.pixel.gamebooster.utils.PackageFilterUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageFilterUtilsTest {

    @Test
    fun verifyKeyboardsIgnored() {
        assertTrue(PackageFilterUtils.isPackageIgnored("dev.jason.com.google.android.inputmethod.latin"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.google.android.inputmethod.latin"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.touchtype.swiftkey.keyboard"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.samsung.android.honeyboard")) // Samsung keyboard
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.inputmethod.latin"))
    }

    @Test
    fun verifySystemDialogsAndServicesIgnored() {
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.systemui"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.google.android.permissioncontroller"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.packageinstaller"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.google.android.gms"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.vending"))
        assertTrue(PackageFilterUtils.isPackageIgnored("android.service.games"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.google.android.play.games"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.google.android.dialer"))
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.incallui"))
    }

    @Test
    fun verifyGamesNotIgnored() {
        assertFalse(PackageFilterUtils.isPackageIgnored("com.activision.callofduty.shooter"))
        assertFalse(PackageFilterUtils.isPackageIgnored("com.ea.gp.apexlegendsmobilefps"))
        assertFalse(PackageFilterUtils.isPackageIgnored("com.miHoYo.GenshinImpact"))
        assertFalse(PackageFilterUtils.isPackageIgnored("com.dts.freefireth"))
        assertFalse(PackageFilterUtils.isPackageIgnored("com.epicgames.portal"))
    }

    @Test
    fun verifyConfiguredGameImmunity() {
        val gameWithKeyboardInName = "com.example.keyboardhero"
        val gameApps = setOf(gameWithKeyboardInName)

        // Without immunity, "keyboard" substring would cause it to be ignored
        assertTrue(PackageFilterUtils.isPackageIgnored(gameWithKeyboardInName, null, emptySet()))
        // With immunity, it is never ignored
        assertFalse(PackageFilterUtils.isPackageIgnored(gameWithKeyboardInName, null, gameApps))
    }

    @Test
    fun verifyNullOrBlankIgnored() {
        assertTrue(PackageFilterUtils.isPackageIgnored(null))
        assertTrue(PackageFilterUtils.isPackageIgnored(""))
        assertTrue(PackageFilterUtils.isPackageIgnored("   "))
    }

    @Test
    fun verifyNullContextSafe() {
        assertTrue(PackageFilterUtils.isPackageIgnored("com.android.systemui", null, emptySet()))
        assertFalse(PackageFilterUtils.isPackageIgnored("com.activision.callofduty.shooter", null, emptySet()))
    }
}
