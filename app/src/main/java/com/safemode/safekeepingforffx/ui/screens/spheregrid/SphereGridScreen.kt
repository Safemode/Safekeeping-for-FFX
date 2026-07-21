package com.safemode.safekeepingforffx.ui.screens.spheregrid

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

private const val STAT_RADIUS = 15f
private const val ABILITY_RADIUS = 24f
private const val LOCK_RADIUS = 13f

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
                            "Share build code"
                        )
                    )
                    Toast.makeText(context, "Build code copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                is SphereGridEvent.ImportDone -> {
                    showImportDialog = false
                    Toast.makeText(context, event.summary.message(), Toast.LENGTH_LONG).show()
                }
                is SphereGridEvent.ImportFailed ->
                    Toast.makeText(context, event.reason, Toast.LENGTH_LONG).show()
            }
        }
    }

    val nodesById = remember(state.grid) { state.grid.nodes.associateBy { it.id } }

    val textMeasurer = rememberTextMeasurer()
    val glyphCache = remember(textMeasurer) {
        GlyphLetters.associateWith {
            textMeasurer.measure(it, TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp))
        }
    }
    // Value/name labels are measured lazily and memoized - there are only a few dozen distinct
    // strings, so each is measured once and reused every frame.
    val labelStyle = remember { TextStyle(fontWeight = FontWeight.Medium, fontSize = 30.sp) }
    val labelCache = remember { mutableMapOf<String, TextLayoutResult>() }

    Column(modifier = modifier.fillMaxSize()) {
        SelectorBar(
            gridType = state.gridType,
            onGridTypeChange = viewModel::setGridType,
            hasEdits = state.hasEdits,
            characterHasPath = state.characterHasPath,
            characterName = state.character.displayName,
            onRevertEdits = {
                confirm = ConfirmAction(
                    title = "Revert all edits?",
                    message = "Every node's content goes back to vanilla on both grids. This can't " +
                        "be undone.",
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
            onImportBuild = { showImportDialog = true }
        )
        CharacterRow(selected = state.character, onSelect = viewModel::setCharacter)
        HorizontalDivider()

        if (state.showHelp) {
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
                    "own colour; edited nodes carry a gold dot."
            )
        }

        Legend(glyphCache = glyphCache)
        HorizontalDivider()

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                !state.gridType.isAvailable -> ExpertPlaceholder(
                    onShowStandard = { viewModel.setGridType(GridType.STANDARD) }
                )
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                else -> GridCanvas(
                    grid = state.grid,
                    overrides = state.overrides,
                    activated = state.activated,
                    characterColor = state.character.activationColor(),
                    selectedId = selectedNodeId,
                    tapActivates = state.tapActivates,
                    glyphCache = glyphCache,
                    textMeasurer = textMeasurer,
                    labelCache = labelCache,
                    labelStyle = labelStyle,
                    onActivate = { id -> nodesById[id]?.let(viewModel::toggleActivation) },
                    onDetails = { selectedNodeId = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val selected = selectedNodeId?.let { nodesById[it] }
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedNodeId = null },
            sheetState = sheetState
        ) {
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
    onImportBuild: () -> Unit
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

/** The pan/zoom canvas. Transform is screen = world * scale + offset. */
@Composable
private fun GridCanvas(
    grid: GridData,
    overrides: Map<String, NodeContent>,
    activated: Set<String>,
    characterColor: Color,
    selectedId: String?,
    tapActivates: Boolean,
    glyphCache: Map<String, TextLayoutResult>,
    textMeasurer: TextMeasurer,
    labelCache: MutableMap<String, TextLayoutResult>,
    labelStyle: TextStyle,
    onActivate: (String) -> Unit,
    onDetails: (String) -> Unit,
    modifier: Modifier = Modifier
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
    var fitted by remember { mutableStateOf(false) }

    LaunchedEffect(canvasSize, bounds) {
        if (fitted || canvasSize.width == 0 || canvasSize.height == 0 || bounds.width == 0f) {
            return@LaunchedEffect
        }
        val fit = minOf(canvasSize.width / bounds.width, canvasSize.height / bounds.height) * 0.95f
        scale = fit
        minScale = fit * 0.7f
        maxScale = fit * 14f
        offset = Offset(
            x = canvasSize.width / 2f - (bounds.minX + bounds.width / 2f) * fit,
            y = canvasSize.height / 2f - (bounds.minY + bounds.height / 2f) * fit
        )
        fitted = true
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
                    scale = newScale
                    offset = Offset(
                        x = centroid.x - worldX * newScale + pan.x,
                        y = centroid.y - worldY * newScale + pan.y
                    )
                }
            }
            .pointerInput(nodes, tapActivates) {
                fun nodeAt(pos: Offset): String? {
                    val worldX = (pos.x - offset.x) / scale
                    val worldY = (pos.y - offset.y) / scale
                    val touch = (44f / scale).coerceAtLeast(ABILITY_RADIUS)
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
                        nodeAt(pos)?.let { if (tapActivates) onActivate(it) else onDetails(it) }
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
            val content = overrides[node.id] ?: node.original
            val dtype = content.displayType
            val worldR = when {
                dtype.isAbility -> ABILITY_RADIUS
                dtype.isLock -> LOCK_RADIUS
                else -> STAT_RADIUS
            }
            val r = (worldR * scale).coerceAtLeast(1.5f)
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
                if (dtype.isLock) {
                    val level = (node.original as? NodeContent.Lock)?.level
                    glyphCache[level?.toString()]?.let { drawGlyphText(it, center, r, glyphColor) }
                } else {
                    val letter = dtype.glyphLetter()
                    if (letter != null) {
                        glyphCache[letter]?.let { drawGlyphText(it, center, r, glyphColor) }
                    } else {
                        drawNodeSymbol(dtype, center, r, glyphColor, background = color)
                    }
                }
            }

            // The value or ability name, as a small label beside the node.
            if (r >= LabelTuning.MIN_RADIUS) {
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
    glyphCache: Map<String, TextLayoutResult>,
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
                LegendSwatch(type = type, glyphCache = glyphCache)
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
    glyphCache: Map<String, TextLayoutResult>
) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val color = type.color()
        drawCircle(color = color, radius = r, center = center)
        if (type == NodeType.EMPTY) return@Canvas
        val glyphColor = glyphColorFor(color)
        val letter = if (type.isLock) "1" else type.glyphLetter()
        if (letter != null) {
            glyphCache[letter]?.let { drawGlyphText(it, center, r, glyphColor) }
        } else {
            drawNodeSymbol(type, center, r, glyphColor, background = color)
        }
    }
}
