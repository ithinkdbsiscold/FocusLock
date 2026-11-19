package com.focuslock.reset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.FocusLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Keeps accounting correct after reboot, a time change, or a missed midnight process run. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as FocusLockApplication).repository.ensureCurrentDay()
            } finally {
                pending.finish()
            }
        }
    }
}
