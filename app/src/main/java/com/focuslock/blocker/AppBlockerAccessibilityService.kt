package com.focuslock.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.focuslock.FocusLockApplication
import com.focuslock.data.AccountingClock
import com.focuslock.data.BlockDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance Accessibility Service for real-time application blocking.
 *
 * Enforces limits through:
 * 1. An in-memory cache of blocked packages kept in sync with Room for instant (0ms) interception.
 * 2. System-level GLOBAL_ACTION_HOME invocation to immediately kick the user out of the restricted app.
 * 3. Displaying FocusLock's BlockingActivity in an isolated task.
 * 4. A foreground ticker that tracks usage while an app remains open so it locks the moment the limit is reached.
 */
class AppBlockerAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val blockedPackages = ConcurrentHashMap.newKeySet<String>()

    private var currentMonitoredPackage: String? = null
    private var monitoringJob: Job? = null

    private var lastInterceptTime = 0L
    private var lastInterceptPackage: String? = null

    private var defaultLauncherPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        resolveDefaultLauncher()

        scope.launch {
            app.repository.ensureCurrentDay()

            // Continuously sync blocked packages from Room database into memory
            app.repository.observeBlockedPackageNames().collect { set ->
                blockedPackages.clear()
                blockedPackages.addAll(set)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventType = event?.eventType ?: return
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val targetPackage = event.packageName?.toString() ?: return

        // 1. Never intercept FocusLock itself
        if (targetPackage == packageName) {
            stopForegroundMonitoring()
            return
        }

        // 2. Never intercept emergency, dialer, settings, system UI, or the launcher
        if (isExempt(targetPackage)) {
            stopForegroundMonitoring()
            return
        }

        // 3. FAST PATH: App is already blocked for today -> Intercept instantly!
        if (blockedPackages.contains(targetPackage)) {
            stopForegroundMonitoring()
            interceptBlockedApp(targetPackage)
            return
        }

        // 4. ACTIVE PATH: App is not yet blocked -> monitor foreground usage in real time
        startForegroundMonitoring(targetPackage)
    }

    private fun interceptBlockedApp(targetPackage: String) {
        val now = System.currentTimeMillis()
        // Debounce repeated events within 400ms for the same package
        if (targetPackage == lastInterceptPackage && (now - lastInterceptTime) < 400L) {
            return
        }
        lastInterceptTime = now
        lastInterceptPackage = targetPackage

        // Step 1: Immediately kick the restricted application off the screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Step 2: Show the BlockingActivity
        scope.launch {
            val appName = app.repository.getAppName(targetPackage)
            val config = app.repository.configuration()
            val nextReset = AccountingClock.nextResetMillis(System.currentTimeMillis(), config.resetMinutesAfterMidnight)
            showBlockScreen(targetPackage, appName, nextReset)
        }
    }

    private fun startForegroundMonitoring(targetPackage: String) {
        if (currentMonitoredPackage == targetPackage && monitoringJob?.isActive == true) {
            return
        }
        currentMonitoredPackage = targetPackage
        monitoringJob?.cancel()

        monitoringJob = scope.launch {
            // Check if opening the app puts it over the limit
            when (val decision = app.repository.shouldBlock(targetPackage)) {
                is BlockDecision.Block -> {
                    blockedPackages.add(targetPackage)
                    interceptBlockedApp(targetPackage)
                    return@launch
                }
                BlockDecision.Allow -> Unit
            }

            // Ticker: actively monitor while this package stays in the foreground
            var lastTick = System.currentTimeMillis()
            while (isActive && currentMonitoredPackage == targetPackage) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val elapsed = now - lastTick
                lastTick = now

                val isNowBlocked = app.repository.addForegroundTime(targetPackage, elapsed)
                if (isNowBlocked) {
                    blockedPackages.add(targetPackage)
                    interceptBlockedApp(targetPackage)
                    break
                }
            }
        }
    }

    private fun stopForegroundMonitoring() {
        currentMonitoredPackage = null
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun showBlockScreen(targetPackage: String, appName: String, nextResetMillis: Long) {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            putExtra(BlockingActivity.EXTRA_PACKAGE, targetPackage)
            putExtra(BlockingActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockingActivity.EXTRA_NEXT_RESET, nextResetMillis)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    private fun isExempt(targetPackage: String): Boolean {
        if (targetPackage in exemptPackages) return true
        if (defaultLauncherPackage == null) resolveDefaultLauncher()
        return targetPackage == defaultLauncherPackage
    }

    private fun resolveDefaultLauncher() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        } catch (_: Exception) {
            defaultLauncherPackage = null
        }
    }

    override fun onInterrupt() {
        stopForegroundMonitoring()
    }

    override fun onDestroy() {
        stopForegroundMonitoring()
        scope.cancel()
        super.onDestroy()
    }

    private val app: FocusLockApplication get() = application as FocusLockApplication

    companion object {
        /** Never redirect core recovery, system UI, launchers, or permission surfaces. */
        private val exemptPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.motorola.launcher3",
            "com.motorola.launcher",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.emergency",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.vending"
        )
    }
}
