package com.focuslock.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FocusLockRepository(
    private val context: Context,
    database: FocusLockDatabase
) {
    private val dao = database.focusLockDao()
    private val usageStats: UsageStatsManager? = context.getSystemService()
    val preferences = FocusPreferences(context)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun today(configuration: BlockingConfigurationEntity = defaultConfiguration()): String =
        AccountingClock.accountingDate(resetMinutes = configuration.resetMinutesAfterMidnight).format(dateFormatter)

    private fun defaultConfiguration() = BlockingConfigurationEntity(
        lastResetDate = AccountingClock.accountingDate().format(dateFormatter)
    )

    suspend fun configuration(): BlockingConfigurationEntity = withContext(Dispatchers.IO) {
        val config = dao.configuration() ?: defaultConfiguration().also { dao.upsertConfiguration(it) }
        ensureCurrentDay(config)
    }

    fun observeConfiguration(): Flow<BlockingConfigurationEntity> = dao.observeConfiguration().combine(
        preferences.lastKnownZone
    ) { config, _ -> config ?: defaultConfiguration() }

    private suspend fun ensureCurrentDay(config: BlockingConfigurationEntity): BlockingConfigurationEntity = withContext(Dispatchers.IO) {
        val currentDate = today(config)
        if (config.lastResetDate == currentDate) return@withContext config
        val rolled = config.copy(lastResetDate = currentDate)
        dao.upsertConfiguration(rolled)
        // Older records remain for history; only the current accounting key is enforceable.
        dao.clearPriorBlockedStates(currentDate)
        preferences.recordTimeZone(ZoneId.systemDefault().id)
        rolled
    }

    suspend fun ensureCurrentDay() = ensureCurrentDay(configuration())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDashboard(): Flow<DashboardSnapshot> = dao.observeConfiguration()
        .map { it ?: defaultConfiguration() }
        .flatMapLatest { config ->
            combine(dao.observeLimits(), dao.observeDailyUsage(today(config)), dao.observeTrackedApps()) { limits, usage, apps ->
                DashboardSnapshot.from(config, limits, usage, apps)
            }
        }

    /** User-visible limited apps for today, updated from Room after every sync. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLimitedApps(): Flow<List<AppUsageRow>> = dao.observeConfiguration()
        .map { it ?: defaultConfiguration() }
        .flatMapLatest { config ->
            combine(dao.observeLimits(), dao.observeTrackedApps(), dao.observeDailyUsage(today(config))) { limits, apps, usage ->
                val usages = usage.associateBy { it.packageName }
                limits.mapNotNull { limit ->
                    val app = apps.firstOrNull { it.packageName == limit.packageName } ?: return@mapNotNull null
                    val day = usages[limit.packageName]
                    AppUsageRow(
                        packageName = limit.packageName,
                        displayName = app.displayName,
                        dailyLimitMillis = limit.dailyLimitMillis,
                        blockingEnabled = limit.blockingEnabled,
                        usageMillis = day?.foregroundMillis ?: 0,
                        launchCount = day?.launchCount ?: 0,
                        isBlocked = day?.isBlocked ?: false
                    )
                }.sortedByDescending { it.usageMillis }
            }
        }

    fun observeManagedApps(): Flow<List<ManagedApp>> = dao.observeTrackedApps().combine(dao.observeLimits()) { apps, limits ->
        val limitsByPackage = limits.associateBy { it.packageName }
        apps.map { app ->
            val limit = limitsByPackage[app.packageName]
            ManagedApp(app.packageName, app.displayName, limit?.dailyLimitMillis, limit?.blockingEnabled == true)
        }
    }

    fun observeHistory(days: Long = 30): Flow<List<DayTotal>> {
        val end = LocalDate.now()
        return dao.observeDayTotals(end.minusDays(days - 1).format(dateFormatter), end.format(dateFormatter))
    }

    fun observeAppDailyHistory(packageName: String, days: Long = 7): Flow<List<DayTotal>> {
        val end = LocalDate.now()
        return dao.observeAppDayTotals(packageName, end.minusDays(days - 1).format(dateFormatter), end.format(dateFormatter))
    }

    fun observeAppHistory(packageName: String, days: Long = 7): Flow<List<UsageSessionEntity>> {
        val end = LocalDate.now()
        return dao.observeSessions(packageName, end.minusDays(days - 1).format(dateFormatter), end.format(dateFormatter))
    }

    suspend fun hasUsageAccess(): Boolean = withContext(Dispatchers.Default) {
        val appOps = context.getSystemService<AppOpsManager>() ?: return@withContext false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Replaces a local daily value with Android's absolute aggregate instead of adding deltas.
     * This is the key deduplication guard: any retry or service restart is idempotent.
     */
    suspend fun reconcileToday(packageFilter: String? = null) = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext
        val config = ensureCurrentDay(configuration())
        val date = today(config)
        val day = LocalDate.parse(date, dateFormatter)
        val from = AccountingClock.dayStartMillis(day, config.resetMinutesAfterMidnight)
        val now = System.currentTimeMillis()

        // 1. Process usage events chronologically as the single source of truth
        val rawEvents = UsageTimelineProcessor.extractRawEvents(usageStats?.queryEvents(from, now))
        var packageTotals = UsageTimelineProcessor.aggregatePackageTotals(
            UsageTimelineProcessor.processRawEvents(rawEvents, from, now)
        )

        // 2. Fallback to INTERVAL_DAILY if raw events are not available on this device
        if (packageTotals.isEmpty()) {
            packageTotals = usageStats?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, from, now)
                .orEmpty()
                .filter { it.packageName !in UsageTimelineProcessor.EXCLUDED_PACKAGES }
                .groupBy { it.packageName }
                .mapValues { (_, values) -> values.maxByOrNull { it.totalTimeInForeground }?.totalTimeInForeground ?: 0L }
        }

        packageTotals.forEach { (packageName, foregroundTime) ->
            if (packageFilter != null && packageName != packageFilter) return@forEach
            val existing = dao.dailyUsage(date, packageName)
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = foregroundTime.coerceAtLeast(0L),
                    launchCount = existing?.launchCount ?: 0,
                    isBlocked = existing?.isBlocked ?: false,
                    lastReconciledAtMillis = now
                )
            )
        }
        dao.upsertConfiguration(config.copy(lastSuccessfulSyncMillis = now))
    }

    suspend fun addOrUpdateLimit(packageName: String, displayName: String, limitMillis: Long, enabled: Boolean = true) = withContext(Dispatchers.IO) {
        val config = configuration()
        check(!config.strictModeEnabled) { "Strict mode is active. Turn it off before changing limits." }
        val date = today(config)
        val cleanLimit = limitMillis.coerceAtLeast(60_000L)
        dao.upsertTrackedApp(TrackedApplicationEntity(packageName, displayName, isEnabled = enabled))
        dao.upsertLimit(AppLimitEntity(packageName, cleanLimit, enabled, updatedAtMillis = System.currentTimeMillis()))
        reconcileToday(packageName)

        val usage = dao.dailyUsage(date, packageName)
        val currentMillis = usage?.foregroundMillis ?: 0L
        val shouldBlock = enabled && (currentMillis >= cleanLimit)

        if (usage != null) {
            dao.upsertDailyUsage(usage.copy(isBlocked = shouldBlock, lastReconciledAtMillis = System.currentTimeMillis()))
        } else if (shouldBlock) {
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = currentMillis,
                    launchCount = 0,
                    isBlocked = true,
                    lastReconciledAtMillis = System.currentTimeMillis()
                )
            )
        }
        shouldBlock
    }

    suspend fun setLimitEnabled(packageName: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = configuration()
        check(!config.strictModeEnabled) { "Strict mode is active. Turn it off before changing restrictions." }
        val limit = dao.limit(packageName) ?: return@withContext
        val date = today(config)
        dao.upsertLimit(limit.copy(blockingEnabled = enabled, updatedAtMillis = System.currentTimeMillis()))
        val usage = dao.dailyUsage(date, packageName)
        val currentMillis = usage?.foregroundMillis ?: 0L
        val shouldBlock = enabled && (currentMillis >= limit.dailyLimitMillis)
        if (usage != null) {
            dao.upsertDailyUsage(usage.copy(isBlocked = shouldBlock, lastReconciledAtMillis = System.currentTimeMillis()))
        } else if (shouldBlock) {
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = currentMillis,
                    launchCount = 0,
                    isBlocked = true,
                    lastReconciledAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeLimit(packageName: String) = withContext(Dispatchers.IO) {
        val config = configuration()
        check(!config.strictModeEnabled) { "Strict mode is active. Turn it off before changing restrictions." }
        dao.removeLimit(packageName)
        val date = today(config)
        val existing = dao.dailyUsage(date, packageName)
        if (existing != null && existing.isBlocked) {
            dao.upsertDailyUsage(existing.copy(isBlocked = false, lastReconciledAtMillis = System.currentTimeMillis()))
        }
    }

    suspend fun getAppTrend(packageName: String): String = withContext(Dispatchers.IO) {
        val end = LocalDate.now()
        val history = dao.observeAppDayTotals(packageName, end.minusDays(6).format(dateFormatter), end.format(dateFormatter))
        // Calculate average of previous days vs today
        val config = configuration()
        val todayStr = today(config)
        val todayUsage = dao.dailyUsage(todayStr, packageName)?.foregroundMillis ?: 0L
        val pastDays = dao.dailyUsageForDate(todayStr).filter { it.packageName == packageName }
        
        val last7Days = (0L..6L).mapNotNull { daysAgo ->
            val d = end.minusDays(daysAgo).format(dateFormatter)
            dao.dailyUsage(d, packageName)?.foregroundMillis
        }
        if (last7Days.isEmpty() || last7Days.all { it == 0L }) return@withContext "Consistent daily usage"
        
        val avg = last7Days.average()
        if (avg <= 60_000.0) return@withContext "Typical usage"
        val diffPct = (((todayUsage - avg) / avg) * 100).toInt()
        when {
            diffPct >= 15 -> "↑ $diffPct% above 7-day average"
            diffPct <= -15 -> "↓ ${-diffPct}% below 7-day average"
            else -> "Consistent with 7-day average"
        }
    }

    suspend fun getScreenTimeForDay(date: LocalDate): ScreenTimeReport = withContext(Dispatchers.IO) {
        val config = configuration()
        val zone = ZoneId.systemDefault()
        val startOfDay = AccountingClock.dayStartMillis(date, config.resetMinutesAfterMidnight, zone)
        val endOfDay = AccountingClock.dayStartMillis(date.plusDays(1), config.resetMinutesAfterMidnight, zone)
        val now = System.currentTimeMillis()
        val queryEnd = minOf(endOfDay, now)

        val isToday = date == LocalDate.now()
        val isYesterday = date == LocalDate.now().minusDays(1)
        val title = when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        }
        val subtitle = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))

        if (!hasUsageAccess() || queryEnd <= startOfDay) {
            return@withContext ScreenTimeReport(title, subtitle, 0L, emptyList(), emptyList())
        }

        // 1. Process usage events chronologically as the single source of truth
        val rawEvents = UsageTimelineProcessor.extractRawEvents(usageStats?.queryEvents(startOfDay, queryEnd))
        val sessions = UsageTimelineProcessor.processRawEvents(rawEvents, startOfDay, queryEnd)
        var packageTotals = UsageTimelineProcessor.aggregatePackageTotals(sessions)
        val hourlyBuckets = UsageTimelineProcessor.distributeToHourlyBuckets(sessions, startOfDay, endOfDay, zone)

        // 2. Fallback to INTERVAL_DAILY if raw events are not available on this device
        if (packageTotals.isEmpty()) {
            packageTotals = usageStats?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, queryEnd)
                .orEmpty()
                .filter { it.packageName !in UsageTimelineProcessor.EXCLUDED_PACKAGES }
                .groupBy { it.packageName }
                .mapValues { (_, values) -> values.maxByOrNull { it.totalTimeInForeground }?.totalTimeInForeground ?: 0L }
                .filterValues { it > 0 }

            val totalFallback = packageTotals.values.sum()
            if (totalFallback > 0) {
                val bucketHour = if (isToday) java.time.LocalTime.now().hour.coerceIn(0, 23) else 12
                hourlyBuckets[bucketHour] = totalFallback
            }
        }

        // Total day millis must match the sum of hourly chart bars and sum of package totals exactly
        val totalDayMillis = packageTotals.values.sum()

        // Build 24 hourly chart bars
        val chartBars = (0..23).map { h ->
            val hour12 = when (h % 12) { 0 -> 12; else -> h % 12 }
            val amPm = if (h < 12) "AM" else "PM"
            val shortLabel = if (h % 4 == 0) "$hour12$amPm" else ""
            ChartBarData(
                id = h,
                label = "$hour12:00 $amPm",
                shortLabel = shortLabel,
                millis = hourlyBuckets[h]
            )
        }

        // App breakdown sorted descending
        val appUsages = packageTotals.entries
            .sortedByDescending { it.value }
            .map { (pkg, millis) ->
                val name = dao.trackedApp(pkg)?.displayName ?: pkg
                val pct = if (totalDayMillis > 0) (millis.toFloat() / totalDayMillis) else 0f
                AppUsageStat(pkg, name, millis, pct)
            }

        ScreenTimeReport(title, subtitle, totalDayMillis, chartBars, appUsages)
    }

    suspend fun getScreenTimeForWeek(startOfWeek: LocalDate): ScreenTimeReport = withContext(Dispatchers.IO) {
        val config = configuration()
        val zone = ZoneId.systemDefault()
        val endOfWeek = startOfWeek.plusDays(7)
        val startMillis = AccountingClock.dayStartMillis(startOfWeek, config.resetMinutesAfterMidnight, zone)
        val endMillis = AccountingClock.dayStartMillis(endOfWeek, config.resetMinutesAfterMidnight, zone)
        val now = System.currentTimeMillis()
        val queryEnd = minOf(endMillis, now)

        val rangeLabel = "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))} – ${endOfWeek.minusDays(1).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        val isCurrentWeek = !startOfWeek.isAfter(LocalDate.now()) && !endOfWeek.isBefore(LocalDate.now())
        val title = if (isCurrentWeek) "This Week" else rangeLabel

        if (!hasUsageAccess() || queryEnd <= startMillis) {
            return@withContext ScreenTimeReport(title, rangeLabel, 0L, emptyList(), emptyList())
        }

        // 1. Process usage events chronologically as the single source of truth
        val rawEvents = UsageTimelineProcessor.extractRawEvents(usageStats?.queryEvents(startMillis, queryEnd))
        val sessions = UsageTimelineProcessor.processRawEvents(rawEvents, startMillis, queryEnd)

        val dayMillisArray = LongArray(7) { 0L }
        val packageTotals = mutableMapOf<String, Long>()

        if (sessions.isNotEmpty()) {
            for (session in sessions) {
                packageTotals[session.packageName] = (packageTotals[session.packageName] ?: 0L) + session.duration
                val daySplits = UsageTimelineProcessor.splitSessionByDay(session, zone, config.resetMinutesAfterMidnight)
                for ((dayDate, dur) in daySplits) {
                    val dayIndex = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, dayDate).toInt()
                    if (dayIndex in 0..6) {
                        dayMillisArray[dayIndex] += dur
                    }
                }
            }
        } else {
            // Fallback to INTERVAL_DAILY if raw events are not available
            val dailyStats = usageStats?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMillis, queryEnd).orEmpty()
            for (stat in dailyStats) {
                if (stat.totalTimeInForeground <= 0 || stat.packageName in UsageTimelineProcessor.EXCLUDED_PACKAGES) continue
                val statDate = java.time.Instant.ofEpochMilli(stat.firstTimeStamp).atZone(zone).toLocalDate()
                val dayIndex = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, statDate).toInt()
                if (dayIndex in 0..6) {
                    dayMillisArray[dayIndex] += stat.totalTimeInForeground
                }
                packageTotals[stat.packageName] = (packageTotals[stat.packageName] ?: 0L) + stat.totalTimeInForeground
            }
        }

        // Weekly total strictly equals the sum of the 7 daily totals
        val totalWeekMillis = dayMillisArray.sum()

        val chartBars = (0..6).map { i ->
            val dayDate = startOfWeek.plusDays(i.toLong())
            val dayName = dayDate.format(DateTimeFormatter.ofPattern("EEE"))
            val fullLabel = dayDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            ChartBarData(
                id = i,
                label = fullLabel,
                shortLabel = dayName,
                millis = dayMillisArray[i]
            )
        }

        val appUsages = packageTotals.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (pkg, millis) ->
                val name = dao.trackedApp(pkg)?.displayName ?: pkg
                val pct = if (totalWeekMillis > 0) (millis.toFloat() / totalWeekMillis) else 0f
                AppUsageStat(pkg, name, millis, pct)
            }

        ScreenTimeReport(title, rangeLabel, totalWeekMillis, chartBars, appUsages)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeBlockedPackageNames(): Flow<Set<String>> = dao.observeConfiguration()
        .map { it ?: defaultConfiguration() }
        .flatMapLatest { config ->
            dao.observeBlockedPackages(today(config)).map { it.toSet() }
        }

    suspend fun getBlockedPackageNames(): Set<String> = withContext(Dispatchers.IO) {
        val config = ensureCurrentDay(configuration())
        dao.blockedPackages(today(config)).toSet()
    }

    suspend fun getAppName(packageName: String): String = withContext(Dispatchers.IO) {
        dao.trackedApp(packageName)?.displayName ?: packageName
    }

    suspend fun setBlocked(packageName: String, blocked: Boolean) = withContext(Dispatchers.IO) {
        val config = ensureCurrentDay(configuration())
        val date = today(config)
        val existing = dao.dailyUsage(date, packageName)
        if (existing != null) {
            dao.upsertDailyUsage(existing.copy(isBlocked = blocked, lastReconciledAtMillis = System.currentTimeMillis()))
        } else {
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = 0L,
                    launchCount = 0,
                    isBlocked = blocked,
                    lastReconciledAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addForegroundTime(packageName: String, additionalMillis: Long): Boolean = withContext(Dispatchers.IO) {
        if (additionalMillis <= 0) return@withContext false
        val config = ensureCurrentDay(configuration())
        val date = today(config)
        val limit = dao.limit(packageName) ?: return@withContext false
        if (!limit.blockingEnabled) return@withContext false

        val existing = dao.dailyUsage(date, packageName)
        val newTotal = (existing?.foregroundMillis ?: 0L) + additionalMillis
        val shouldBlock = BlockingPolicy.shouldBlock(
            limit.blockingEnabled,
            existing?.isBlocked == true,
            newTotal,
            limit.dailyLimitMillis
        )

        dao.upsertDailyUsage(
            DailyUsageEntity(
                usageDate = date,
                packageName = packageName,
                foregroundMillis = newTotal,
                launchCount = existing?.launchCount ?: 1,
                isBlocked = shouldBlock,
                lastReconciledAtMillis = System.currentTimeMillis()
            )
        )
        shouldBlock
    }

    suspend fun updateBlockedState(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val config = ensureCurrentDay(configuration())
        val date = today(config)
        val limit = dao.limit(packageName) ?: return@withContext false
        val usage = dao.dailyUsage(date, packageName)
        val shouldBlock = BlockingPolicy.shouldBlock(
            limit.blockingEnabled,
            usage?.isBlocked == true,
            usage?.foregroundMillis ?: 0L,
            limit.dailyLimitMillis
        )
        if (usage != null) {
            dao.upsertDailyUsage(usage.copy(isBlocked = shouldBlock))
        } else if (shouldBlock) {
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = 0L,
                    launchCount = 0,
                    isBlocked = true,
                    lastReconciledAtMillis = System.currentTimeMillis()
                )
            )
        }
        shouldBlock
    }

    suspend fun shouldBlock(packageName: String): BlockDecision = withContext(Dispatchers.IO) {
        val config = ensureCurrentDay(configuration())
        val date = today(config)
        val limit = dao.limit(packageName) ?: return@withContext BlockDecision.Allow
        if (!limit.blockingEnabled) return@withContext BlockDecision.Allow

        // 1. FAST CHECK: If already persisted as blocked for today, enforce block immediately!
        val existingUsage = dao.dailyUsage(date, packageName)
        if (existingUsage?.isBlocked == true) {
            val name = dao.trackedApp(packageName)?.displayName ?: packageName
            return@withContext BlockDecision.Block(
                name,
                AccountingClock.nextResetMillis(System.currentTimeMillis(), config.resetMinutesAfterMidnight)
            )
        }

        // 2. RECONCILE: Check latest usage against limit
        reconcileToday(packageName)
        val usage = dao.dailyUsage(date, packageName)
        val currentMillis = usage?.foregroundMillis ?: 0L
        val blocked = BlockingPolicy.shouldBlock(
            limit.blockingEnabled,
            usage?.isBlocked == true,
            currentMillis,
            limit.dailyLimitMillis
        )
        if (!blocked) return@withContext BlockDecision.Allow

        // 3. PERSIST: Save blocked state in Room
        if (usage != null) {
            dao.upsertDailyUsage(usage.copy(isBlocked = true))
        } else {
            dao.upsertDailyUsage(
                DailyUsageEntity(
                    usageDate = date,
                    packageName = packageName,
                    foregroundMillis = currentMillis,
                    launchCount = 0,
                    isBlocked = true,
                    lastReconciledAtMillis = System.currentTimeMillis()
                )
            )
        }

        val name = dao.trackedApp(packageName)?.displayName ?: packageName
        BlockDecision.Block(name, AccountingClock.nextResetMillis(System.currentTimeMillis(), config.resetMinutesAfterMidnight))
    }

    suspend fun setStrictMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        val config = configuration()
        dao.upsertConfiguration(config.copy(strictModeEnabled = enabled))
    }

    suspend fun setResetMinutes(minutes: Int) = withContext(Dispatchers.IO) {
        val config = configuration()
        check(!config.strictModeEnabled) { "Strict mode is active. Turn it off before changing reset time." }
        dao.upsertConfiguration(config.copy(resetMinutesAfterMidnight = minutes.coerceIn(0, 1439), lastResetDate = ""))
        ensureCurrentDay()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        check(!configuration().strictModeEnabled) { "Turn off strict mode before resetting data." }
        dao.clearUsage()
        dao.clearSessions()
        dao.clearLimits()
        dao.clearTrackedApps()
        dao.upsertConfiguration(BlockingConfigurationEntity())
        preferences.clear()
    }

    suspend fun seedTrackedApps() = withContext(Dispatchers.IO) {
        LauncherAppScanner(context).apps().forEach { app ->
            val old = dao.trackedApp(app.packageName)
            if (old == null) dao.upsertTrackedApp(TrackedApplicationEntity(app.packageName, app.label, isEnabled = false))
        }
    }
}

