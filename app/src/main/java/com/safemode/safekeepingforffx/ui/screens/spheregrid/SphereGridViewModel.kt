package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.CharacterStatus
import com.safemode.safekeepingforffx.data.reference.CharacterStatusCalculator
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    val tapActivates: Boolean = false,
    /** When true the node editor offers every attribute and ability; otherwise the short list. */
    val fullNodeEditor: Boolean = false
) {
    fun current(node: SphereGridNode): NodeContent = overrides[node.id] ?: node.original
    fun isEdited(node: SphereGridNode): Boolean = overrides.containsKey(node.id)
    fun isActivated(nodeId: String): Boolean = activated.contains(nodeId)

    /**
     * A lock the grid still gates: nobody has opened it yet, so it shows as a lock and can't be
     * edited. Opening it is a shared, grid-wide change (a blank override) that turns it into an
     * ordinary editable node for every character - so the check is override presence, not the
     * per-character path.
     */
    fun isLockedGate(node: SphereGridNode): Boolean =
        node.original is NodeContent.Lock && !isEdited(node)

    val gridAvailable: Boolean get() = grid.totalNodes > 0
    val hasEdits: Boolean get() = overrides.isNotEmpty()
    val characterHasPath: Boolean get() = activated.isNotEmpty()

    /** True when there is any player work at all to export. */
    val hasAnythingToShare: Boolean get() = hasEdits || characterHasPath
}

/** One step of a route replay for the viewed character: a grid edit, or an activation. */
sealed interface RouteStep {
    val nodeId: String

    data class Edit(override val nodeId: String, val content: NodeContent) : RouteStep

    data class Activate(override val nodeId: String) : RouteStep
}

/**
 * Read-only replay of a saved route: one ordered timeline of the route's edits and the viewed
 * character's activations, revealed [stepIndex] steps deep. Edits and activations are interleaved in
 * the order they happened, so stepping shows a blank node becoming, say, Magic +4 at the point it was
 * edited and then lighting up when it was taken. Nothing here is written to the database - it overlays
 * the live grid without touching the player's own progress.
 */
data class RouteViewState(
    val name: String,
    val gridType: GridType,
    val character: GridCharacter,
    val availableCharacters: List<GridCharacter>,
    val steps: List<RouteStep>,
    val stepIndex: Int
) {
    val stepCount: Int get() = steps.size
    private val shown: List<RouteStep> get() = steps.take(stepIndex)

    /** Grid edits in force at the current step; a later edit to a node wins over an earlier one. */
    val overrides: Map<String, NodeContent> get() = buildMap {
        shown.forEach { if (it is RouteStep.Edit) put(it.nodeId, it.content) }
    }

    /** Nodes activated by the current step. */
    val activated: Set<String> get() = buildSet {
        shown.forEach { if (it is RouteStep.Activate) add(it.nodeId) }
    }

    /** 1-based activation order for the activated nodes shown so far; edits are not numbered. */
    val orderLabels: Map<String, Int> get() = buildMap {
        var order = 0
        shown.forEach { if (it is RouteStep.Activate) { order++; put(it.nodeId, order) } }
    }

    /** The step just applied at the current position, for the "what happened here" caption. */
    val currentStep: RouteStep? get() = steps.getOrNull(stepIndex - 1)

    /**
     * The node's content at the current step: the latest route edit in force; else its original -
     * except a lock the path has already reached, which is by definition unlocked and shows as blank.
     */
    fun contentAt(nodeId: String, original: NodeContent): NodeContent =
        overrides[nodeId]
            ?: if (original is NodeContent.Lock && nodeId in activated) NodeContent.Empty else original

    /** This node's 1-based activation position in the whole route, or null if it isn't activated. */
    fun activationStepOf(nodeId: String): Int? {
        var order = 0
        steps.forEach {
            if (it is RouteStep.Activate) {
                order++
                if (it.nodeId == nodeId) return order
            }
        }
        return null
    }
}

/** One-off outcomes of a share/import action, delivered to the screen as events (not UI state). */
sealed interface SphereGridEvent {
    data class ExportReady(val code: String) : SphereGridEvent
    data class ImportDone(val summary: SphereGridRepository.ImportSummary) : SphereGridEvent
    data class ImportFailed(val reason: String) : SphereGridEvent
    data class Notice(val message: String) : SphereGridEvent
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

