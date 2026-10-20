package com.safemode.safekeepingforffx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.backup.BackupCounts
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.data.repository.BackupRepository
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.FavoritesRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val checklistRepository: ChecklistRepository,
    private val monsterArenaRepository: MonsterArenaRepository,
    private val sphereGridRepository: SphereGridRepository,
    private val favoritesRepository: FavoritesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val gameVersion = settingsRepository.gameVersion.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GameVersion.DEFAULT
    )

    private val _resetConfirmed = MutableStateFlow(false)

    /** Flips true once a reset finishes, so the screen can acknowledge it. */
    val resetConfirmed = _resetConfirmed.asStateFlow()

    val theme = settingsRepository.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreference.DEFAULT
    )

    fun setGameVersion(version: GameVersion) {
        viewModelScope.launch { settingsRepository.setGameVersion(version) }
    }

    val showHelp = settingsRepository.showHelp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val sphereGridTapActivates = settingsRepository.sphereGridTapActivates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun setTheme(preference: ThemePreference) {
        viewModelScope.launch { settingsRepository.setTheme(preference) }
    }

    fun setShowHelp(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHelp(show) }
    }

    fun setSphereGridTapActivates(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSphereGridTapActivates(value) }
    }

    val sphereGridFullNodeEditor = settingsRepository.sphereGridFullNodeEditor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun setSphereGridFullNodeEditor(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSphereGridFullNodeEditor(value) }
    }

    private val _busy = MutableStateFlow(false)

    /** True while a backup or restore is running, so the buttons can't be tapped twice. */
    val busy = _busy.asStateFlow()

    private val _backupResult = MutableStateFlow<BackupEvent?>(null)

    /**
     * The outcome of the last backup or restore, held until the screen calls [acknowledgeBackupResult].
     * State rather than a one-shot event: an event fired while the screen was being recreated (a
     * rotation during a slow write to Drive) had no collector and was lost, and the player never
     * learned whether their backup was saved.
     */
    val backupResult = _backupResult.asStateFlow()

    fun acknowledgeBackupResult() {
        _backupResult.value = null
    }

    /**
     * Writes the whole backup to [sink]. The sink is supplied by the screen and points at the file
     * the player chose, so nothing here needs to know about content URIs.
     *
     * Runs to completion even if the player leaves Settings partway: the file was opened in truncate
     * mode, so cancelling mid-write would leave them a broken backup in place of the old one.
     */
    fun backUp(sink: BackupSink) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = withContext(NonCancellable) {
                runCatching { sink.write(backupRepository.exportJson()) }
            }
            _backupResult.value = result.fold(
                onSuccess = { BackupEvent.Saved },
                onFailure = { BackupEvent.Failed(it.message ?: "Couldn't save the backup.") }
            )
            _busy.value = false
        }
    }

    /**
     * Replaces everything with the backup read from [source]. Caller is responsible for confirming
     * with the user first - the current progress is overwritten, not merged.
     *
     * Like [backUp], runs to completion once started, so leaving Settings can't stop a restore
     * between its database write and its settings write.
     */
    fun restore(source: BackupSource) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _backupResult.value = withContext(NonCancellable) {
                val json = runCatching { source.read() }.getOrNull()
                if (json == null) {
                    BackupEvent.Failed("Couldn't read that file.")
                } else {
                    backupRepository.restoreJson(json).fold(
                        onSuccess = { BackupEvent.Restored(it) },
                        onFailure = {
                            BackupEvent.Failed(it.message ?: "That backup couldn't be restored.")
                        }
                    )
                }
            }
            _busy.value = false
        }
    }

    /** Caller is responsible for confirming with the user first - this cannot be undone. */
    fun resetProgress() {
        viewModelScope.launch {
            checklistRepository.clearAllProgress()
            // Monster Arena counts and the sphere grid live in their own tables, so clearing the
            // checklists alone would leave them behind and "reset everything" would be a lie.
            monsterArenaRepository.clearAll()
            sphereGridRepository.clearAll()
            // Favorites point at items in the lists being cleared, so leaving them starred would
            // hand back a shortlist of things the app no longer thinks you have touched.
            favoritesRepository.clearAll()
            _resetConfirmed.value = true
        }
    }

    fun acknowledgeReset() {
        _resetConfirmed.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                SettingsViewModel(
                    app.container.settingsRepository,
                    app.container.checklistRepository,
                    app.container.monsterArenaRepository,
                    app.container.sphereGridRepository,
                    app.container.favoritesRepository,
                    app.container.backupRepository
                )
            }
        }
    }
}

/** The outcome of a backup or a restore, shown until the player acknowledges it. */
sealed interface BackupEvent {
    data object Saved : BackupEvent

    data class Restored(val counts: BackupCounts) : BackupEvent

    /** [reason] is already written for the player - show it as-is. */
    data class Failed(val reason: String) : BackupEvent
}
