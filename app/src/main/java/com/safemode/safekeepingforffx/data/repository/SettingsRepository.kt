package com.safemode.safekeepingforffx.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User preferences. Kept in DataStore rather than the Room database because these are single
 * scalar settings, not progress data - nothing to query or join.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val gameVersion: Flow<GameVersion> = dataStore.data.map { preferences ->
        // Stored by name so the value survives reordering the enum.
        preferences[GAME_VERSION]
            ?.let { stored -> GameVersion.entries.firstOrNull { it.name == stored } }
            ?: GameVersion.DEFAULT
    }

    val theme: Flow<ThemePreference> = dataStore.data.map { preferences ->
        preferences[THEME]
            ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
            ?: ThemePreference.DEFAULT
    }

    /**
     * Defaults to true, so the guidance is there the first time a list is opened. Turning it off
     * hides the advice banners at the top of every category.
     */
    val showHelp: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_HELP] ?: true
    }

    suspend fun setGameVersion(version: GameVersion) {
        dataStore.edit { it[GAME_VERSION] = version.name }
    }

    suspend fun setShowHelp(show: Boolean) {
        dataStore.edit { it[SHOW_HELP] = show }
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    private companion object {
        val GAME_VERSION = stringPreferencesKey("game_version")
        val THEME = stringPreferencesKey("theme")
        val SHOW_HELP = booleanPreferencesKey("show_help")
    }
}
