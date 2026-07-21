package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SphereGridUiState(
    val isLoading: Boolean = true,
    val gridType: GridType = GridType.DEFAULT,
    val grid: GridData = GridData.EMPTY,
    val character: GridCharacter = GridCharacter.DEFAULT,
    /** Player content edits by node id, shared across characters. Absent = vanilla. */
    val overrides: Map<String, NodeContent> = emptyMap(),
    /** Node ids the selected character has activated. */
    val activated: Set<String> = emptySet(),
    val showHelp: Boolean = true,
    /** When true a tap activates a node and a long-press opens its details; otherwise tap opens. */
    val tapActivates: Boolean = false
) {
    fun current(node: SphereGridNode): NodeContent = overrides[node.id] ?: node.original
    fun isEdited(node: SphereGridNode): Boolean = overrides.containsKey(node.id)
    fun isActivated(nodeId: String): Boolean = activated.contains(nodeId)

    val gridAvailable: Boolean get() = grid.totalNodes > 0
    val hasEdits: Boolean get() = overrides.isNotEmpty()
    val characterHasPath: Boolean get() = activated.isNotEmpty()

    /** True when there is any player work at all to export. */
    val hasAnythingToShare: Boolean get() = hasEdits || characterHasPath
}

/** One-off outcomes of a share/import action, delivered to the screen as events (not UI state). */
sealed interface SphereGridEvent {
    data class ExportReady(val code: String) : SphereGridEvent
    data class ImportDone(val summary: SphereGridRepository.ImportSummary) : SphereGridEvent
    data class ImportFailed(val reason: String) : SphereGridEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class SphereGridViewModel(
    private val repository: SphereGridRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val gridType = MutableStateFlow(GridType.DEFAULT)
    private val character = MutableStateFlow(GridCharacter.DEFAULT)

    /** One-shot results of a share/import action, consumed by the screen to drive clipboard + toasts. */
    private val eventChannel = Channel<SphereGridEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /** The parsed grid for the chosen type, reloaded whenever the type changes. */
    private val gridLoad = gridType
        .mapLatest { type ->
            GridLoad(type, runCatching { repository.grid(type) }.getOrDefault(GridData.EMPTY))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GridLoad(GridType.DEFAULT, GridData.EMPTY, isLoading = true)
        )

    /** The selected character's activations, re-subscribed whenever the character changes. */
    private val activations = character.flatMapLatest { repository.observeActivations(it) }

    private val settings = combine(
        settingsRepository.showHelp,
        settingsRepository.sphereGridTapActivates
    ) { showHelp, tapActivates -> showHelp to tapActivates }

    val uiState = combine(
        gridLoad,
        character,
        repository.observeOverrides(),
        activations,
        settings
    ) { load, character, overrides, activated, settings ->
        SphereGridUiState(
            isLoading = load.isLoading,
            gridType = load.type,
            grid = load.grid,
            character = character,
            overrides = overrides,
            activated = activated,
            showHelp = settings.first,
            tapActivates = settings.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SphereGridUiState()
    )

    fun setGridType(type: GridType) {
        gridType.value = type
    }

    fun setCharacter(value: GridCharacter) {
        character.value = value
    }

    /** Toggles whether the selected character has activated [node]. Works on any node type. */
    fun toggleActivation(node: SphereGridNode) {
        val active = uiState.value.isActivated(node.id)
        viewModelScope.launch { repository.setActivation(character.value, node.id, !active) }
    }

    /** Writes a shared content edit to [node], or reverts it. Locks are permanent and ignored. */
    fun setContent(node: SphereGridNode, content: NodeContent?) {
        if (!node.original.isEditable) return
        viewModelScope.launch { repository.setContent(node.id, content, node.original) }
    }

    /** Reverts every shared content edit. Caller confirms first - cannot be undone. */
    fun revertEdits() {
        viewModelScope.launch { repository.clearOverrides() }
    }

    /** Clears the selected character's whole path. Caller confirms first - cannot be undone. */
    fun clearCharacterPath() {
        viewModelScope.launch { repository.clearCharacterActivations(character.value) }
    }

    /** Encodes a shareable build for the current grid/character and hands the code to the screen. */
    fun exportBuild(scope: BuildScope) {
        viewModelScope.launch {
            val code = repository.exportBuild(scope, character.value, gridType.value)
            eventChannel.send(SphereGridEvent.ExportReady(code))
        }
    }

    /** Applies a pasted build code. Caller confirms first - import replaces existing edits/paths. */
    fun importBuild(text: String) {
        viewModelScope.launch {
            repository.importBuild(text, gridType.value)
                .onSuccess { eventChannel.send(SphereGridEvent.ImportDone(it)) }
                .onFailure {
                    eventChannel.send(
                        SphereGridEvent.ImportFailed(it.message ?: "That build code couldn't be read.")
                    )
                }
        }
    }

    private data class GridLoad(
        val type: GridType,
        val grid: GridData,
        val isLoading: Boolean = false
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                SphereGridViewModel(
                    app.container.sphereGridRepository,
                    app.container.settingsRepository
                )
            }
        }
    }
}
