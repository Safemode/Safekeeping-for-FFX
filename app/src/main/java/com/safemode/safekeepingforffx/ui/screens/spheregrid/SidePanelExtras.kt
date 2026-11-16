package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.CharacterStatus
import com.safemode.safekeepingforffx.data.reference.StatLine
import kotlinx.coroutines.launch

/**
 * The selected character's totals, kept in view in the side panel so a build can be watched taking
 * shape as nodes are tapped. Ten stats two to a row, under a line saying how much of the grid they
 * have taken. The Character Status sheet holds the full breakdown and the ability lists.
 */
@Composable
internal fun PanelStatusSummary(status: CharacterStatus, icons: SphereIcons) {
    val nodes = status.activatedNodes
    val learned = status.abilities.values.sumOf { it.learned.size }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            "${status.character.displayName}'s stats",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = (if (nodes == 1) "1 node with content" else "$nodes nodes with content") +
                " · " + (if (learned == 1) "1 ability learned" else "$learned abilities learned"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.padding(top = 8.dp))
        status.attributes.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pair.forEach { line ->
                    StatCell(line = line, icons = icons, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One stat: its sphere, its short name, and its total, picked out when it has hit the cap. */
@Composable
private fun StatCell(line: StatLine, icons: SphereIcons, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        NodeSwatch(type = line.attribute, icons = icons)
        Spacer(Modifier.width(8.dp))
        Text(
            line.attribute.legendLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            line.total.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (line.isCapped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * The route and build actions from the overflow menu, a tap away in the side panel. The ones that
 * wipe progress stay in the menu, where they are harder to hit by accident.
 */
@Composable
internal fun PanelShortcuts(
    canSaveRoute: Boolean,
    canShare: Boolean,
    onOpenRoutes: () -> Unit,
    onOpenPointRoutes: () -> Unit,
    onSaveRoute: () -> Unit,
    onShareBuild: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenRoutes, modifier = Modifier.weight(1f)) {
                Text("Routes", maxLines = 1)
            }
            OutlinedButton(onClick = onOpenPointRoutes, modifier = Modifier.weight(1f)) {
                Text("Point-to-Point", maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSaveRoute, enabled = canSaveRoute, modifier = Modifier.weight(1f)) {
                Text("Save as route", maxLines = 1)
            }
            OutlinedButton(onClick = onShareBuild, enabled = canShare, modifier = Modifier.weight(1f)) {
                Text("Share build", maxLines = 1)
            }
        }
    }
}

/**
 * A slim "More below" bar pinned under the side panel's scrolling area while there is more to scroll
 * to, such as the shortcut buttons under the stats. It sits below the content rather than over it,
 * so it never hides anything, and it slides away once the end is in view. Tapping it scrolls there.
 */
@Composable
internal fun MoreBelowBar(scroll: ScrollState) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = scroll.canScrollForward,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider()
            TextButton(
                onClick = { scope.launch { scroll.animateScrollTo(scroll.maxValue) } },
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text("More below", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
