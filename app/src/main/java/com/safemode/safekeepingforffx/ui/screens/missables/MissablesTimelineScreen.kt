package com.safemode.safekeepingforffx.ui.screens.missables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.ui.components.Banner
import com.safemode.safekeepingforffx.ui.components.ChecklistItemRow
import com.safemode.safekeepingforffx.ui.components.SectionHeader

/** One rendered row: a stage header, or a flagged entry filed under it. */
private sealed interface TimelineRow {
    val key: String

    data class Stage(val label: String) : TimelineRow {
        override val key get() = "stage_$label"
    }

    data class Entry(val entry: TimelineItem) : TimelineRow {
        // Same item can only appear once, and its id is unique across categories by construction.
        override val key get() = "${entry.categoryId}_${entry.item.id}"
    }
}

private fun timelineRows(stages: List<TimelineStage>): List<TimelineRow> =
    stages.flatMap { stage ->
        listOf(TimelineRow.Stage(stage.label)) + stage.items.map { TimelineRow.Entry(it) }
    }

/**
 * A read-only, story-ordered digest of everything the other lists flag as missable or guarded.
 * Tapping a row opens that item where it lives, so it can be checked off there with full context;
 * already-collected items show struck through here, so the timeline doubles as an at-a-glance
 * "what's still at risk" view.
 */
@Composable
fun MissablesTimelineScreen(
    categories: List<ChecklistCategory>,
    /** Opens the flagged item in the list that owns it, scrolled to it. */
    onOpen: (categoryId: String, itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MissablesTimelineViewModel = viewModel(
        factory = MissablesTimelineViewModel.factory(categories)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rows = remember(state.stages) { timelineRows(state.stages) }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.showHelp) {
            Banner(
                Icons.Outlined.Info,
                "Everything that can be lost or blocked, in the order you reach it. Tap a row to open " +
                    "it in its own list. Collected items show struck through."
            )
        }

        if (!state.isLoading) {
            Text(
                text = "${state.collectedCount} of ${state.totalCount} collected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        HorizontalDivider()

        if (state.isEmpty) {
            Text(
                text = "Nothing here is at risk on this game version. Switch to the International / " +
                    "HD Remaster version in Settings to also see Dark-Aeon-guarded items.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is TimelineRow.Stage -> SectionHeader(row.label)
                    is TimelineRow.Entry -> {
                        val entry = row.entry
                        ChecklistItemRow(
                            item = entry.item,
                            // Read-only here: the check lives in the owning list, so tapping opens
                            // that list rather than toggling a copy the timeline doesn't own.
                            onCheckedChange = {},
                            onLongPress = {},
                            trackProgress = false,
                            onClick = { onOpen(entry.categoryId, entry.item.id) },
                            onClickLabel = "Open in ${entry.categoryLabel}",
                            sourceLabel = entry.categoryLabel
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
