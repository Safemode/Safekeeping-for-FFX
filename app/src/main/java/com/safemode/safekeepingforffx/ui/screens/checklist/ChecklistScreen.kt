package com.safemode.safekeepingforffx.ui.screens.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.ui.components.Banner
import com.safemode.safekeepingforffx.ui.components.ChecklistItemRow
import com.safemode.safekeepingforffx.ui.components.ChecklistProgressHeader
import com.safemode.safekeepingforffx.ui.components.ScreenshotDialog
import com.safemode.safekeepingforffx.ui.components.SearchField
import com.safemode.safekeepingforffx.ui.components.SectionHeader
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.ui.util.rememberHeaderExpanded

/** Long enough to catch the eye after the scroll settles, short enough not to look like state. */
private const val HIGHLIGHT_DURATION_MS = 2_500L

/** Fits inside the progress row's 48dp action height, so the picker costs no vertical space. */
private val COMPACT_PILL_HEIGHT = 40.dp

/** One rendered row: either a section header or an entry. */
private sealed interface ChecklistRow {
    val key: String

    data class Section(val title: String) : ChecklistRow {
        override val key get() = "section_$title"
    }

    data class Entry(val item: ChecklistItem) : ChecklistRow {
        override val key get() = item.id
    }
}

/** groupBy keeps insertion order, so sections appear in the order they are declared. */
private fun checklistRows(items: List<ChecklistItem>): List<ChecklistRow> =
    items.groupBy { it.section }.flatMap { (section, entries) ->
        val header = section?.let { listOf(ChecklistRow.Section(it)) } ?: emptyList()
        header + entries.map { ChecklistRow.Entry(it) }
    }

@Composable
fun ChecklistScreen(
    category: ChecklistCategory,
    modifier: Modifier = Modifier,
    /** Item to scroll to and briefly highlight, set when arriving from a Home search result. */
    focusItemId: String? = null,
    /** Publishes a "dismiss the search" action while one is active, so back can clear it. */
    onSearchDismissChange: ((() -> Unit)?) -> Unit = {},
    viewModel: ChecklistViewModel = viewModel(
        // Keyed by category, otherwise navigating between two checklists would reuse the first
        // one's ViewModel and show the wrong list.
        key = category.id,
        factory = ChecklistViewModel.factory(category)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Store the id rather than the item so the dialog survives rotation.
    var shownItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val shownItem = state.items.firstOrNull { it.id == shownItemId }
    val hasScreenshots = state.items.any { it.imageRes != null }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val headerExpanded = rememberHeaderExpanded(listState)

    // Flattened once so section headers and rows share one index space - the only way to scroll to
    // a given item without guessing how many headers sit above it.
    val rows = remember(state.visibleItems) { checklistRows(state.visibleItems) }

    var highlightedId by remember { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Back clears this list's search before it does anything else, matching Home. Released as soon
    // as the field is empty, so back goes back to leaving the category as usual.
    DisposableEffect(state.isSearching) {
        onSearchDismissChange(
            if (state.isSearching) {
                {
                    viewModel.setQuery("")
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
            } else {
                null
            }
        )
        onDispose { onSearchDismissChange(null) }
    }

    // Re-ordering shuffles every row, so the old scroll offset means nothing afterwards. Tracked
    // against the last sort actually rendered rather than keyed on state.sort alone, so this can't
    // fire on first composition and fight the focus scroll below.
    var lastSort by remember { mutableStateOf(state.sort) }
    LaunchedEffect(state.sort) {
        if (state.sort != lastSort) {
            lastSort = state.sort
            listState.scrollToItem(0)
        }
    }

    // Keyed on the load flag rather than on `rows`, so ticking a checkbox doesn't yank the list
    // back to the focused item.
    LaunchedEffect(focusItemId, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val target = focusItemId ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is ChecklistRow.Entry && it.item.id == target }
        if (index < 0) return@LaunchedEffect

        listState.scrollToItem(index)
        highlightedId = target
        delay(HIGHLIGHT_DURATION_MS)
        highlightedId = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Progress stays pinned - it's the reason to look at the top of the screen at all. The
        // guidance below it collapses away once you're reading the list.
        if (category.trackProgress) {
            ChecklistProgressHeader(
                foundCount = state.foundCount,
                totalCount = state.totalCount,
                onReset = { showResetDialog = true },
                // Rides in the progress row's spare width rather than claiming a row of its own.
                // Re-ordering is a way of reading the list, so it has to stay reachable while you
                // scroll - but not at the price of another 60dp of permanent chrome.
                action = if (state.canSort) {
                    { SortSelector(sort = state.sort, onSortChange = viewModel::setSort) }
                } else {
                    null
                }
            )
        } else if (state.canSort) {
            // Reference-only lists have no progress row to ride in. None carry story stages today,
            // but the control shouldn't quietly vanish if one ever does.
            SortSelector(
                sort = state.sort,
                onSortChange = viewModel::setSort,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
            )
        }
        AnimatedVisibility(
            // An active search stays put even when scrolling down: hiding the field while it is
            // filtering the list would leave no way to see or undo what was typed.
            visible = headerExpanded || state.isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                placeholder = "Search ${category.label}"
            )
        }
        AnimatedVisibility(
            // Hidden outright when help is switched off in Settings, not merely collapsed.
            visible = headerExpanded && state.showHelp,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                state.note?.let { Banner(Icons.Outlined.Info, it) }
                // Only advertise the long-press where there is actually something to show.
                if (hasScreenshots) {
                    Banner(
                        Icons.Outlined.Image,
                        "Long-press an entry to see a screenshot of its location."
                    )
                }
            }
        }
        HorizontalDivider()

        if (state.hasNoMatches) {
            Text(
                text = "No entries in ${category.label} match \"${state.query.trim()}\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(state = listState) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is ChecklistRow.Section -> SectionHeader(row.title)
                    is ChecklistRow.Entry -> {
                        val entry = row.item
                        Box(
                            modifier = Modifier.background(
                                if (entry.id == highlightedId) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                        ) {
                            ChecklistItemRow(
                                item = entry,
                                onCheckedChange = { checked ->
                                    viewModel.setChecked(entry.id, checked)
                                },
                                onLongPress = { shownItemId = entry.id },
                                trackProgress = category.trackProgress
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (shownItem != null) {
        ScreenshotDialog(item = shownItem, onDismiss = { shownItemId = null })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset ${category.label}?") },
            text = {
                Text(
                    "The ${state.foundCount} checked ${
                        if (state.foundCount == 1) "item" else "items"
                    } in this list will be unchecked. Your other lists are untouched. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetCategory()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * The order picker, built like the Sphere Grid's grid picker so the two read as the same control:
 * a pill showing what you are looking at, tapped to swap it.
 *
 * Sized to sit inside the progress row's 48dp without stretching it, which is why the button says
 * only "Chronological" where the grid picker says "Standard Grid". The list underneath removes any
 * doubt anyway - stage headers or weapon headers tell you which order you are in at a glance.
 *
 * Each choice carries a line of explanation. Unlike the grid picker, the labels alone don't say
 * what changes.
 */
@Composable
private fun SortSelector(
    sort: ChecklistSort,
    onSortChange: (ChecklistSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { menu = true },
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
            modifier = Modifier.heightIn(max = COMPACT_PILL_HEIGHT)
        ) {
            Text(sort.label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change the order of this list")
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            ChecklistSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${option.label} order")
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSortChange(option)
                        menu = false
                    }
                )
            }
        }
    }
}

