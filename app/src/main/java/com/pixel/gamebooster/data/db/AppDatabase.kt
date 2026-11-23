package com.pixel.gamebooster.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pixel.gamebooster.data.db.dao.GameProfileDao
import com.pixel.gamebooster.data.db.entity.GameProfileEntity

@Database(
    entities = [GameProfileEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao

    companion object {
        const val DATABASE_NAME = "pixel_game_booster.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
