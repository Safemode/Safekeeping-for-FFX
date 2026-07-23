package com.safemode.safekeepingforffx.ui.screens.spheregrid

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import com.safemode.safekeepingforffx.ui.components.Banner
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** World width framed when a route replay focuses on a step's node - a comfortable neighbourhood. */
private const val FOCUS_WORLD_SPAN = 520f

private val GridBackground = Color(0xFF161824)

/** Light text for the value/name labels sitting on the dark grid backing. */
private val LabelColor = Color(0xFFEBEBF2)

/** Gold ring: this node's content was edited away from vanilla (a shared, grid-wide change). */
private val EditedAccent = Color(0xFFFFD54F)

private data class ConfirmAction(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit
)

/**
 * A saved pan/zoom transform for one grid. Kept per [GridType] so each grid remembers where the
 * player left it when they switch away and back. Transform is screen = world * [scale] + [offset].
 */
private data class GridView(val scale: Float, val offset: Offset)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SphereGridScreen(
    modifier: Modifier = Modifier,
    viewModel: SphereGridViewModel = viewModel(factory = SphereGridViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }

    // Saved routes library + read-only replay of a route.
    val routeView by viewModel.routeView.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    var showRoutesSheet by remember { mutableStateOf(false) }
    var showSaveRouteDialog by remember { mutableStateOf(false) }
    var showImportRouteDialog by rememberSaveable { mutableStateOf(false) }
    var routeToRename by remember { mutableStateOf<SphereGridRepository.SavedRoute?>(null) }
    // Replay focus: which node the canvas should ease to, bumped each time the player changes step.
    var routeFocusTarget by remember { mutableStateOf<String?>(null) }
    var routeFocusSignal by remember { mutableIntStateOf(0) }

    // Per-grid pan/zoom memory: each grid keeps its own view when the player switches away and back.
    // resetSignal nudges the canvas to re-fit; canResetView drives the overlay reset button, which
    // only shows once the current grid's view has been moved off its default fit.
    val savedGridViews = remember { mutableMapOf<GridType, GridView>() }
    var resetSignal by remember { mutableIntStateOf(0) }
    var canResetView by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SphereGridEvent.ExportReady -> {
                    clipboard.setText(AnnotatedString(event.code))
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, event.code)
                            },
                            "Share code"
                        )
                    )
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                is SphereGridEvent.ImportDone -> {
                    showImportDialog = false
                    Toast.makeText(context, event.summary.message(), Toast.LENGTH_LONG).show()
                }
                is SphereGridEvent.ImportFailed ->
                    Toast.makeText(context, event.reason, Toast.LENGTH_LONG).show()
                is SphereGridEvent.Notice ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val nodesById = remember(state.grid) { state.grid.nodes.associateBy { it.id } }

    val textMeasurer = rememberTextMeasurer()
    val sphereIcons = rememberSphereIcons()
    // Value/name labels are measured lazily and memoized - there are only a few dozen distinct
    // strings, so each is measured once and reused every frame.
    val labelStyle = remember { TextStyle(fontWeight = FontWeight.Medium, fontSize = 30.sp) }
    val labelCache = remember { mutableMapOf<String, TextLayoutResult>() }

    val activeRoute = routeView
    Column(modifier = modifier.fillMaxSize()) {
        if (activeRoute != null) {
            RouteReplayBar(
                route = activeRoute,
                onSelectCharacter = viewModel::setRouteCharacter,
                onExit = viewModel::exitRouteView,
                onApply = {
                    confirm = ConfirmAction(
                        title = "Make this your live progress?",
                        message = "This overwrites your current grid edits and the route's character " +
                            "paths with \"${activeRoute.name}\". This can't be undone.",
                        confirmLabel = "Overwrite",
                        onConfirm = viewModel::applyRouteToProgress
                    )
                }
            )
        } else {
            SelectorBar(
                gridType = state.gridType,
                onGridTypeChange = viewModel::setGridType,
                hasEdits = state.hasEdits,
                characterHasPath = state.characterHasPath,
                characterName = state.character.displayName,
                onRevertEdits = {
                    confirm = ConfirmAction(
                        title = "Revert all edits?",
                        message = "Every node's content goes back to vanilla on both grids. This " +
                            "can't be undone.",
                        confirmLabel = "Revert all",
                        onConfirm = viewModel::revertEdits
                    )
                },
                onClearPath = {
                    confirm = ConfirmAction(
                        title = "Clear ${state.character.displayName}'s path?",
                        message = "Every node ${state.character.displayName} has activated will be " +
                            "cleared. This can't be undone.",
                        confirmLabel = "Clear path",
                        onConfirm = viewModel::clearCharacterPath
                    )
                },
                canShare = state.hasAnythingToShare,
                onShareBuild = { showShareDialog = true },
                onImportBuild = { showImportDialog = true },
                onOpenRoutes = { showRoutesSheet = true },
                canSaveRoute = state.hasAnythingToShare,
                onSaveRoute = { showSaveRouteDialog = true }
            )
            CharacterRow(selected = state.character, onSelect = viewModel::setCharacter)
        }
        HorizontalDivider()

        if (activeRoute == null && state.showHelp) {
            val tapHint = if (state.tapActivates) {
                "Tap a node to activate it for ${state.character.displayName}; long-press for its " +
                    "content and editor."
            } else {
                "Tap a node for its content, to activate it for ${state.character.displayName}, or " +
                    "to edit it."
            }
            Banner(
                icon = Icons.Outlined.Info,
                text = "Node edits are shared across everyone; each character tracks their own path. " +
                    "$tapHint Activated nodes and the links between them are drawn in the character's " +
                    "own colour; edited nodes carry a gold dot.",
                // A little breathing room so the banner's rounded top doesn't touch the divider above.
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Legend(icons = sphereIcons)
        HorizontalDivider()

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                !state.gridType.isAvailable -> ExpertPlaceholder(
                    onShowStandard = { viewModel.setGridType(GridType.STANDARD) }
                )
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                // Entering a route on a different grid: wait for that grid to load before overlaying
                // the route, so live progress never flashes under the replay for a frame.
                activeRoute != null && state.gridType != activeRoute.gridType ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                activeRoute != null -> GridCanvas(
                    grid = state.grid,
                    gridType = state.gridType,
                    savedViews = savedGridViews,
                    resetSignal = resetSignal,
                    onCanResetChange = { canResetView = it },
                    overrides = activeRoute.overrides,
                    activated = activeRoute.activated,
                    characterColor = activeRoute.character.activationColor(),
                    selectedId = selectedNodeId,
                    tapActivates = false,
                    readOnly = true,
                    orderLabels = activeRoute.orderLabels,
                    focusNodeId = routeFocusTarget,
                    focusSignal = routeFocusSignal,
                    icons = sphereIcons,
                    textMeasurer = textMeasurer,
                    labelCache = labelCache,
                    labelStyle = labelStyle,
                    onActivate = {},
                    onDetails = { selectedNodeId = it },
                    modifier = Modifier.fillMaxSize()
                )
                else -> GridCanvas(
                    grid = state.grid,
                    gridType = state.gridType,
                    savedViews = savedGridViews,
                    resetSignal = resetSignal,
                    onCanResetChange = { canResetView = it },
                    overrides = state.overrides,
                    activated = state.activated,
                    characterColor = state.character.activationColor(),
                    selectedId = selectedNodeId,
                    tapActivates = state.tapActivates,
                    icons = sphereIcons,
                    textMeasurer = textMeasurer,
                    labelCache = labelCache,
                    labelStyle = labelStyle,
                    onActivate = { id -> nodesById[id]?.let(viewModel::toggleActivation) },
                    onDetails = { selectedNodeId = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Reset-view button: only while a grid is shown and its view has been moved off default.
            if (canResetView && state.gridAvailable && !state.isLoading) {
                FilledTonalIconButton(
                    onClick = { resetSignal++ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = "Reset view to fit the whole grid"
                    )
                }
            }
        }

        if (activeRoute != null) {
            // A short caption for the step just reached, so an edit reads as clearly as an activation.
            val stepText = activeRoute.currentStep?.let { step ->
                when (step) {
                    is RouteStep.Edit ->
                        if (step.content is NodeContent.Empty) "Cleared node" else "Set to ${step.content.label()}"
                    is RouteStep.Activate -> {
                        val original = nodesById[step.nodeId]?.original ?: NodeContent.Empty
                        val content = activeRoute.contentAt(step.nodeId, original)
                        if (content is NodeContent.Empty) "Activated blank node" else "Activated ${content.label()}"
                    }
                }
            }
            RouteStepBar(
                route = activeRoute,
                currentStepText = stepText,
                onStep = { newIndex ->
                    val clamped = newIndex.coerceIn(0, activeRoute.stepCount)
                    viewModel.setRouteStep(clamped)
                    // Focus the node this step lands on; step 0 has none, so the view stays put.
                    routeFocusTarget = activeRoute.steps.getOrNull(clamped - 1)?.nodeId
                    routeFocusSignal++
                }
            )
        }
    }

    val selected = selectedNodeId?.let { nodesById[it] }
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedNodeId = null },
            sheetState = sheetState
        ) {
            if (activeRoute != null) {
                // Read-only inspection during replay: the route's content for this node and its step.
                RouteNodeDetail(
                    node = selected,
                    current = activeRoute.contentAt(selected.id, selected.original),
                    routeName = activeRoute.name,
                    characterName = activeRoute.character.displayName,
                    step = activeRoute.activationStepOf(selected.id)
                )
            } else {
                NodeDetail(
                    node = selected,
                    current = state.current(selected),
                    characterName = state.character.displayName,
                    isActivated = state.isActivated(selected.id),
                    onToggleActivation = { viewModel.toggleActivation(selected) },
                    onEdit = {
                        editingNodeId = selected.id
                        selectedNodeId = null
                    },
                    onRevert = {
                        viewModel.setContent(selected, null)
                        selectedNodeId = null
                    }
                )
            }
        }
    }

    val editing = editingNodeId?.let { nodesById[it] }
    if (editing != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingNodeId = null },
            sheetState = sheetState
        ) {
            NodeEditor(
                node = editing,
                current = state.current(editing),
                abilities = state.grid.abilities,
                onApply = { content ->
                    viewModel.setContent(editing, content)
                    editingNodeId = null
                }
            )
        }
    }

    if (showShareDialog) {
        ShareScopeDialog(
            characterName = state.character.displayName,
            onDismiss = { showShareDialog = false },
            onPick = { scope ->
                showShareDialog = false
                viewModel.exportBuild(scope)
            }
        )
    }

    if (showImportDialog) {
        ImportBuildDialog(
            onDismiss = { showImportDialog = false },
            onImport = viewModel::importBuild
        )
    }

    if (showRoutesSheet) {
        RoutesSheet(
            routes = routes,
            onDismiss = { showRoutesSheet = false },
            onView = { id ->
                showRoutesSheet = false
                viewModel.enterRouteView(id)
            },
            onShare = viewModel::shareRoute,
            onRename = { routeToRename = it },
            onDelete = { route ->
                confirm = ConfirmAction(
                    title = "Delete \"${route.name}\"?",
                    message = "This removes the saved route from your library. This can't be undone.",
                    confirmLabel = "Delete",
                    onConfirm = { viewModel.deleteRoute(route.id) }
                )
            },
            onImportRoute = { showImportRouteDialog = true }
        )
    }

    if (showSaveRouteDialog) {
        SaveRouteDialog(
            characterName = state.character.displayName,
            onDismiss = { showSaveRouteDialog = false },
            onSave = { name, scope ->
                showSaveRouteDialog = false
                viewModel.saveCurrentAsRoute(name, scope)
            }
        )
    }

    if (showImportRouteDialog) {
        ImportRouteDialog(
            onDismiss = { showImportRouteDialog = false },
            onImport = { name, code ->
                showImportRouteDialog = false
                viewModel.importRouteToLibrary(name, code)
            }
        )
    }

    routeToRename?.let { route ->
        RenameRouteDialog(
            currentName = route.name,
            onDismiss = { routeToRename = null },
            onRename = { name ->
                viewModel.renameRoute(route.id, name)
                routeToRename = null
            }
        )
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        action.onConfirm()
                        confirm = null
                    }
                ) {
                    Text(action.confirmLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel") }
            }
        )
    }
}

