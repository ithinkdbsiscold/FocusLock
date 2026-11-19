package com.focuslock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_applications")
data class TrackedApplicationEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val lastSeenAtMillis: Long = System.currentTimeMillis()
)

/** A reconciled, absolute system usage snapshot for an app on a local calendar day. */
@Entity(
    tableName = "daily_usage",
    primaryKeys = ["usageDate", "packageName"],
    indices = [Index("usageDate"), Index("packageName")]
)
data class DailyUsageEntity(
    val usageDate: String,
    val packageName: String,
    val foregroundMillis: Long,
    val launchCount: Int = 0,
    val isBlocked: Boolean = false,
    val lastReconciledAtMillis: Long = System.currentTimeMillis()
)

/** Optional raw system-derived intervals, retained for diagnosis and future analytics. */
@Entity(tableName = "usage_sessions", indices = [Index("packageName"), Index("usageDate")])
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val usageDate: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val source: String = "usage_stats"
)

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val dailyLimitMillis: Long,
    val blockingEnabled: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocking_configuration")
data class BlockingConfigurationEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val strictModeEnabled: Boolean = false,
    /** Time in minutes after local midnight at which a new accounting day begins. */
    val resetMinutesAfterMidnight: Int = 0,
    val lastResetDate: String = "",
    val lastSuccessfulSyncMillis: Long = 0L
) {
    companion object { const val SINGLETON_ID = 1 }
}

data class AppUsageRow(
    val packageName: String,
    val displayName: String,
    val dailyLimitMillis: Long,
    val blockingEnabled: Boolean,
    val usageMillis: Long,
    val launchCount: Int,
    val isBlocked: Boolean
) {
    val remainingMillis: Long get() = (dailyLimitMillis - usageMillis).coerceAtLeast(0)
    val progress: Float get() = if (dailyLimitMillis <= 0) 0f else (usageMillis.toFloat() / dailyLimitMillis).coerceIn(0f, 1f)
}

data class DayTotal(val usageDate: String, val foregroundMillis: Long)

data class ManagedApp(
    val packageName: String,
    val displayName: String,
    val limitMillis: Long?,
    val enabled: Boolean
)

data class ChartBarData(
    val id: Int,
    val label: String,
    val shortLabel: String,
    val millis: Long
)

data class AppUsageStat(
    val packageName: String,
    val displayName: String,
    val usageMillis: Long,
    val percentage: Float
)

data class ScreenTimeReport(
    val periodTitle: String,
    val periodSubtitle: String,
    val totalMillis: Long,
    val chartBars: List<ChartBarData>,
    val appUsages: List<AppUsageStat>
)

enum class ScreenTimeTab {
    DAY,
    WEEK
}
