package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CampaignProfileEntity::class,
        VillageBuildingEntity::class,
        OutpostEntity::class,
        WarriorEntity::class,
        HistoryEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LostEarthDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: LostEarthDatabase? = null

        fun getInstance(context: Context): LostEarthDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LostEarthDatabase::class.java,
                    "lost_earth_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
