package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.PathMode

/**
 * The planned path's colour on the grid. Drawn dashed, so it reads apart from every character's solid
 * path line even where the two run together, and orange so it matches none of the character colours.
 */
internal val PlannedPathColor = Color(0xFFFF9F1C)

/** How a path endpoint reads in the bar, e.g. "Strength +2" or "a blank node". */
internal fun NodeContent.endpointLabel(): String =
    if (this is NodeContent.Empty) "a blank node" else label()

/** The Key Spheres a path needs, e.g. "1x Lv.2 Key Sphere, 2x Lv.4 Key Sphere". */
internal fun GridPath.keySummary(): String =
    keysByLevel.entries.joinToString(", ") { (level, count) -> "${count}x Lv.$level Key Sphere" }

/** One line describing the plan's outcome: its cost, or why there is nothing to show yet. */
internal fun PathPlan.summary(): String {
    val path = path
    return when {
        fromId == null -> "Pick a start: long-press a node and choose \"Path from here\"."
        request.toId == null -> "Pick a destination: long-press a node and choose \"Path to here\"."
        path == null && request.mode == PathMode.NO_LOCKS ->
            "No way there without opening a lock. Try Shortest or Fewest keys."
        path == null -> "No path between these nodes."
        path.steps == 0 -> "Already there."
        else -> {
            val steps = if (path.steps == 1) "1 step" else "${path.steps} steps"
            val levels = if (path.sphereLevels == 1) "1 Sphere Level" else "${path.sphereLevels} Sphere Levels"
            "$steps, $levels"
        }
    }
}

/**
 * The bar under the grid while a path is planned: where it runs, the lock mode, what it costs and
 * which keys it needs, and the actions to step through it or save it as a point-to-point route.
 */
@Composable
internal fun PathPlanBar(
    plan: PathPlan,
    characterName: String,
    fromLabel: String?,
    toLabel: String?,
    onModeChange: (PathMode) -> Unit,
    onStartFromCharacter: () -> Unit,
    onStepThrough: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(PlannedPathColor)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Planned path", style = MaterialTheme.typography.labelSmall, color = muted)
                val from = if (plan.startsAtCharacter) characterName else fromLabel ?: "?"
                Text(
                    text = if (toLabel != null) "From $from to $toLabel" else "From $from",
                    style = MaterialTheme.typography.titleSmall,
                    color = onSurface,
                    maxLines = 2
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Stop planning a path", tint = onSurface)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PathMode.entries.forEach { mode ->
                FilterChip(
                    selected = plan.request.mode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.label) }
                )
            }
        }
        Text(plan.summary(), style = MaterialTheme.typography.bodyMedium, color = onSurface)
        val path = plan.path
        if (path != null && path.steps > 0) {
            Text(
                text = if (path.locks.isEmpty()) "No locks to open." else "Needs ${path.keySummary()}.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
            if (!plan.startsAtCharacter) {
                TextButton(onClick = onStartFromCharacter) { Text("Start from $characterName") }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSave, enabled = plan.hasSteps) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onStepThrough, enabled = plan.hasSteps) {
                Text("Step through")
            }
        }
    }
}
