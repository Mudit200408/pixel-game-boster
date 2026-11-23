package com.pixel.gamebooster.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixel.gamebooster.data.db.entity.GameProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName")
    fun getProfileFlow(packageName: String): Flow<GameProfileEntity?>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName")
    suspend fun getProfile(packageName: String): GameProfileEntity?

    @Query("SELECT * FROM game_profiles")
    fun getAllProfilesFlow(): Flow<List<GameProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: GameProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
