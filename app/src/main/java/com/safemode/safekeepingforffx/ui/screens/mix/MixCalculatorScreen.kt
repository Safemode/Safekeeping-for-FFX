package com.safemode.safekeepingforffx.ui.screens.mix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.MixCombination
import com.safemode.safekeepingforffx.data.reference.MixIngredient
import com.safemode.safekeepingforffx.data.reference.MixResult
import com.safemode.safekeepingforffx.ui.components.SearchField

/**
 * Pick two items, see what Rikku's Mix produces. Purely informational - nothing here is checkable
 * or counted, so the screen has no progress header and no reset.
 */
@Composable
fun MixCalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: MixViewModel = viewModel(factory = MixViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<MixSlot?>(null) }
    var pickingResult by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.failedToLoad) {
            LoadFailedNotice()
        }

        IngredientSlot(
            label = "First item",
            ingredient = state.first,
            onClick = { picking = MixSlot.FIRST }
        )
        IngredientSlot(
            label = "Second item",
            ingredient = state.second,
            onClick = { picking = MixSlot.SECOND }
        )

        ResultCard(state = state, onClick = { pickingResult = true })

        if (state.first != null || state.second != null) {
            TextButton(
                onClick = viewModel::clear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear")
            }
        }

        state.browsedResult?.let { browsed ->
            CombinationsSection(
                result = browsed,
                combinations = state.combinations,
                onDismiss = viewModel::stopBrowsing
            )
        }
    }

    picking?.let { slot ->
        PickerDialog(
            title = "Choose an item",
            placeholder = "Search items",
            entries = state.ingredients,
            label = { it.name },
            key = { it.id },
            onPick = { ingredient ->
                viewModel.choose(slot, ingredient)
                picking = null
            },
            onDismiss = { picking = null }
        )
    }

    if (pickingResult) {
        PickerDialog(
            title = "Choose a result",
            placeholder = "Search results",
            entries = state.results,
            label = { it.name },
            key = { it.id },
            onPick = { result ->
                viewModel.browse(result)
                pickingResult = false
            },
            onDismiss = { pickingResult = false }
        )
    }
}

@Composable
private fun IngredientSlot(
    label: String,
    ingredient: MixIngredient?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = ingredient?.name ?: "Tap to choose",
                style = MaterialTheme.typography.titleMedium,
                color = if (ingredient == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ResultCard(
    state: MixUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val result = state.result

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (result != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Result",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                result != null -> {
                    Text(
                        text = result.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Both picked and still nothing: the table has no entry for this pair. Saying so
                // is the honest answer - anything else would be inventing a result.
                state.isUnknownCombination -> Text(
                    text = "No recipe recorded for this pair.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                else -> Text(
                    text = "Choose two items to see what they make.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Tap to look up a result instead",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * The reverse lookup: every way to make one result. Rendered as plain rows inside the screen's
 * existing scroll rather than a nested lazy list, which would fight it for scroll gestures.
 */
@Composable
private fun CombinationsSection(
    result: MixResult,
    combinations: List<MixCombination>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ways to make ${result.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Hide") }
        }

        Text(
            text = result.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (combinations.isEmpty()) {
            Text(
                text = "No combination in the table produces this.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        combinations.forEach { combination ->
            HorizontalDivider()
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = combination.anchor.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "+ ${combination.partners.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun <T> PickerDialog(
    title: String,
    placeholder: String,
    entries: List<T>,
    label: (T) -> String,
    key: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, entries) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            entries
        } else {
            entries.filter { label(it).contains(needle, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = placeholder,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                if (matches.isEmpty()) {
                    Text(
                        text = "Nothing matches \"${query.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(matches, key = { key(it) }) { entry ->
                            Text(
                                text = label(entry),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(entry) }
                                    .padding(vertical = 14.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LoadFailedNotice(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
            Text(
                text = "The Mix table could not be loaded, so no combination will return a " +
                    "result.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
