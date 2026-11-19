package com.focuslock

import android.app.Application
import com.focuslock.data.FocusLockDatabase
import com.focuslock.data.FocusLockRepository

class FocusLockApplication : Application() {
    val database by lazy { FocusLockDatabase.create(this) }
    val repository by lazy { FocusLockRepository(this, database) }
}
