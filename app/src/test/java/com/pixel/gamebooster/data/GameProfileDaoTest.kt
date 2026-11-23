package com.pixel.gamebooster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixel.gamebooster.data.db.AppDatabase
import com.pixel.gamebooster.data.db.dao.GameProfileDao
import com.pixel.gamebooster.data.db.entity.GameProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameProfileDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GameProfileDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.gameProfileDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveProfileDirectly() = runTest {
        val entity = GameProfileEntity(
            packageName = "com.epicgames.fortnite",
            customized = true,
            fpsOverride = 90,
            lockRefreshRate = true,
            boostRam = true,
            suspendedApps = setOf("com.facebook.katana", "com.instagram.android"),
        )
        dao.upsertProfile(entity)

        val retrieved = dao.getProfile("com.epicgames.fortnite")
        assertNotNull(retrieved)
        assertEquals("com.epicgames.fortnite", retrieved?.packageName)
        assertTrue(retrieved?.customized == true)
        assertEquals(90, retrieved?.fpsOverride)
        assertEquals(setOf("com.facebook.katana", "com.instagram.android"), retrieved?.suspendedApps)
    }

    @Test
    fun observeProfileViaFlow() = runTest {
        val entity = GameProfileEntity(
            packageName = "com.miHoYo.GenshinImpact",
            customized = false,
            fpsOverride = 60,
        )
        dao.upsertProfile(entity)

        val flowResult = dao.getProfileFlow("com.miHoYo.GenshinImpact").first()
        assertNotNull(flowResult)
        assertEquals("com.miHoYo.GenshinImpact", flowResult?.packageName)
        assertEquals(60, flowResult?.fpsOverride)

        // Update and verify flow emits updated entity
        val updated = flowResult!!.copy(customized = true, fpsOverride = 120)
        dao.upsertProfile(updated)

        val updatedResult = dao.getProfileFlow("com.miHoYo.GenshinImpact").first()
        assertEquals(120, updatedResult?.fpsOverride)
        assertTrue(updatedResult?.customized == true)
    }

    @Test
    fun observeAllProfilesFlow() = runTest {
        val entity1 = GameProfileEntity(packageName = "com.game.one")
        val entity2 = GameProfileEntity(packageName = "com.game.two")

        dao.upsertProfile(entity1)
        dao.upsertProfile(entity2)

        val allProfiles = dao.getAllProfilesFlow().first()
        assertEquals(2, allProfiles.size)
        assertTrue(allProfiles.any { it.packageName == "com.game.one" })
        assertTrue(allProfiles.any { it.packageName == "com.game.two" })
    }

    @Test
    fun deleteProfileRemovesEntity() = runTest {
        val entity = GameProfileEntity(packageName = "com.pubg.imobile")
        dao.upsertProfile(entity)
        assertNotNull(dao.getProfile("com.pubg.imobile"))

        dao.deleteProfile(entity)
        assertNull(dao.getProfile("com.pubg.imobile"))
    }

    @Test
    fun deleteByPackageNameRemovesEntity() = runTest {
        val entity = GameProfileEntity(packageName = "com.riotgames.league.wildrift")
        dao.upsertProfile(entity)
        assertNotNull(dao.getProfile("com.riotgames.league.wildrift"))

        dao.deleteByPackageName("com.riotgames.league.wildrift")
        assertNull(dao.getProfile("com.riotgames.league.wildrift"))
    }
}
