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
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val checklistRepository: ChecklistRepository,
    private val monsterArenaRepository: MonsterArenaRepository,
    private val sphereGridRepository: SphereGridRepository,
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

    private val _events = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)

    /** One-shot outcomes of a backup or restore, for the screen to report. */
    val events = _events.asSharedFlow()

    /**
     * Writes the whole backup to [sink]. The sink is supplied by the screen and points at the file
     * the player chose, so nothing here needs to know about content URIs.
     */
    fun backUp(sink: BackupSink) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { sink.write(backupRepository.exportJson()) }
                .onSuccess { _events.emit(BackupEvent.Saved) }
                .onFailure {
                    _events.emit(BackupEvent.Failed(it.message ?: "Couldn't save the backup."))
                }
            _busy.value = false
        }
    }

    /**
     * Replaces everything with the backup read from [source]. Caller is responsible for confirming
     * with the user first - the current progress is overwritten, not merged.
     */
    fun restore(source: BackupSource) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val json = runCatching { source.read() }.getOrNull()
            if (json == null) {
                _events.emit(BackupEvent.Failed("Couldn't read that file."))
            } else {
                backupRepository.restoreJson(json)
                    .onSuccess { _events.emit(BackupEvent.Restored(it)) }
                    .onFailure {
                        _events.emit(
                            BackupEvent.Failed(it.message ?: "That backup couldn't be restored.")
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
                    app.container.backupRepository
                )
            }
        }
    }
}

/** The outcome of a backup or a restore, reported once. */
sealed interface BackupEvent {
    data object Saved : BackupEvent

    data class Restored(val counts: BackupCounts) : BackupEvent

    /** [reason] is already written for the player - show it as-is. */
    data class Failed(val reason: String) : BackupEvent
}
