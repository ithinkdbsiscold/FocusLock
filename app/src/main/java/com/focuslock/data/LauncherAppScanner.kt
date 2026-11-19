package com.focuslock.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class LaunchableApp(val packageName: String, val label: String)

class LauncherAppScanner(private val context: Context) {
    fun apps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { info ->
                LaunchableApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(context.packageManager).toString()
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
