package com.focuslock.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.FocusLockApplication
import com.focuslock.data.AppUsageRow
import com.focuslock.data.BlockingConfigurationEntity
import com.focuslock.data.DashboardSnapshot
import com.focuslock.data.DayTotal
import com.focuslock.data.ManagedApp
import com.focuslock.data.ScreenTimeReport
import com.focuslock.data.ScreenTimeTab
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FocusLockApplication).repository
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    val dashboard: StateFlow<DashboardSnapshot> = repository.observeDashboard().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        DashboardSnapshot(BlockingConfigurationEntity(), emptyList(), emptyList())
    )
    val limitedApps: StateFlow<List<AppUsageRow>> = repository.observeLimitedApps().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val managedApps: StateFlow<List<ManagedApp>> = repository.observeManagedApps().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history: StateFlow<List<DayTotal>> = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val configuration: StateFlow<BlockingConfigurationEntity> = repository.observeConfiguration().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockingConfigurationEntity()
    )
    val darkTheme: StateFlow<Boolean> = repository.preferences.darkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    /** null while DataStore loads: avoids flashing onboarding for returning users. */
    val onboardingComplete: StateFlow<Boolean?> = repository.preferences.onboardingComplete
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Screen Time Dashboard State
    val screenTimeTab = MutableStateFlow(ScreenTimeTab.DAY)
    val selectedDay = MutableStateFlow(LocalDate.now())
    val selectedWeekStart = MutableStateFlow(LocalDate.now().with(java.time.DayOfWeek.MONDAY))

    private val _screenTimeReport = MutableStateFlow(
        ScreenTimeReport("Today", "", 0L, emptyList(), emptyList())
    )
    val screenTimeReport: StateFlow<ScreenTimeReport> = _screenTimeReport.asStateFlow()

    val canGoForward: StateFlow<Boolean> = combine(screenTimeTab, selectedDay, selectedWeekStart) { tab, day, week ->
        val today = LocalDate.now()
        if (tab == ScreenTimeTab.DAY) {
            day.isBefore(today)
        } else {
            week.isBefore(today.with(java.time.DayOfWeek.MONDAY))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        refresh()
        loadScreenTime()
    }

    fun refresh() = viewModelScope.launch {
        safely {
            repository.ensureCurrentDay()
            repository.seedTrackedApps()
            repository.reconcileToday()
            loadScreenTime()
        }
    }

    fun loadScreenTime() = viewModelScope.launch {
        safely {
            val report = if (screenTimeTab.value == ScreenTimeTab.DAY) {
                repository.getScreenTimeForDay(selectedDay.value)
            } else {
                repository.getScreenTimeForWeek(selectedWeekStart.value)
            }
            _screenTimeReport.value = report
        }
    }

    fun setScreenTimeTab(tab: ScreenTimeTab) {
        screenTimeTab.value = tab
        loadScreenTime()
    }

    fun previousScreenTimePeriod() {
        if (screenTimeTab.value == ScreenTimeTab.DAY) {
            selectedDay.value = selectedDay.value.minusDays(1)
        } else {
            selectedWeekStart.value = selectedWeekStart.value.minusWeeks(1)
        }
        loadScreenTime()
    }

    fun nextScreenTimePeriod() {
        val today = LocalDate.now()
        if (screenTimeTab.value == ScreenTimeTab.DAY) {
            if (selectedDay.value.isBefore(today)) {
                selectedDay.value = selectedDay.value.plusDays(1)
                loadScreenTime()
            }
        } else {
            val currentWeekMonday = today.with(java.time.DayOfWeek.MONDAY)
            if (selectedWeekStart.value.isBefore(currentWeekMonday)) {
                selectedWeekStart.value = selectedWeekStart.value.plusWeeks(1)
                loadScreenTime()
            }
        }
    }

    fun appHistory(packageName: String): StateFlow<List<DayTotal>> = repository.observeAppDailyHistory(packageName).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun appTrend(packageName: String): StateFlow<String> = flow {
        emit(repository.getAppTrend(packageName))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Calculating trend...")

    fun saveLimit(packageName: String, name: String, millis: Long, enabled: Boolean = true) = viewModelScope.launch {
        safely { repository.addOrUpdateLimit(packageName, name, millis, enabled) }
    }

    fun removeLimit(packageName: String) = viewModelScope.launch {
        safely { repository.removeLimit(packageName) }
    }

    fun setEnabled(packageName: String, enabled: Boolean) = viewModelScope.launch {
        safely { repository.setLimitEnabled(packageName, enabled) }
    }

    fun setStrictMode(enabled: Boolean) = viewModelScope.launch { safely { repository.setStrictMode(enabled) } }
    fun setResetMinutes(minutes: Int) = viewModelScope.launch { safely { repository.setResetMinutes(minutes) } }
    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { repository.preferences.setDarkTheme(dark) }
    fun completeOnboarding() = viewModelScope.launch { repository.preferences.setOnboardingComplete() }
    fun resetOnboarding() = viewModelScope.launch { repository.preferences.resetOnboarding() }
    fun clearData() = viewModelScope.launch { safely { repository.clearAllData() } }

    fun notificationPermissionNeeded() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private suspend fun safely(block: suspend () -> Unit) {
        runCatching { block() }.onFailure { _messages.emit(it.message ?: "That action could not be completed.") }
    }
}