sealed interface BlockDecision {
    data object Allow : BlockDecision
    data class Block(val appName: String, val nextResetMillis: Long) : BlockDecision
}

data class DashboardSnapshot(
    val configuration: BlockingConfigurationEntity,
    val limits: List<AppLimitEntity>,
    val usage: List<DailyUsageEntity>,
    val appNames: Map<String, String> = emptyMap()
) {
    val totalMillis: Long get() = usage.sumOf { it.foregroundMillis }
    val totalLaunches: Int get() = usage.sumOf { it.launchCount }
    val restrictedCount: Int get() = limits.count { it.blockingEnabled }
    val blockedCount: Int get() = usage.count { it.isBlocked }
    val rows: List<AppUsageRow> get() = limits.map { limit ->
        val day = usage.firstOrNull { it.packageName == limit.packageName }
        AppUsageRow(
            packageName = limit.packageName,
            displayName = appNames[limit.packageName] ?: limit.packageName,
            dailyLimitMillis = limit.dailyLimitMillis,
            blockingEnabled = limit.blockingEnabled,
            usageMillis = day?.foregroundMillis ?: 0L,
            launchCount = day?.launchCount ?: 0,
            isBlocked = day?.isBlocked ?: false
        )
    }.sortedByDescending { it.usageMillis }

    companion object {
        fun from(config: BlockingConfigurationEntity, limits: List<AppLimitEntity>, usage: List<DailyUsageEntity>, apps: List<TrackedApplicationEntity>) =
            DashboardSnapshot(config, limits, usage, apps.associate { it.packageName to it.displayName })
    }
}
