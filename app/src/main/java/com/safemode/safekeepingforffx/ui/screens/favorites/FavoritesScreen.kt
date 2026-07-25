package com.safemode.safekeepingforffx.ui.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.ui.components.ChecklistItemRow
import com.safemode.safekeepingforffx.ui.components.SectionHeader

/**
 * Everything starred anywhere in the app, grouped under the list it came from.
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
                style = MaterialTheme.typography.titleMedium
            )
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
            state.groups.forEach { group ->
                item(key = "group_${group.categoryId}") { SectionHeader(group.label) }

                items(
                    count = group.items.size,
                    // Ids are only unique within a category, so the group has to be part of the key
                    // or two lists sharing an id would collide.
                    key = { index -> "${group.categoryId}/${group.items[index].id}" }
                ) { index ->
                    val item = group.items[index]
                    ChecklistItemRow(
                        item = item,
                        onCheckedChange = {},
                        onLongPress = {},
                        // No checkbox: ticking belongs to the list that owns the item. The title is
                        // still struck through when it is done, so the state is not lost here.
                        trackProgress = false,
                        onClick = { onOpen(group.categoryId, item.id) },
                        onClickLabel = "Open ${item.title} in ${group.label}",
                        onFavoriteChange = { favorite ->
                            viewModel.setFavorite(group.categoryId, item.id, favorite)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
