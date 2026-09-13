package com.safemode.safekeepingforffx.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.domain.ProgressCount

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    onDestinationClick: (FfxDestination) -> Unit,
    /** Found and total per group id. A group missing from the map shows no count. */
    groupProgress: Map<String, ProgressCount>,
    /** The ids of the groups that are open. Every other group is collapsed. */
    expandedGroups: Set<String>,
    onToggleGroup: (groupId: String) -> Unit
) {
    // Checklist routes are registered with a `?focusId=` pattern, so compare only the part before
    // it. Matching the raw route left every checklist screen unhighlighted in the drawer.
    val currentBase = currentRoute?.substringBefore('?')

    ModalDrawerSheet {
        Text(
            text = "Safekeeping for FFX",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        HorizontalDivider()

        // Takes the available space and scrolls when the screen is too short to show every
        // destination, so nothing is clipped on low-resolution devices. On tall screens the
        // weight still pushes Settings to the bottom, matching the previous layout.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            drawerLayout.forEach { entry ->
                when (entry) {
                    is DrawerEntry.Single ->
                        DrawerItem(entry.destination, currentBase, onDestinationClick)

                    is DrawerEntry.Group -> DrawerGroup(
                        group = entry,
                        expanded = entry.id in expandedGroups,
                        progress = groupProgress[entry.id]?.takeIf { entry.showProgress },
                        currentBase = currentBase,
                        onToggle = { onToggleGroup(entry.id) },
                        onDestinationClick = onDestinationClick
                    )
                }
            }
        }

        HorizontalDivider()
        DrawerItem(settingsDestination, currentBase, onDestinationClick)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * A collapsible header and the screens under it. The header toggles rather than navigating, since a
 * group is not a screen of its own.
 */
@Composable
private fun DrawerGroup(
    group: DrawerEntry.Group,
    expanded: Boolean,
    progress: ProgressCount?,
    currentBase: String?,
    onToggle: () -> Unit,
    onDestinationClick: (FfxDestination) -> Unit
) {
    val containsCurrent = group.destinations.any { it.route == currentBase }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "drawerGroupChevron"
    )

    NavigationDrawerItem(
        label = { Text(group.label) },
        icon = { Icon(group.icon, contentDescription = null) },
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                progress?.let {
                    Text(
                        text = "${it.found} / ${it.total}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .rotate(chevronRotation)
                )
            }
        },
        // A collapsed group carries the highlight for the screen inside it, so the drawer always
        // shows where you are without having to open anything. Once open, the screen's own row does.
        selected = containsCurrent && !expanded,
        onClick = onToggle,
        modifier = Modifier
            .drawerRow()
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
    )

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            group.destinations.forEach { destination ->
                // Indented so the rows read as belonging to the header above them.
                DrawerItem(destination, currentBase, onDestinationClick, indent = 16.dp)
            }
        }
    }
}

@Composable
private fun DrawerItem(
    destination: FfxDestination,
    currentBase: String?,
    onClick: (FfxDestination) -> Unit,
    indent: Dp = 0.dp
) {
    NavigationDrawerItem(
        label = { Text(destination.label) },
        icon = { Icon(destination.icon, contentDescription = null) },
        selected = destination.route == currentBase,
        onClick = { onClick(destination) },
        modifier = Modifier.drawerRow(indent)
    )
}

/** Height of the highlight pill. Slimmer than the stock 56dp, but still a full 48dp touch target. */
private val DrawerPillHeight = 48.dp

/** The stock row height, kept so the list's spacing is unchanged by the slimmer pill. */
private val DrawerRowHeight = 56.dp

/**
 * Lays out one drawer row: inset from the sheet's edges like the stock item, pushed in by [indent]
 * for rows inside a group, and drawn [DrawerPillHeight] tall centred in a [DrawerRowHeight] slot -
 * the highlight is the item's own background, so shrinking the item is what slims the highlight.
 */
private fun Modifier.drawerRow(indent: Dp = 0.dp): Modifier =
    padding(NavigationDrawerItemDefaults.ItemPadding)
        .padding(start = indent)
        .padding(vertical = (DrawerRowHeight - DrawerPillHeight) / 2)
        .height(DrawerPillHeight)
