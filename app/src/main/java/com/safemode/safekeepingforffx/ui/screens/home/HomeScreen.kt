package com.safemode.safekeepingforffx.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.ui.components.SearchField
import com.safemode.safekeepingforffx.ui.components.SectionHeader

@Composable
fun HomeScreen(
    categories: List<ChecklistCategory>,
    searchCategories: List<ChecklistCategory>,
    onCategoryClick: (String) -> Unit,
    onResultClick: (categoryId: String, itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Publishes a "dismiss the search" action while one is active, so back can clear it. */
    onSearchDismissChange: ((() -> Unit)?) -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(categories, searchCategories)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searching = query.isNotBlank()

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Only claimed while there is a search to back out of; released as soon as the field is empty
    // or Home leaves the composition, so back reverts to its normal behaviour.
    DisposableEffect(searching) {
        onSearchDismissChange(
            if (searching) {
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

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = viewModel::setQuery,
            placeholder = "Search all lists",
            modifier = Modifier.padding(top = 16.dp)
        )

        if (searching) {
            SearchResults(
                results = results,
                query = query.trim(),
                onResultClick = onResultClick
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Your progress",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${state.totalFound} of ${state.totalItems} collected across " +
                                "${state.categories.size} lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                items(state.categories, key = { it.route }) { progress ->
                    CategoryProgressCard(
                        progress = progress,
                        onClick = { onCategoryClick(progress.route) }
                    )
                }
            }
        }
    }
}

/**
 * Results are grouped under the list they came from, because "which list is this in" is most of
 * the answer when you are looking something up.
 */
@Composable
private fun SearchResults(
    results: List<SearchResult>,
    query: String,
    onResultClick: (categoryId: String, itemId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        Text(
            text = "Nothing in any list matches \"$query\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }

    LazyColumn(modifier = modifier) {
        item(key = "count") {
            Text(
                text = "${results.size} ${if (results.size == 1) "match" else "matches"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        results.groupBy { it.categoryLabel }.forEach { (label, hits) ->
            item(key = "section_$label") { SectionHeader(label) }

            items(hits, key = { "${it.categoryId}_${it.item.id}" }) { result ->
                SearchResultRow(
                    result = result,
                    onClick = { onResultClick(result.categoryId, result.item.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = result.item.title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = result.item.location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = result.item.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryProgressCard(
    progress: CategoryProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = progress.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (progress.isComplete) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = "${progress.foundCount} / ${progress.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}
