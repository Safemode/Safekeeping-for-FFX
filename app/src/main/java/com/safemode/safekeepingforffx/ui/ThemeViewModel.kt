package com.safemode.safekeepingforffx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/** Supplies the theme at the root of the activity, above the nav graph. */
class ThemeViewModel(repository: SettingsRepository) : ViewModel() {

    /**
     * The saved theme, or null until it has been read from disk. Null rather than a default so the
     * activity can tell "not loaded yet" apart from "the player chose System" and hold its first frame
     * until the real value is in - starting on System and switching a moment later drew a frame in
     * the wrong theme for anyone who had forced Light or Dark.
     */
    val theme: StateFlow<ThemePreference?> = repository.theme
        // A settings file that can't be read must not leave the first frame held forever.
        .catch { emit(ThemePreference.DEFAULT) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                ThemeViewModel(app.container.settingsRepository)
            }
        }
    }
}
