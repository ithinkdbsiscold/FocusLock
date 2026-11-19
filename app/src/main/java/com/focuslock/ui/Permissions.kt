package com.focuslock.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object PermissionStatus {
    fun hasUsageAccess(context: Context): Boolean {
        val manager = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            manager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            manager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledServices?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true) {
            return true
        }
        val expected = ComponentName(context, "com.focuslock.blocker.AppBlockerAccessibilityService")
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { entry ->
            val cn = ComponentName.unflattenFromString(entry)
            cn == expected || entry.equals(expected.flattenToString(), ignoreCase = true) || entry.equals(expected.flattenToShortString(), ignoreCase = true)
        }
    }
}
