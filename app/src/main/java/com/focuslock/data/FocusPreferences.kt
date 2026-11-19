package com.focuslock.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusPreferences by preferencesDataStore("focuslock_preferences")

/** Small device-level choices live in DataStore; enforcement state remains in Room. */
class FocusPreferences(private val context: Context) {
    private object Keys {
        val darkTheme = booleanPreferencesKey("dark_theme")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val lastKnownZone = stringPreferencesKey("last_known_zone")
    }

    val darkTheme: Flow<Boolean> = context.focusPreferences.data.map { it[Keys.darkTheme] ?: true }
    val onboardingComplete: Flow<Boolean> = context.focusPreferences.data.map { it[Keys.onboardingComplete] ?: false }
    val lastKnownZone: Flow<String> = context.focusPreferences.data.map { it[Keys.lastKnownZone] ?: "" }

    suspend fun setDarkTheme(value: Boolean) = context.focusPreferences.edit { it[Keys.darkTheme] = value }
    suspend fun setOnboardingComplete() = context.focusPreferences.edit { it[Keys.onboardingComplete] = true }
    suspend fun resetOnboarding() = context.focusPreferences.edit { it[Keys.onboardingComplete] = false }
    suspend fun recordTimeZone(zone: String) = context.focusPreferences.edit { it[Keys.lastKnownZone] = zone }
    suspend fun clear() = context.focusPreferences.edit { it.clear() }
}
