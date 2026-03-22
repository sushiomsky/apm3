package com.sushiomsky.proxmark.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "proxmark_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val commandTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    val favoriteFeatureIds: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoScanEnabled: Boolean = false,
    val autoScanIntervalMs: Int = DEFAULT_AUTO_SCAN_INTERVAL_MS
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000
        const val MIN_TIMEOUT_MS = 1_000
        const val MAX_TIMEOUT_MS = 30_000
        const val DEFAULT_AUTO_SCAN_INTERVAL_MS = 3_000
        const val MIN_AUTO_SCAN_INTERVAL_MS = 1_000
        const val MAX_AUTO_SCAN_INTERVAL_MS = 15_000
    }
}

class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore
    private val timeoutKey = intPreferencesKey("command_timeout_ms")
    private val favoritesKey = stringSetPreferencesKey("favorite_feature_ids")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val autoScanIntervalKey = intPreferencesKey("auto_scan_interval_ms")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            commandTimeoutMs = prefs[timeoutKey] ?: AppSettings.DEFAULT_TIMEOUT_MS,
            favoriteFeatureIds = prefs[favoritesKey] ?: emptySet(),
            themeMode = prefs[themeModeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            autoScanIntervalMs = prefs[autoScanIntervalKey] ?: AppSettings.DEFAULT_AUTO_SCAN_INTERVAL_MS
        )
    }

    suspend fun updateTimeout(timeoutMs: Int) {
        val clamped = timeoutMs.coerceIn(AppSettings.MIN_TIMEOUT_MS, AppSettings.MAX_TIMEOUT_MS)
        dataStore.edit { prefs ->
            prefs[timeoutKey] = clamped
        }
    }

    suspend fun toggleFavorite(featureId: String) {
        dataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (featureId in current) {
                current - featureId
            } else {
                current + featureId
            }
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun updateAutoScanInterval(intervalMs: Int) {
        val clamped = intervalMs.coerceIn(AppSettings.MIN_AUTO_SCAN_INTERVAL_MS, AppSettings.MAX_AUTO_SCAN_INTERVAL_MS)
        dataStore.edit { prefs ->
            prefs[autoScanIntervalKey] = clamped
        }
    }
}
