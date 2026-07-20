package com.safemode.safekeepingforffx.ui.screens.itemlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.ui.screens.checklist.ChecklistScreen

/**
 * The item list is an ordinary reference category that happens to come from a CSV rather than from
 * Kotlin, so once loaded it hands straight off to [ChecklistScreen]. That is what gives it the same
 * search bar, the same collapse-on-scroll and the same tag rendering as every other category, for
 * free.
 */
@Composable
fun ItemListScreen(
    modifier: Modifier = Modifier,
    focusItemId: String? = null,
    onSearchDismissChange: ((() -> Unit)?) -> Unit = {},
    viewModel: ItemListViewModel = viewModel(factory = ItemListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val category = state.category

    when {
        state.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        category == null -> Text(
            text = "The item list could not be loaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )

        else -> ChecklistScreen(
            category = category,
            modifier = modifier,
            focusItemId = focusItemId,
            onSearchDismissChange = onSearchDismissChange
        )
    }
}
