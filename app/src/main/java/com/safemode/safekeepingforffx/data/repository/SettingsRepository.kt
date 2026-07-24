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

    /**
     * How a tap behaves on the Sphere Grid. Defaults to false - a tap opens the node's details -
     * which is the more discoverable behaviour. When true, a tap activates the node for the current
     * character and a long-press opens details, which is faster for building a path.
     */
    val sphereGridTapActivates: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SPHERE_GRID_TAP_ACTIVATES] ?: false
    }

    /**
     * What the Sphere Grid node editor offers. Defaults to false, the short list: the spheres a
     * max-stats plan actually places - HP +300, MP +40 and the +4 attributes - and no abilities.
     * True restores the full catalog, every attribute value and all 85 abilities.
     */
    val sphereGridFullNodeEditor: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SPHERE_GRID_FULL_NODE_EDITOR] ?: false
    }

    suspend fun setGameVersion(version: GameVersion) {
        dataStore.edit { it[GAME_VERSION] = version.name }
    }

    suspend fun setShowHelp(show: Boolean) {
        dataStore.edit { it[SHOW_HELP] = show }
    }

    suspend fun setSphereGridTapActivates(value: Boolean) {
        dataStore.edit { it[SPHERE_GRID_TAP_ACTIVATES] = value }
    }

    suspend fun setSphereGridFullNodeEditor(value: Boolean) {
        dataStore.edit { it[SPHERE_GRID_FULL_NODE_EDITOR] = value }
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    private companion object {
        val GAME_VERSION = stringPreferencesKey("game_version")
        val THEME = stringPreferencesKey("theme")
        val SHOW_HELP = booleanPreferencesKey("show_help")
        val SPHERE_GRID_TAP_ACTIVATES = booleanPreferencesKey("sphere_grid_tap_activates")
        val SPHERE_GRID_FULL_NODE_EDITOR = booleanPreferencesKey("sphere_grid_full_node_editor")
    }
}
