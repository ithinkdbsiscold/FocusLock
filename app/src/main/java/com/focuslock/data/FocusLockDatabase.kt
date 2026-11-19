package com.focuslock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackedApplicationEntity::class,
        DailyUsageEntity::class,
        UsageSessionEntity::class,
        AppLimitEntity::class,
        BlockingConfigurationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun focusLockDao(): FocusLockDao

    companion object {
        fun create(context: Context): FocusLockDatabase = Room.databaseBuilder(
            context.applicationContext,
            FocusLockDatabase::class.java,
            "focuslock.db"
        ).fallbackToDestructiveMigration().build()
    }
}
