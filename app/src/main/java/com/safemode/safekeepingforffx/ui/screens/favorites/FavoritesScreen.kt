package com.safemode.safekeepingforffx.ui.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.ui.components.ChecklistItemRow
import com.safemode.safekeepingforffx.ui.components.SectionHeader
import com.safemode.safekeepingforffx.ui.components.SortSelector

/** One rendered row: either a list's name or a starred item. */
private sealed interface FavoriteRow {
    val key: String

    data class Header(val label: String) : FavoriteRow {
        override val key get() = "header_$label"
    }

    data class Entry(val entry: FavoriteEntry) : FavoriteRow {
        // Item ids are only unique within a category, so the list has to be part of the key or two
        // of them sharing an id would collide.
        override val key get() = "${entry.categoryId}/${entry.item.id}"
    }
}

/**
 * Headers belong to grouping, so they only exist in one order. In the other the rows carry their
 * list on a badge instead, since "recently added" deliberately mixes the lists together and there
 * is no run of rows for a header to sit above.
 *
 * groupBy keeps insertion order, and the entries already arrive in drawer order.
 */
private fun favoriteRows(
    entries: List<FavoriteEntry>,
    sort: FavoritesSort
): List<FavoriteRow> = when (sort) {
    FavoritesSort.GROUPED -> entries.groupBy { it.categoryId }.flatMap { (_, group) ->
        listOf(FavoriteRow.Header(group.first().categoryLabel)) + group.map { FavoriteRow.Entry(it) }
    }

    FavoritesSort.RECENT -> entries.map { FavoriteRow.Entry(it) }
}

/**
 * Everything starred anywhere in the app, either grouped under the list it came from or in the order
 * it was starred.
 *
 * A shortlist rather than a sixth tracker: a row here says what it is and takes you to it, but the
 * ticking and the counting stay in the list that owns them. That is why a row leads somewhere
 * instead of toggling - the same tap in the same place would otherwise mean two different things
 * depending on which screen you were looking at.
 */
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    /** Opens a favorite where it lives, scrolled to it. */
    onOpen: (categoryId: String, itemId: String) -> Unit = { _, _ -> },
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val rows = remember(state.entries, state.sort) { favoriteRows(state.entries, state.sort) }

    // Re-ordering shuffles every row, so the old scroll offset means nothing afterwards. The stored
    // order lands after the screen is composed, so the first loaded value is recorded as the
    // starting point rather than mistaken for the player changing the order.
    var lastSort by remember { mutableStateOf<FavoritesSort?>(null) }
    LaunchedEffect(state.sort, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val previous = lastSort
        lastSort = state.sort
        if (previous != null && previous != state.sort) {
            listState.scrollToItem(0)
        }
    }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (state.totalCount) {
                    0 -> "Nothing starred yet"
                    1 -> "1 favorite"
                    else -> "${state.totalCount} favorites"
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            // Rides in the count row's spare width rather than claiming a row of its own, the same
            // way the checklists carry theirs. Hidden while there is nothing to order.
            if (state.totalCount > 0) {
                SortSelector(
                    selected = state.sort,
                    options = FavoritesSort.entries,
                    onSelect = viewModel::setSort
                )
            }
        }
        HorizontalDivider()

        if (state.isEmpty) {
            // Says where the control is rather than only that the list is empty: the star is the
            // one thing a player has to find before this screen can ever fill up.
            Text(
                text = "Tap the star on any entry in a list to keep it here. Favorites gathers " +
                    "them from every list in one place, so the handful you are working on right " +
                    "now doesn't need hunting for.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        LazyColumn(state = listState) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is FavoriteRow.Header -> SectionHeader(row.label)
                    is FavoriteRow.Entry -> {
                        val entry = row.entry
                        ChecklistItemRow(
                            item = entry.item,
                            onCheckedChange = {},
                            onLongPress = {},
                            // No checkbox: ticking belongs to the list that owns the item. The title
                            // is still struck through when it is done, so nothing is lost here.
                            trackProgress = false,
                            onClick = { onOpen(entry.categoryId, entry.item.id) },
                            onClickLabel = "Open ${entry.item.title} in ${entry.categoryLabel}",
                            onFavoriteChange = { favorite ->
                                viewModel.setFavorite(entry.categoryId, entry.item.id, favorite)
                            },
                            // Only where no header above the row already says it.
                            sourceLabel = entry.categoryLabel.takeIf {
                                state.sort == FavoritesSort.RECENT
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