    /** The saved routes library, for the routes sheet. */
    val routes: StateFlow<List<SphereGridRepository.SavedRoute>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Non-null while replaying a route read-only; the screen renders this instead of live progress. */
    private val _routeView = MutableStateFlow<RouteViewState?>(null)
    val routeView: StateFlow<RouteViewState?> = _routeView

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
        settingsRepository.sphereGridTapActivates,
        settingsRepository.sphereGridFullNodeEditor
    ) { showHelp, tapActivates, fullNodeEditor ->
        GridSettings(showHelp, tapActivates, fullNodeEditor)
    }

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
            showHelp = settings.showHelp,
            tapActivates = settings.tapActivates,
            fullNodeEditor = settings.fullNodeEditor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SphereGridUiState()
    )

    /** Starting stats for every character, read once from the bundled asset. */
    private val baseStats = flow { emit(runCatching { repository.baseStats() }.getOrDefault(emptyMap())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * The status of whichever character the canvas is currently showing, recomputed from the grid,
     * the edits and the path in view. During a route replay that means the *route's* character and
     * step, so stepping through a route walks their stats forward; otherwise it is live progress.
     * Null while there is nothing coherent to describe - no grid, or a replay whose grid is still
     * loading, where the live path would briefly be attributed to the route's character.
     */
    val characterStatus: StateFlow<CharacterStatus?> =
        combine(uiState, routeView, baseStats) { state, route, stats ->
            when {
                !state.gridAvailable -> null
                route != null && state.gridType != route.gridType -> null
                else -> {
                    val viewed = route?.character ?: state.character
                    CharacterStatusCalculator.compute(
                        character = viewed,
                        baseStats = stats[viewed],
                        grid = state.grid,
                        overrides = route?.overrides ?: state.overrides,
                        activated = route?.activated ?: state.activated
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The node the status sheet should send the player to for an ability, resolved against the same
     * view the sheet is describing. Null when no node on the grid holds it, in which case the caller
     * leaves the view where it is.
     */
    fun nodeForAbility(name: String, family: NodeType): String? {
        val state = uiState.value
        val route = _routeView.value
        return CharacterStatusCalculator.nodeForAbility(
            name = name,
            family = family,
            grid = state.grid,
            overrides = route?.overrides ?: state.overrides,
            activated = route?.activated ?: state.activated
        )
    }

    fun setGridType(type: GridType) {
        gridType.value = type
    }

    fun setCharacter(value: GridCharacter) {
        character.value = value
    }

    /**
     * The primary tap action on a node. A still-locked gate is *opened* rather than path-toggled:
     * unlocking is a shared, grid-wide change, so it writes a blank override that turns the gate into
     * an ordinary node for every character. Any other node just toggles the selected character's path.
     */
    fun toggleActivation(node: SphereGridNode) {
        val state = uiState.value
        if (state.isLockedGate(node)) {
            viewModelScope.launch { repository.setContent(node.id, NodeContent.Empty, node.original) }
            return
        }
        val active = state.isActivated(node.id)
        viewModelScope.launch { repository.setActivation(character.value, node.id, !active) }
    }

    /**
     * Writes a shared content edit to [node], or reverts it. A lock can't be edited until it has been
     * opened (a blank override exists); reverting an opened gate's edit re-locks it.
     */
    fun setContent(node: SphereGridNode, content: NodeContent?) {
        if (uiState.value.isLockedGate(node)) return
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

    // --- Saved routes library + read-only replay ---

    /** The route currently open for replay, kept so switching characters needs no database read. */
    private var openRouteBuild: SphereGridBuild? = null

    /** Saves the current work as a named route in the library. */
    fun saveCurrentAsRoute(name: String, scope: BuildScope) {
        val label = name.trim()
        if (label.isEmpty()) return
        viewModelScope.launch {
            repository.saveCurrentAsRoute(label, scope, character.value, gridType.value)
            eventChannel.send(SphereGridEvent.Notice("Route saved to your library."))
        }
    }

    /** Saves a pasted route code into the library for replay. */
    fun importRouteToLibrary(name: String, code: String) {
        viewModelScope.launch {
            repository.saveImportedRoute(name.trim(), code.trim())
                .onSuccess { eventChannel.send(SphereGridEvent.Notice("Route added to your library.")) }
                .onFailure {
                    eventChannel.send(
                        SphereGridEvent.ImportFailed(it.message ?: "That route code couldn't be read.")
                    )
                }
        }
    }

    fun renameRoute(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameRoute(id, trimmed) }
    }

    fun deleteRoute(id: Long) {
        viewModelScope.launch { repository.deleteRoute(id) }
    }

    /** Hands a saved route's code to the screen (clipboard + share sheet), like a build export. */
    fun shareRoute(id: Long) {
        viewModelScope.launch {
            repository.routeCode(id)?.let { eventChannel.send(SphereGridEvent.ExportReady(it)) }
        }
    }

    /** Opens a saved route for read-only replay, switching the view to the route's grid. */
    fun enterRouteView(id: Long) {
        viewModelScope.launch {
            val build = repository.routeBuild(id) ?: run {
                eventChannel.send(SphereGridEvent.ImportFailed("That route couldn't be opened."))
                return@launch
            }
            openRouteBuild = build
            gridType.value = build.gridType
            val chars = build.events.filterIsInstance<RouteEvent.Activate>()
                .map { it.character }.distinct()
            val first = chars.firstOrNull()
            val steps = stepsFor(build, first)
            _routeView.value = RouteViewState(
                name = build.name ?: "Route",
                gridType = build.gridType,
                character = first ?: GridCharacter.DEFAULT,
                availableCharacters = chars,
                steps = steps,
                stepIndex = steps.size
            )
        }
    }

    /** Switches which character's path the current replay shows. */
    fun setRouteCharacter(value: GridCharacter) {
        val build = openRouteBuild ?: return
        val current = _routeView.value ?: return
        val steps = stepsFor(build, value)
        _routeView.value = current.copy(character = value, steps = steps, stepIndex = steps.size)
    }

    /**
     * The replay timeline for [character]: that character's activations in path order, each preceded
     * by the edit on that node (edits are grid-wide, so they ride the path of whoever takes the node).
     * The edit is anchored to its node's activation rather than to where it sits in the payload, so a
     * route saved before the ordering fix - whose edits were mis-interleaved - still replays correctly.
     * Edits on nodes the character never takes are left out. Null [character] yields every edit, for
     * an edits-only route with no path to walk.
     */
    private fun stepsFor(build: SphereGridBuild, character: GridCharacter?): List<RouteStep> {
        if (character == null) {
            return build.events.filterIsInstance<RouteEvent.Edit>()
                .map { RouteStep.Edit(it.nodeId, it.content) }
        }
        val editByNode = build.events.filterIsInstance<RouteEvent.Edit>().associateBy { it.nodeId }
        val steps = ArrayList<RouteStep>()
        val emitted = HashSet<String>()
        build.events.forEach { event ->
            if (event is RouteEvent.Activate && event.character == character) {
                editByNode[event.nodeId]?.let { edit ->
                    if (emitted.add(edit.nodeId)) steps.add(RouteStep.Edit(edit.nodeId, edit.content))
                }
                steps.add(RouteStep.Activate(event.nodeId))
            }
        }
        return steps
    }

    /** Reveals the route up to [step] nodes (0..path length). */
    fun setRouteStep(step: Int) {
        val current = _routeView.value ?: return
        _routeView.value = current.copy(stepIndex = step.coerceIn(0, current.stepCount))
    }

    fun exitRouteView() {
        openRouteBuild = null
        _routeView.value = null
    }

    /**
     * Overwrites the player's live progress with the route being viewed and leaves the replay,
     * switching to the viewed character so the adopted path is on screen. Caller confirms first -
     * this replaces the grid edits and the route's character paths and can't be undone.
     */
    fun applyRouteToProgress() {
        val build = openRouteBuild ?: return
        val viewedCharacter = _routeView.value?.character
        viewModelScope.launch {
            repository.applyBuild(build, build.gridType)
                .onSuccess {
                    viewedCharacter?.let { character.value = it }
                    exitRouteView()
                    eventChannel.send(SphereGridEvent.Notice("Route applied to your grid."))
                }
                .onFailure {
                    eventChannel.send(
                        SphereGridEvent.ImportFailed(it.message ?: "That route couldn't be applied.")
                    )
                }
        }
    }

    /** The player settings the planner reads, combined into one value for the state fold. */
    private data class GridSettings(
        val showHelp: Boolean,
        val tapActivates: Boolean,
        val fullNodeEditor: Boolean
    )

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
