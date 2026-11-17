package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository

/**
 * The name the save dialog suggests for a path, e.g. "Tidus to MP +20". The destination is what
 * tells two saved paths apart; the start is usually just wherever the character was.
 */
internal fun pointRouteName(
    characterName: String,
    path: GridPath,
    nodesById: Map<String, SphereGridNode>,
    state: SphereGridUiState
): String {
    val end = nodesById[path.nodeIds.last()] ?: return characterName
    val content = state.current(end)
    val label = if (content is NodeContent.Empty) {
        "blank node"
    } else {
        content.label()
    }
    return "$characterName to $label"
}

/** Header shown while stepping through a point-to-point route: its name, a way out, and Save. */
@Composable
internal fun PointRouteBar(
    view: PointRouteViewState,
    onExit: () -> Unit,
    onSave: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = "Stop stepping through", tint = onSurface)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (view.isSaved) "Point-to-point route" else "Planned path (not saved)",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
                Text(
                    view.name ?: "${view.character.displayName}'s path",
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface,
                    maxLines = 1
                )
            }
            if (!view.isSaved) {
                OutlinedButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save", maxLines = 1)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp)
        ) {
            ColorDot(view.character.activationColor())
            Spacer(Modifier.width(8.dp))
            Text(
                "${view.character.displayName} · ${view.mode.label} · ${view.path.costSummary()}",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
    }
}

/** The point-to-point routes library: step through, rename or delete a saved path. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PointRoutesSheet(
    routes: List<SphereGridRepository.PointRoute>,
    onDismiss: () -> Unit,
    onView: (Long) -> Unit,
    onRename: (SphereGridRepository.PointRoute) -> Unit,
    onDelete: (SphereGridRepository.PointRoute) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Point-to-Point Routes", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(8.dp))
            if (routes.isEmpty()) {
                Text(
                    "No point-to-point routes yet. Long-press a node and choose \"Path to here\", " +
                        "then Save from the path bar to keep it here and step through it later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(routes, key = { it.id }) { route ->
                        PointRouteRow(
                            route = route,
                            onView = { onView(route.id) },
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

/** One row in the point-to-point library: tap to step through, overflow to rename or delete. */
@Composable
private fun PointRouteRow(
    route: SphereGridRepository.PointRoute,
    onView: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
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
                "${route.gridType.label} grid · ${route.character.displayName} · ${route.mode.label}",
                style = MaterialTheme.typography.labelMedium,
                color = muted
            )
            Text(route.path.costSummary(), style = MaterialTheme.typography.labelMedium, color = muted)
            Text(
                "Updated ${formatRouteTimestamp(route.updatedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = muted
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Route actions")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Step through") }, onClick = { menu = false; onView() })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** Names a path and saves it to the point-to-point library. */
@Composable
internal fun SavePointRouteDialog(
    suggestedName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by rememberSaveable(suggestedName) { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save point-to-point route") },
        text = {
            ScrollingDialogBody {
                Text(
                    "Keeps this path as it is now, so it can be stepped through later from " +
                        "Point-to-Point Routes in the menu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Route name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
