package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.AbilityGroup
import com.safemode.safekeepingforffx.data.reference.CharacterStatus
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.StatLine
import kotlinx.coroutines.launch

/**
 * The sections the status sheet pages through, in swipe order: the stat table first, then one page
 * per ability family. [family] is null for the attributes page, which has no family of its own.
 */
private enum class StatusPage(val title: String, val family: NodeType?) {
    ATTRIBUTES("Attributes", null),
    SKILLS("Skills", NodeType.SKILL),
    ABILITIES("Abilities", NodeType.SPECIAL),
    WHITE_MAGIC("White Magic", NodeType.WHITE_MAGIC),
    BLACK_MAGIC("Black Magic", NodeType.BLACK_MAGIC)
}

/**
 * A read-only look at what the grid in view has made of a character: base stats plus everything
 * their activated nodes add, then the abilities they have learned family by family.
 *
 * The sheet holds no state of its own beyond which page is showing - [status] is recomputed upstream
 * from whatever the canvas is drawing, so switching character, editing a node or stepping a route
 * replay updates these numbers underneath the open sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterStatusSheet(
    status: CharacterStatus?,
    gridLabel: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (status == null) {
            Text(
                text = "No grid is loaded, so there is nothing to total up yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            )
            return@ModalBottomSheet
        }

        val pages = remember { StatusPage.entries }
        val pagerState = rememberPagerState(pageCount = { pages.size })
        val scope = rememberCoroutineScope()
        val icons = rememberSphereIcons()

        Column(modifier = Modifier.fillMaxSize()) {
            StatusHeader(status = status, gridLabel = gridLabel)

            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 12.dp
            ) {
                pages.forEachIndexed { index, page ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(page.title, maxLines = 1) }
                    )
                }
            }
            HorizontalDivider()

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { index ->
                val page = pages[index]
                if (page.family == null) {
                    AttributesPage(lines = status.attributes, icons = icons)
                } else {
                    AbilitiesPage(group = status.group(page.family), icons = icons)
                }
            }
        }
    }
}

/** Who this is and how much of the grid they have walked, so the totals below have a context. */
@Composable
private fun StatusHeader(status: CharacterStatus, gridLabel: String) {
    val nodes = status.activatedNodes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(status.character.activationColor(), CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(status.character.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "$gridLabel grid · " + if (nodes == 1) "1 node activated" else "$nodes nodes activated",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The ten stats, each showing where it started, what the path added, and where it lands. */
@Composable
private fun AttributesPage(lines: List<StatLine>, icons: SphereIcons) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 32.dp
        )
    ) {
        items(lines, key = { it.attribute.name }) { line ->
            StatRow(line = line, icons = icons)
            HorizontalDivider()
        }
    }
}

@Composable
private fun StatRow(line: StatLine, icons: SphereIcons) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NodeSwatch(type = line.attribute, icons = icons)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(line.attribute.attributeName(), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${line.base} base · +${line.fromGrid} from grid",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = line.total.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (line.isCapped) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (line.isCapped) {
                // Anything past the cap is sphere spend that buys nothing - worth calling out on a
                // screen whose whole job is planning where the spheres go.
                Text(
                    text = if (line.wasted > 0) "max · ${line.wasted} wasted" else "max",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * One ability family: what the character knows, then what this grid still holds for them. The
 * remainder is the planning half - it says what taking more of this grid would actually teach.
 */
@Composable
private fun AbilitiesPage(group: AbilityGroup, icons: SphereIcons) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text(
                text = "${group.learned.size} of ${group.availableOnGrid} on this grid",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
        }

        if (group.learned.isEmpty()) {
            item {
                Text(
                    text = "Nothing learned yet - none of this grid's " +
                        "${group.family.attributeName()} nodes have been activated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(group.learned, key = { "learned_$it" }) { name ->
                AbilityRow(name = name, family = group.family, icons = icons, learned = true)
            }
        }

        if (group.remaining.isNotEmpty()) {
            item {
                Spacer(Modifier.size(20.dp))
                Text(
                    text = "Still on the grid",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
            }
            items(group.remaining, key = { "remaining_$it" }) { name ->
                AbilityRow(name = name, family = group.family, icons = icons, learned = false)
            }
        }
    }
}

@Composable
private fun AbilityRow(name: String, family: NodeType, icons: SphereIcons, learned: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // An unlearned ability keeps its family swatch but sits back, so the two groups read apart
        // at a glance without a second colour scheme.
        NodeSwatch(type = family, icons = icons, dimmed = !learned)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (learned) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** The node's own sphere, drawn small - the same fill and icon the grid and legend use. */
@Composable
private fun NodeSwatch(type: NodeType, icons: SphereIcons, dimmed: Boolean = false) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val color = type.color()
        drawCircle(color = color, radius = radius, center = center, alpha = if (dimmed) 0.35f else 1f)
        icons.forType(type)?.let {
            drawNodeIcon(
                it,
                center,
                radius,
                type.iconColor(color).copy(alpha = if (dimmed) 0.45f else 1f),
                NodeSizing.LEGEND_ICON_SCALE,
                type.iconOutline(),
                type.iconOutlineFactor()
            )
        }
    }
}
