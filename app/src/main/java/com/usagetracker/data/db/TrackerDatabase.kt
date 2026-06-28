package com.usagetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.usagetracker.data.model.AppUsageEntity
import com.usagetracker.data.model.BrowserHistoryEntity

@Database(
    entities = [AppUsageEntity::class, BrowserHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TrackerDatabase : RoomDatabase() {

    abstract fun appUsageDao(): AppUsageDao
    abstract fun browserHistoryDao(): BrowserHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
