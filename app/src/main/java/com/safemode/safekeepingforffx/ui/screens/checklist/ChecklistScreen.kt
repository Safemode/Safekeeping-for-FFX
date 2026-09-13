package com.safemode.safekeepingforffx.ui.screens.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.safemode.safekeepingforffx.ui.components.SortSelector
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.ui.screens.spheregrid.CharacterRow
import com.safemode.safekeepingforffx.ui.util.rememberLeadingItemsScrollRoom

/** Long enough to catch the eye after the scroll settles, short enough not to look like state. */
private const val HIGHLIGHT_DURATION_MS = 2_500L

/**
 * Where a reference row leads, and how to say so - to a screen reader through [label], and on screen
 * through [icon]. A null icon leaves the row unmarked, which is right when every row in the list
 * leads to the same kind of place and the mark would say nothing.
 */
data class ItemAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

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
    /**
     * What tapping a row does, for reference lists that lead somewhere. Returning null for an item
     * leaves that row inert, which is what keeps the item list from offering a dead-end tap on
     * something no fiend carries. Ignored by tracked lists, where a tap already ticks the box.
     */
    itemAction: (ChecklistItem) -> ItemAction? = { null },
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

    // Flattened once so section headers and rows share one index space - the only way to scroll to
    // a given item without guessing how many headers sit above it.
    val rows = remember(state.visibleItems) { checklistRows(state.visibleItems) }

    // The search field and the advice banners are the first items *inside* the list, so they scroll
    // away with the content instead of being pinned chrome. That is deliberate: a pinned header sits
    // in its own box above the list, and collapsing it hands its height back to the list, which on a
    // short list is enough to refit the content and bounce it back to the top - a loop that could
    // leave a barely-overflowing list frozen, unable to scroll at all. As list items they just
    // scroll off. The trade is that scroll-to-item has to count them, hence [leadingItemCount].
    // Counts the search field and the divider (both always present) plus whichever banners show.
    // The no-matches row is excluded on purpose: it only exists while searching, and the focus
    // scroll that reads this never runs then.
    val leadingItemCount = 2 +
        (if (state.showHelp && state.note != null) 1 else 0) +
        (if (state.showHelp && hasScreenshots) 1 else 0)

    // The other trade: a list only a little taller than the screen, like Destruction Spheres, runs
    // out of scroll before its leading items have cleared, leaving the bottom of the note stuck
    // under the progress bar. A spacer after the last row makes up that shortfall, and is zero on
    // any list long enough or short enough not to need it.
    val scrollRoom = rememberLeadingItemsScrollRoom(listState, leadingItemCount)

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
    // fire on arrival and fight the focus scroll below. The stored sort is read asynchronously and
    // lands after the screen is already composed, so the first loaded value is recorded as the
    // starting point rather than mistaken for the player changing the order.
    var lastSort by remember { mutableStateOf<ChecklistSort?>(null) }
    LaunchedEffect(state.sort, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val previous = lastSort
        lastSort = state.sort
        if (previous != null && previous != state.sort) {
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

        // Offset past the search field and banners that now lead the list.
        listState.scrollToItem(leadingItemCount + index)
        highlightedId = target
        delay(HIGHLIGHT_DURATION_MS)
        highlightedId = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Above the progress it drives, and pinned with it: the count below is only ever this
        // character's, so the switch belongs where you can see whose count you are reading. The same
        // row the Sphere Grid Planner uses, so a character looks the same in both places.
        state.character?.let { selected ->
            CharacterRow(
                selected = selected,
                onSelect = viewModel::setCharacter,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Progress stays pinned - it's the reason to look at the top of the screen at all, and it
        // carries the reset and the sort control. Everything below it lives in the list and scrolls.
        if (category.trackProgress) {
            ChecklistProgressHeader(
                foundCount = state.foundCount,
                totalCount = state.totalCount,
                onReset = { showResetDialog = true },
                // Rides in the progress row's spare width rather than claiming a row of its own.
                action = if (state.canSort) {
                    {
                        SortSelector(
                            selected = state.sort,
                            options = ChecklistSort.entries,
                            onSelect = viewModel::setSort
                        )
                    }
                } else {
                    null
                }
            )
        } else if (state.canSort) {
            // Reference-only lists have no progress row to ride in. None carry story stages today,
            // but the control shouldn't quietly vanish if one ever does.
            SortSelector(
                selected = state.sort,
                options = ChecklistSort.entries,
                onSelect = viewModel::setSort,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider()

        LazyColumn(state = listState) {
            // Leading items - kept in sync with [leadingItemCount] above, which the focus scroll
            // relies on to land on the right row.
            item(key = "search") {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::setQuery,
                    placeholder = "Search ${category.label}"
                )
            }
            if (state.showHelp) {
                state.note?.let { note ->
                    item(key = "banner_note") { Banner(Icons.Outlined.Info, note) }
                }
                // Only advertise the long-press where there is actually something to show.
                if (hasScreenshots) {
                    item(key = "banner_screenshot") {
                        Banner(
                            Icons.Outlined.Image,
                            "Long-press an entry to see a screenshot of its location."
                        )
                    }
                }
            }
            item(key = "leading_divider") { HorizontalDivider() }

            if (state.hasNoMatches) {
                item(key = "no_matches") {
                    Text(
                        text = "No entries in ${category.label} match \"${state.query.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

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
                            val action = itemAction(entry)
                            ChecklistItemRow(
                                item = entry,
                                onCheckedChange = { checked ->
                                    viewModel.setChecked(entry.id, checked)
                                },
                                onLongPress = { shownItemId = entry.id },
                                trackProgress = category.trackProgress,
                                onClick = action?.onClick,
                                onClickLabel = action?.label,
                                onClickIcon = action?.icon,
                                onFavoriteChange = { favorite ->
                                    viewModel.setFavorite(entry.id, favorite)
                                }
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }

            // Must stay the last item - the room is measured on the assumption that it is.
            item(key = "scroll_room") { Spacer(Modifier.height(scrollRoom)) }
        }
    }

    if (shownItem != null) {
        ScreenshotDialog(item = shownItem, onDismiss = { shownItemId = null })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            // On a per-character list only the selected character is cleared, so the dialog names
            // them rather than letting "this list" sound like everyone's progress.
            title = {
                Text(
                    state.character
                        ?.let { "Reset ${category.label} for ${it.displayName}?" }
                        ?: "Reset ${category.label}?"
                )
            },
            text = {
                Text(
                    "The ${state.foundCount} checked ${
                        if (state.foundCount == 1) "item" else "items"
                    } ${
                        state.character?.let { "for ${it.displayName} will be unchecked. Other " +
                            "characters and your other lists are untouched. " }
                            ?: "in this list will be unchecked. Your other lists are untouched. "
                    }This can't be undone."
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