/** The grid picker on the left and an overflow menu of destructive actions on the right. */
@Composable
private fun SelectorBar(
    gridType: GridType,
    onGridTypeChange: (GridType) -> Unit,
    hasEdits: Boolean,
    characterHasPath: Boolean,
    characterName: String,
    onRevertEdits: () -> Unit,
    onClearPath: () -> Unit,
    canShare: Boolean,
    onShareBuild: () -> Unit,
    onImportBuild: () -> Unit,
    onOpenRoutes: () -> Unit,
    canSaveRoute: Boolean,
    onSaveRoute: () -> Unit
) {
    var gridMenu by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            OutlinedButton(onClick = { gridMenu = true }) {
                Text("${gridType.label} Grid")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = gridMenu, onDismissRequest = { gridMenu = false }) {
                GridType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(if (type.isAvailable) type.label else "${type.label} (coming soon)")
                        },
                        onClick = {
                            onGridTypeChange(type)
                            gridMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Box {
            IconButton(onClick = { overflow = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
            }
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text("Share build") },
                    enabled = canShare,
                    onClick = {
                        overflow = false
                        onShareBuild()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import build") },
                    onClick = {
                        overflow = false
                        onImportBuild()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Routes") },
                    onClick = {
                        overflow = false
                        onOpenRoutes()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Save current as route") },
                    enabled = canSaveRoute,
                    onClick = {
                        overflow = false
                        onSaveRoute()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Revert all edits") },
                    enabled = hasEdits,
                    onClick = {
                        overflow = false
                        onRevertEdits()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear $characterName's path") },
                    enabled = characterHasPath,
                    onClick = {
                        overflow = false
                        onClearPath()
                    }
                )
            }
        }
    }
}

/** Lets the player choose how much of their work a shared build code should carry. */
@Composable
private fun ShareScopeDialog(
    characterName: String,
    onDismiss: () -> Unit,
    onPick: (BuildScope) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share build") },
        text = {
            Column {
                Text(
                    "A build code copies to your clipboard and opens the share sheet. Choose what to " +
                        "include:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(8.dp))
                BuildScope.entries.forEach { scope ->
                    Text(
                        text = scope.shareLabel(characterName),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(scope) }
                            .padding(vertical = 14.dp)
                    )
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Pastes a build code and imports it, warning first that it replaces the current edits/paths. */
@Composable
private fun ImportBuildDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import build") },
        text = {
            Column {
                Text(
                    "Paste a build code below. Importing replaces the grid edits and any character " +
                        "paths the code includes - this can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Build code") },
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank(),
                onClick = { onImport(code.trim()) }
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** The scope label with the current character's name filled in, for the share dialog. */
private fun BuildScope.shareLabel(characterName: String): String = when (this) {
    BuildScope.EDITS_AND_CURRENT -> "Edits + $characterName's path"
    BuildScope.EDITS_AND_ALL -> "Edits + all characters' paths"
    BuildScope.CURRENT_PATH -> "$characterName's path only"
    BuildScope.EDITS_ONLY -> "Grid edits only"
}

/** A short, human summary of what an import applied, for the confirmation toast. */
private fun SphereGridRepository.ImportSummary.message(): String {
    val parts = buildList {
        editCount?.let { add(if (it == 1) "1 grid edit" else "$it grid edits") }
        pathCounts?.let { counts ->
            val named = counts.entries.filter { it.value > 0 }
            when {
                named.isEmpty() -> add("no character paths")
                named.size == 1 -> add("${named.first().key.displayName}'s path")
                else -> add("${named.size} character paths")
            }
        }
    }
    return if (parts.isEmpty()) "Nothing to import." else "Imported ${parts.joinToString(" and ")}."
}

/** A scrollable row of the seven characters; the selected one owns the path shown on the grid. */
@Composable
private fun CharacterRow(selected: GridCharacter, onSelect: (GridCharacter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GridCharacter.entries.forEach { character ->
            FilterChip(
                selected = character == selected,
                onClick = { onSelect(character) },
                label = { Text(character.displayName) },
                leadingIcon = { ColorDot(character.activationColor()) }
            )
        }
    }
}

@Composable
private fun ExpertPlaceholder(onShowStandard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "The Expert Sphere Grid isn't available yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(12.dp))
        OutlinedButton(onClick = onShowStandard) { Text("Show Standard grid") }
    }
}

/** Node details: activate for the current character, see vanilla vs current content, and edit it. */
@Composable
private fun NodeDetail(
    node: SphereGridNode,
    current: NodeContent,
    characterName: String,
    isActivated: Boolean,
    onToggleActivation: () -> Unit,
    onEdit: () -> Unit,
    onRevert: () -> Unit
) {
    val edited = current != node.original
    // A gate nobody has opened yet still holds its Lock content; opening it is a shared change, so it
    // gets an Unlock action rather than the per-character activation switch.
    val isLocked = current is NodeContent.Lock
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(text = current.kindLabel(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLocked) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Locked gate", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Unlocking turns this into a blank node for every character. You can " +
                            "then write content onto it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(10.dp))
                    OutlinedButton(onClick = onToggleActivation) { Text("Unlock (make blank)") }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onToggleActivation)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Activated by $characterName",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isActivated) "On this character's path" else "Not activated yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isActivated, onCheckedChange = { onToggleActivation() })
                }
            }
        }

        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContentCard(heading = "Original", content = node.original, modifier = Modifier.weight(1f))
            ContentCard(
                heading = "Current",
                content = current,
                highlighted = edited,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.size(16.dp))
        Text(
            text = current.activationSentence(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (current.isEditable) {
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Edit content")
                }
                if (edited) {
                    TextButton(onClick = onRevert, modifier = Modifier.weight(1f)) {
                        Text("Revert to original")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentCard(
    heading: String,
    content: NodeContent,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlighted) {
            androidx.compose.foundation.BorderStroke(2.dp, EditedAccent)
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(content.displayType.color(), CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = content.label(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeEditor(
    node: SphereGridNode,
    current: NodeContent,
    abilities: List<com.safemode.safekeepingforffx.data.reference.GridAbility>,
    onApply: (NodeContent?) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, abilities) {
        val needle = query.trim()
        if (needle.isEmpty()) abilities
        else abilities.filter { it.name.contains(needle, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Text("Edit content", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Currently: ${current.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onApply(NodeContent.Empty) },
                    label = { Text("Clear (Empty)") }
                )
                if (current != node.original) {
                    AssistChip(
                        onClick = { onApply(null) },
                        label = { Text("Revert to original") }
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Text("Attributes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttributeCatalog.forEach { attr ->
                    FilterChip(
                        selected = current == attr,
                        onClick = { onApply(attr) },
                        label = { Text(attr.label()) },
                        leadingIcon = { ColorDot(attr.displayType.color()) }
                    )
                }
            }
            Spacer(Modifier.size(20.dp))
            Text("Abilities", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            SearchBox(query = query, onQueryChange = { query = it })
            Spacer(Modifier.size(8.dp))
        }

        items(filtered, key = { "${it.family}_${it.name}" }) { ability ->
            val content = NodeContent.Ability(ability.name, ability.family)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApply(content) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(ability.family.color())
                Spacer(Modifier.width(12.dp))
                Text(text = ability.name, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    text = ability.family.attributeName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }

        item { Spacer(Modifier.size(24.dp)) }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(color, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
    )
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search abilities") }
    )
}

/** Header shown while replaying a saved route: its name, a way out, a character picker, and actions. */
@Composable
private fun RouteReplayBar(
    route: RouteViewState,
    onSelectCharacter: (GridCharacter) -> Unit,
    onExit: () -> Unit,
    onApply: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = "Exit route view")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Viewing route",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(route.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Route actions")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Make this my live progress") },
                        onClick = {
                            menu = false
                            onApply()
                        }
                    )
                }
            }
        }
        if (route.availableCharacters.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                route.availableCharacters.forEach { character ->
                    FilterChip(
                        selected = character == route.character,
                        onClick = { onSelectCharacter(character) },
                        label = { Text(character.displayName) },
                        leadingIcon = { ColorDot(character.activationColor()) }
                    )
                }
            }
        }
    }
}

/** The replay scrubber: step through the route one event at a time, or drag to any point. */
@Composable
private fun RouteStepBar(route: RouteViewState, currentStepText: String?, onStep: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (route.stepCount == 0) {
            Text(
                "${route.character.displayName} has no steps in this route.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Step ${route.stepIndex} of ${route.stepCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentStepText != null) {
                Text(
                    "  ·  $currentStepText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onStep(0) },
                enabled = route.stepIndex > 0
            ) { Icon(Icons.Filled.FirstPage, contentDescription = "Jump to start") }
            IconButton(
                onClick = { onStep(route.stepIndex - 1) },
                enabled = route.stepIndex > 0
            ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous step") }
            Slider(
                value = route.stepIndex.toFloat(),
                onValueChange = { onStep(it.roundToInt()) },
                valueRange = 0f..route.stepCount.toFloat(),
                steps = (route.stepCount - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onStep(route.stepIndex + 1) },
                enabled = route.stepIndex < route.stepCount
            ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next step") }
            IconButton(
                onClick = { onStep(route.stepCount) },
                enabled = route.stepIndex < route.stepCount
            ) { Icon(Icons.Filled.LastPage, contentDescription = "Jump to end") }
        }
    }
}

/** The saved routes library: view, share, rename or delete a route, or import a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutesSheet(
    routes: List<SphereGridRepository.SavedRoute>,
    onDismiss: () -> Unit,
    onView: (Long) -> Unit,
    onShare: (Long) -> Unit,
    onRename: (SphereGridRepository.SavedRoute) -> Unit,
    onDelete: (SphereGridRepository.SavedRoute) -> Unit,
    onImportRoute: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Saved routes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onImportRoute) { Text("Import") }
            }
            Spacer(Modifier.size(8.dp))
            if (routes.isEmpty()) {
                Text(
                    "No saved routes yet. Use \"Save current as route\" to store your path in the " +
                        "order you took it, or import a route someone shared.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(routes, key = { it.id }) { route ->
                        RouteRow(
                            route = route,
                            onView = { onView(route.id) },
                            onShare = { onShare(route.id) },
                            onRename = { onRename(route) },
                            onDelete = { onDelete(route) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** One row in the routes library: tap to replay, overflow to share/rename/delete. */
@Composable
private fun RouteRow(
    route: SphereGridRepository.SavedRoute,
    onView: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val pathCount = route.pathCounts.values.count { it > 0 }
    val subtitle = buildList {
        add("${route.gridType.label} grid")
        if (route.editCount > 0) add(if (route.editCount == 1) "1 edit" else "${route.editCount} edits")
        if (pathCount > 0) add(if (pathCount == 1) "1 path" else "$pathCount paths")
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(route.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Route actions")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("View") },
                    onClick = { menu = false; onView() }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { menu = false; onShare() }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { menu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

/** Names the current work and picks how much of it to keep, then saves it as a route. */
@Composable
private fun SaveRouteDialog(
    characterName: String,
    onDismiss: () -> Unit,
    onSave: (String, BuildScope) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var scope by remember { mutableStateOf(BuildScope.EDITS_AND_CURRENT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save route") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Route name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(12.dp))
                Text("Include:", style = MaterialTheme.typography.labelLarge)
                BuildScope.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope = option }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == scope, onClick = { scope = option })
                        Spacer(Modifier.size(4.dp))
                        Text(option.shareLabel(characterName))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), scope) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Pastes a route code and adds it to the library, optionally under a chosen name. */
@Composable
private fun ImportRouteDialog(
    onDismiss: () -> Unit,
    onImport: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import route") },
        text = {
            Column {
                Text(
                    "Paste a route code to add it to your library. You can replay it without touching " +
                        "your own progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Route code") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = code.isNotBlank(), onClick = { onImport(name.trim(), code.trim()) }) {
                Text("Import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Renames a saved route. */
@Composable
private fun RenameRouteDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename route") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Route name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onRename(name.trim()) }) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Read-only node inspection during replay: the route's content for a node and where it falls. */
@Composable
private fun RouteNodeDetail(
    node: SphereGridNode,
    current: NodeContent,
    routeName: String,
    characterName: String,
    step: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(text = current.kindLabel(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(12.dp))
        ContentCard(heading = "Content", content = current, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(16.dp))
        Text(
            text = if (step != null) {
                "Step $step on $characterName's path in \"$routeName\"."
            } else {
                "Not on $characterName's path in this route."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A node's fallback content in a replay when the route hasn't (yet) edited it. Normally its vanilla
 * original - but a lock the route's path has already reached is by definition unlocked, so it shows
 * as a blank node instead of a lock. The database only keeps a node's final edit, so a lock that was
 * unlocked and then filled loses its "unlocked to blank" step; this infers it from the activation.
 */
private fun routeUnlockedOriginal(
    node: SphereGridNode,
    readOnly: Boolean,
    activated: Set<String>
): NodeContent =
    if (readOnly && node.original is NodeContent.Lock && node.id in activated) {
        NodeContent.Empty
    } else {
        node.original
    }

/** The pan/zoom canvas. Transform is screen = world * scale + offset. */
@Composable
private fun GridCanvas(
    grid: GridData,
    gridType: GridType,
    savedViews: MutableMap<GridType, GridView>,
    resetSignal: Int,
    onCanResetChange: (Boolean) -> Unit,
    overrides: Map<String, NodeContent>,
    activated: Set<String>,
    characterColor: Color,
    selectedId: String?,
    tapActivates: Boolean,
    icons: SphereIcons,
    textMeasurer: TextMeasurer,
    labelCache: MutableMap<String, TextLayoutResult>,
    labelStyle: TextStyle,
    onActivate: (String) -> Unit,
    onDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Read-only replay: taps only open node details, never toggle activation. */
    readOnly: Boolean = false,
    /** Activation-order number to draw on each revealed node, for route replay. */
    orderLabels: Map<String, Int> = emptyMap(),
    /** Route replay: the node to ease the view onto when [focusSignal] changes. */
    focusNodeId: String? = null,
    /** Bumped by the caller each time it wants the view to re-focus (one focus per step change). */
    focusSignal: Int = 0
) {
    val bounds = grid.bounds
    val nodes = grid.nodes
    val edges = remember(grid) {
        val byId = nodes.associateBy { it.id }
        grid.edges.mapNotNull { e ->
            val a = byId[e.fromId]
            val b = byId[e.toId]
            if (a != null && b != null) EdgeSegment(a.id, b.id, a.x, a.y, b.x, b.y) else null
        }
    }
    // Which side each node's label sits on, decided once from its connections so it points into the
    // emptiest direction instead of always downward onto whatever node is below it.
    val labelPlacement = remember(grid) {
        val byId = nodes.associateBy { it.id }
        val adjacency = HashMap<String, MutableList<SphereGridNode>>()
        grid.edges.forEach { e ->
            val a = byId[e.fromId]
            val b = byId[e.toId]
            if (a != null && b != null) {
                adjacency.getOrPut(a.id) { mutableListOf() }.add(b)
                adjacency.getOrPut(b.id) { mutableListOf() }.add(a)
            }
        }
        nodes.associate { it.id to labelPlacementFor(it, adjacency[it.id]) }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var minScale by remember { mutableFloatStateOf(0.05f) }
    var maxScale by remember { mutableFloatStateOf(2f) }

    // The fit-and-centre transform for this grid in the current viewport: the whole grid framed with
    // a small margin, centred on its bounding box. Null until the canvas has been measured.
    val fitView: GridView? = if (
        canvasSize.width == 0 || canvasSize.height == 0 || bounds.width == 0f || bounds.height == 0f
    ) {
        null
    } else {
        val fit = minOf(canvasSize.width / bounds.width, canvasSize.height / bounds.height) * 0.95f
        GridView(
            scale = fit,
            offset = Offset(
                x = canvasSize.width / 2f - (bounds.minX + bounds.width / 2f) * fit,
                y = canvasSize.height / 2f - (bounds.minY + bounds.height / 2f) * fit
            )
        )
    }

    fun applyView(view: GridView) {
        scale = view.scale
        offset = view.offset
        savedViews[gridType] = view
    }

    // Each grid keeps its own pan/zoom: restore the view saved for this grid, or fit-and-centre it
    // the first time it is shown. Runs on first measure and whenever the grid or viewport changes.
    LaunchedEffect(gridType, canvasSize) {
        val fit = fitView ?: return@LaunchedEffect
        minScale = fit.scale * 0.7f
        maxScale = fit.scale * 14f
        val saved = savedViews[gridType]
        if (saved != null) {
            scale = saved.scale.coerceIn(minScale, maxScale)
            offset = saved.offset
        } else {
            applyView(fit)
        }
    }

    // The reset control only appears once the view has been nudged off the default fit.
    val atDefaultView = fitView == null ||
        (abs(scale - fitView.scale) <= fitView.scale * 0.001f &&
            abs(offset.x - fitView.offset.x) <= 0.5f &&
            abs(offset.y - fitView.offset.y) <= 0.5f)
    val canReset = fitView != null && !atDefaultView
    LaunchedEffect(canReset) { onCanResetChange(canReset) }

    // A reset request from the overlay button re-frames the whole grid.
    LaunchedEffect(resetSignal) {
        if (resetSignal != 0) fitView?.let { applyView(it) }
    }

    // Route replay: when the player changes step, ease the view to centre on that step's node, zoomed
    // in to a comfortable neighbourhood so it's easy to see what the step does.
    LaunchedEffect(focusSignal) {
        if (focusSignal == 0) return@LaunchedEffect
        val target = nodes.firstOrNull { it.id == focusNodeId } ?: return@LaunchedEffect
        val fit = fitView ?: return@LaunchedEffect
        val endScale = (canvasSize.width / FOCUS_WORLD_SPAN).coerceIn(fit.scale, maxScale)
        val startScale = scale
        val startOffset = offset
        val endOffset = Offset(
            x = canvasSize.width / 2f - target.x * endScale,
            y = canvasSize.height / 2f - target.y * endScale
        )
        animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(durationMillis = 350)) { t, _ ->
            scale = startScale + (endScale - startScale) * t
            offset = Offset(
                x = startOffset.x + (endOffset.x - startOffset.x) * t,
                y = startOffset.y + (endOffset.y - startOffset.y) * t
            )
        }
        savedViews[gridType] = GridView(scale, offset)
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
            .background(GridBackground)
            .pointerInput(nodes) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val worldX = (centroid.x - offset.x) / scale
                    val worldY = (centroid.y - offset.y) / scale
                    applyView(
                        GridView(
                            scale = newScale,
                            offset = Offset(
                                x = centroid.x - worldX * newScale + pan.x,
                                y = centroid.y - worldY * newScale + pan.y
                            )
                        )
                    )
                }
            }
            .pointerInput(nodes, tapActivates, readOnly) {
                fun nodeAt(pos: Offset): String? {
                    val worldX = (pos.x - offset.x) / scale
                    val worldY = (pos.y - offset.y) / scale
                    val touch = (44f / scale).coerceAtLeast(NodeSizing.ABILITY_RADIUS)
                    var bestId: String? = null
                    var bestDist = touch
                    nodes.forEach { node ->
                        val d = hypot(node.x - worldX, node.y - worldY)
                        if (d <= bestDist) {
                            bestDist = d
                            bestId = node.id
                        }
                    }
                    return bestId
                }
                detectTapGestures(
                    onTap = { pos ->
                        // In read-only replay a tap only inspects a node; it never toggles activation.
                        nodeAt(pos)?.let { if (!readOnly && tapActivates) onActivate(it) else onDetails(it) }
                    },
                    // Long-press always opens details, so the editor stays reachable in either mode.
                    onLongPress = { pos -> nodeAt(pos)?.let(onDetails) }
                )
            }
    ) {
        val edgeColor = Color.White.copy(alpha = 0.22f)
        val edgeWidth = (1.4f * scale).coerceIn(0.6f, 3f)
        // A link both of whose ends the character has taken is part of their path; drawn thicker and
        // in their own colour so a route reads as a connected chain rather than scattered rings.
        val linkWidth = (2.6f * scale).coerceIn(1.2f, 5f)

        // Base pass: every link except the ones that are fully on the path (those go on top, below).
        edges.forEach { seg ->
            if (seg.fromId in activated && seg.toId in activated) return@forEach
            val ax = seg.ax * scale + offset.x
            val ay = seg.ay * scale + offset.y
            val bx = seg.bx * scale + offset.x
            val by = seg.by * scale + offset.y
            if (!segmentVisible(ax, ay, bx, by, size.width, size.height)) return@forEach
            drawLine(edgeColor, Offset(ax, ay), Offset(bx, by), strokeWidth = edgeWidth)
        }

        // Highlight pass: activated links on top so crossings never bury the path.
        edges.forEach { seg ->
            if (seg.fromId !in activated || seg.toId !in activated) return@forEach
            val ax = seg.ax * scale + offset.x
            val ay = seg.ay * scale + offset.y
            val bx = seg.bx * scale + offset.x
            val by = seg.by * scale + offset.y
            if (!segmentVisible(ax, ay, bx, by, size.width, size.height)) return@forEach
            drawLine(characterColor, Offset(ax, ay), Offset(bx, by), strokeWidth = linkWidth)
        }

        nodes.forEach { node ->
            val cx = node.x * scale + offset.x
            val cy = node.y * scale + offset.y
            val content = overrides[node.id] ?: routeUnlockedOriginal(node, readOnly, activated)
            val dtype = content.displayType
            val r = (dtype.nodeRadius() * scale).coerceAtLeast(1.5f)
            if (cx < -r || cy < -r || cx > size.width + r || cy > size.height + r) return@forEach

            val color = dtype.color()
            val center = Offset(cx, cy)
            val isActivated = activated.contains(node.id)

            drawCircle(color = color, radius = r, center = center)
            // The rim marks activation - the selected character's own colour when they have taken this
            // node, a plain dark edge otherwise. Because it rides the node's own outline it never
            // reaches a neighbouring node, so a whole activated path stays readable instead of a mass
            // of rings.
            drawCircle(
                color = if (isActivated) characterColor else Color.Black.copy(alpha = 0.3f),
                radius = r,
                center = center,
                style = Stroke(
                    width = if (isActivated) (r * 0.18f).coerceAtLeast(1.6f)
                    else (r * 0.12f).coerceAtLeast(0.8f)
                )
            )

            if (r >= 9f) {
                val glyphColor = glyphColorFor(color)
                val icon = if (dtype.isLock) {
                    (node.original as? NodeContent.Lock)?.level?.let { icons.forLock(it) }
                } else {
                    icons.forType(dtype)
                }
                icon?.let { drawNodeIcon(it, center, r, glyphColor, dtype.iconScale()) }
            }

            // In route replay the activation-order number sits above the node; otherwise the value or
            // ability name sits beside it.
            if (r >= LabelTuning.MIN_RADIUS) {
                val order = orderLabels[node.id]
                if (order != null) {
                    val text = order.toString()
                    val result = labelCache.getOrPut("#$text") { textMeasurer.measure(text, labelStyle) }
                    drawNodeLabel(result, center, r, LabelColor, LabelPlacement.UP)
                } else {
                    val label = when (content) {
                        is NodeContent.Attribute -> "+${content.value}"
                        is NodeContent.Ability -> content.name
                        else -> null
                    }
                    if (label != null) {
                        val result = labelCache.getOrPut(label) { textMeasurer.measure(label, labelStyle) }
                        val placement = labelPlacement[node.id] ?: LabelPlacement.DOWN
                        drawNodeLabel(result, center, r, LabelColor, placement)
                    }
                }
            }

            // Edited content is flagged with a small gold dot in the corner - inside the node, so it
            // can't crowd neighbours either.
            if (overrides.containsKey(node.id)) {
                val dot = Offset(cx + r * 0.5f, cy - r * 0.5f)
                val dotRadius = (r * 0.26f).coerceAtLeast(2f)
                drawCircle(EditedAccent, radius = dotRadius, center = dot)
                drawCircle(
                    Color.Black.copy(alpha = 0.35f),
                    radius = dotRadius,
                    center = dot,
                    style = Stroke(width = (dotRadius * 0.28f).coerceAtLeast(1f))
                )
            }
            // The tapped node gets a thin outer highlight; only ever one at a time.
            if (node.id == selectedId) {
                drawCircle(
                    color = Color.White,
                    radius = r + r * 0.12f,
                    center = center,
                    style = Stroke(width = (r * 0.14f).coerceAtLeast(1.2f))
                )
            }
        }
    }
}

/**
 * Picks the side for a node's label: the direction pointing away from the average of its neighbours,
 * so labels lean into open space. Falls back to below for an isolated or symmetrically-surrounded
 * node.
 */
private fun labelPlacementFor(node: SphereGridNode, neighbours: List<SphereGridNode>?): LabelPlacement {
    if (neighbours.isNullOrEmpty()) return LabelPlacement.DOWN
    var sx = 0f
    var sy = 0f
    neighbours.forEach { n ->
        val dx = n.x - node.x
        val dy = n.y - node.y
        val len = hypot(dx, dy)
        if (len > 0f) {
            sx += dx / len
            sy += dy / len
        }
    }
    // Away from the crowd.
    val ax = -sx
    val ay = -sy
    if (abs(ax) < 0.15f && abs(ay) < 0.15f) return LabelPlacement.DOWN
    return if (abs(ax) > abs(ay)) {
        if (ax > 0f) LabelPlacement.RIGHT else LabelPlacement.LEFT
    } else {
        if (ay > 0f) LabelPlacement.DOWN else LabelPlacement.UP
    }
}

private data class EdgeSegment(
    val fromId: String,
    val toId: String,
    val ax: Float,
    val ay: Float,
    val bx: Float,
    val by: Float
)

private fun segmentVisible(ax: Float, ay: Float, bx: Float, by: Float, w: Float, h: Float): Boolean {
    if (ax < 0f && bx < 0f) return false
    if (ay < 0f && by < 0f) return false
    if (ax > w && bx > w) return false
    if (ay > h && by > h) return false
    return true
}

@Composable
private fun Legend(
    icons: SphereIcons,
    modifier: Modifier = Modifier
) {
    val entries = remember {
        listOf(
            NodeType.HP, NodeType.MP, NodeType.STRENGTH, NodeType.DEFENSE, NodeType.MAGIC,
            NodeType.MAGIC_DEFENSE, NodeType.AGILITY, NodeType.ACCURACY, NodeType.EVASION,
            NodeType.LUCK, NodeType.WHITE_MAGIC, NodeType.BLACK_MAGIC, NodeType.SKILL,
            NodeType.SPECIAL, NodeType.LOCK, NodeType.EMPTY
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendSwatch(type = type, icons = icons)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = type.legendLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendSwatch(
    type: NodeType,
    icons: SphereIcons
) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val color = type.color()
        drawCircle(color = color, radius = r, center = center)
        if (type == NodeType.EMPTY) return@Canvas
        val glyphColor = glyphColorFor(color)
        val icon = if (type.isLock) icons.forLock(1) else icons.forType(type)
        icon?.let { drawNodeIcon(it, center, r, glyphColor, NodeSizing.LEGEND_ICON_SCALE) }
    }
}
