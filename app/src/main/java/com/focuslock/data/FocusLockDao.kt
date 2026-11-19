package com.focuslock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusLockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTrackedApp(app: TrackedApplicationEntity)

    @Query("SELECT * FROM tracked_applications ORDER BY displayName COLLATE NOCASE")
    fun observeTrackedApps(): Flow<List<TrackedApplicationEntity>>

    @Query("SELECT * FROM tracked_applications WHERE packageName = :packageName LIMIT 1")
    fun trackedApp(packageName: String): TrackedApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertLimit(limit: AppLimitEntity)

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun limit(packageName: String): AppLimitEntity?

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    fun removeLimit(packageName: String)

    @Query("SELECT * FROM app_limits")
    fun observeLimits(): Flow<List<AppLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertDailyUsage(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE usageDate = :date AND packageName = :packageName LIMIT 1")
    fun dailyUsage(date: String, packageName: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE usageDate = :date")
    fun observeDailyUsage(date: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE usageDate = :date")
    fun dailyUsageForDate(date: String): List<DailyUsageEntity>

    @Query("SELECT packageName FROM daily_usage WHERE usageDate = :date AND isBlocked = 1")
    fun observeBlockedPackages(date: String): Flow<List<String>>

    @Query("SELECT packageName FROM daily_usage WHERE usageDate = :date AND isBlocked = 1")
    fun blockedPackages(date: String): List<String>

    @Query("SELECT usageDate, SUM(foregroundMillis) AS foregroundMillis FROM daily_usage WHERE usageDate BETWEEN :startDate AND :endDate GROUP BY usageDate ORDER BY usageDate")
    fun observeDayTotals(startDate: String, endDate: String): Flow<List<DayTotal>>

    @Query("SELECT usageDate, SUM(foregroundMillis) AS foregroundMillis FROM daily_usage WHERE packageName = :packageName AND usageDate BETWEEN :startDate AND :endDate GROUP BY usageDate ORDER BY usageDate")
    fun observeAppDayTotals(packageName: String, startDate: String, endDate: String): Flow<List<DayTotal>>

    @Query("UPDATE daily_usage SET isBlocked = :blocked WHERE usageDate = :date AND packageName = :packageName")
    fun setBlocked(date: String, packageName: String, blocked: Boolean)

    @Query("UPDATE daily_usage SET isBlocked = 0 WHERE usageDate != :date")
    fun clearPriorBlockedStates(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: UsageSessionEntity)

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName AND usageDate BETWEEN :startDate AND :endDate ORDER BY startedAtMillis")
    fun observeSessions(packageName: String, startDate: String, endDate: String): Flow<List<UsageSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertConfiguration(configuration: BlockingConfigurationEntity)

    @Query("SELECT * FROM blocking_configuration WHERE id = 1 LIMIT 1")
    fun configuration(): BlockingConfigurationEntity?

    @Query("SELECT * FROM blocking_configuration WHERE id = 1 LIMIT 1")
    fun observeConfiguration(): Flow<BlockingConfigurationEntity?>

    @Query("DELETE FROM daily_usage")
    fun clearUsage()

    @Query("DELETE FROM usage_sessions")
    fun clearSessions()

    @Query("DELETE FROM app_limits")
    fun clearLimits()

    @Query("DELETE FROM tracked_applications")
    fun clearTrackedApps()
}
