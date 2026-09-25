package com.safemode.safekeepingforffx.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    /** The drawer's order, used as the default when the player hasn't set one and by Reset. */
    defaultHomeOrder: List<String> = emptyList(),
    /**
     * Publishes a back action for Home to claim, or null to release it. Used for two transient states
     * that back should undo before it does anything else: an active search, and edit mode. Only one is
     * ever live at a time, so a single slot is enough.
     */
    onBackHandlerChange: ((() -> Unit)?) -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(categories, searchCategories, defaultHomeOrder)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searching = query.isNotBlank()

    // Lives here rather than in ProgressList so it survives a search (which swaps ProgressList out) and
    // so the back handler below can cancel it. Cancelling just leaves edit mode; the uncommitted order
    // and hidden set are dropped, since only Done saves them.
    var editing by rememberSaveable { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Back undoes a search first, then edit mode, before it would fall through to leaving the app.
    // Claimed only while one of those is active and released otherwise (and when Home leaves the
    // composition), so back is normal the rest of the time.
    DisposableEffect(searching, editing) {
        onBackHandlerChange(
            when {
                searching -> {
                    {
                        viewModel.setQuery("")
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
                }
                editing -> {
                    { editing = false }
                }
                else -> null
            }
        )
        onDispose { onBackHandlerChange(null) }
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
            ProgressList(
                state = state,
                defaultHomeOrder = defaultHomeOrder,
                editing = editing,
                onEditingChange = { editing = it },
                onCategoryClick = onCategoryClick,
                onCommit = { order, hidden ->
                    viewModel.setHomeOrder(order)
                    viewModel.setHomeHidden(hidden)
                }
            )
        }
    }
}

/**
 * The "Your progress" list, in normal and edit modes.
 *
 * Editing is a local, self-contained mode: it works on copies of the order and the hidden set and
 * only calls [onCommit] when the player taps Done, so the cards on Home and the summary counts don't
 * shift under them mid-edit. While editing, every card is shown - hidden ones dimmed - so they can be
 * reordered and switched back on in place, without a separate "hidden" section.
 */
@Composable
private fun ProgressList(
    state: HomeUiState,
    defaultHomeOrder: List<String>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onCategoryClick: (String) -> Unit,
    onCommit: (order: List<String>, hidden: Set<String>) -> Unit
) {
    val listState = rememberLazyListState()

    // The edit in progress: the order being dragged and the set being toggled. Held in
    // rememberSaveable so a rotation (or process death) mid-edit keeps the uncommitted arrangement
    // instead of snapping back to what was last saved. Seeded when edit is entered (see the Edit
    // button), committed and left behind on Done.
    val workingOrder = rememberSaveable(saver = stringListSaver) { mutableStateListOf<String>() }
    val workingHidden = rememberSaveable(saver = stringListSaver) { mutableStateListOf<String>() }

    // A safety net only: if we somehow return to an edit with nothing staged - a restore that lost
    // the working lists - fill them from the saved state. Gated on loaded so it seeds from the real
    // order rather than the pre-load seed, and it never runs when the lists already hold an edit, so
    // it can't clobber the arrangement rememberSaveable just brought back.
    LaunchedEffect(editing, state.loaded) {
        if (editing && state.loaded && workingOrder.isEmpty()) {
            workingOrder.addAll(state.categories.map { it.route })
            workingHidden.addAll(state.hidden)
        }
    }

    val reorderState = rememberHomeReorderState(
        listState = listState,
        headerCount = 1,
        onMove = { from, to ->
            if (from in workingOrder.indices && to in workingOrder.indices) {
                workingOrder.add(to, workingOrder.removeAt(from))
            }
        }
    )

    // Looks a card's live progress up by route, so a reorder driven by the route list still shows
    // real counts rather than a frozen snapshot taken when editing began.
    val progressByRoute = state.categories.associateBy { it.route }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your progress",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    // Held back until the saved state is in, so the count doesn't jump from the
                    // seed's "all lists" to the real "visible lists" alongside the cards appearing.
                    if (state.loaded) {
                        Text(
                            text = "${state.totalFound} of ${state.totalItems} collected across " +
                                "${state.visibleCategories.size} lists",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                }
                // No Edit until there's a real, saved order to edit - editing the pre-load seed
                // could stage and then commit the wrong arrangement.
                if (state.loaded) {
                    // Puts the edit back to the drawer order with everything shown. It only restages
                    // the working copies - like every other edit, it lands when Done is tapped.
                    if (editing) {
                        TextButton(
                            onClick = {
                                val present = state.categories.mapTo(HashSet()) { it.route }
                                workingOrder.clear()
                                workingOrder.addAll(defaultHomeOrder.filter { it in present })
                                workingHidden.clear()
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                    TextButton(
                        onClick = {
                            if (editing) {
                                onCommit(workingOrder.toList(), workingHidden.toSet())
                                onEditingChange(false)
                            } else {
                                // Seed here, on the actual tap, rather than in a launched effect:
                                // that way a rotation while editing never re-triggers seeding and
                                // wipes an in-progress reorder.
                                workingOrder.clear()
                                workingOrder.addAll(state.categories.map { it.route })
                                workingHidden.clear()
                                workingHidden.addAll(state.hidden)
                                onEditingChange(true)
                            }
                        }
                    ) {
                        Text(if (editing) "Done" else "Edit")
                    }
                }
            }
        }

        if (!state.loaded) {
            // Nothing else until the saved order and hidden set are read. The wait is a frame or
            // two of just the header, rather than the default order flashing in and reshuffling.
            return@LazyColumn
        }

        if (editing) {
            itemsIndexed(workingOrder, key = { _, route -> route }) { index, route ->
                val progress = progressByRoute[route] ?: return@itemsIndexed
                val isDragged = reorderState.draggingIndex == index
                CategoryProgressCard(
                    progress = progress,
                    editing = true,
                    hidden = route in workingHidden,
                    onToggleHidden = {
                        if (route in workingHidden) workingHidden.remove(route) else workingHidden.add(route)
                    },
                    onClick = {},
                    dragHandleModifier = Modifier.pointerInput(route) {
                        detectDragGestures(
                            onDragStart = { reorderState.onDragStart(workingOrder.indexOf(route)) },
                            onDrag = { change, amount ->
                                change.consume()
                                reorderState.onDrag(amount.y)
                            },
                            onDragEnd = { reorderState.onDragEnd() },
                            onDragCancel = { reorderState.onDragEnd() }
                        )
                    },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationY = reorderState.draggingItemTranslationY
                                shadowElevation = 8.dp.toPx()
                            }
                        }
                        // The dragged card is positioned by hand; letting the placement animation
                        // also act on it would fight that and make it stutter.
                        .then(if (isDragged) Modifier else Modifier.animateItem())
                )
            }
        } else {
            if (state.visibleCategories.isEmpty()) {
                item(key = "all_hidden") {
                    Text(
                        text = "Every list is hidden. Tap Edit to show some again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.visibleCategories, key = { it.route }) { progress ->
                CategoryProgressCard(
                    progress = progress,
                    editing = false,
                    hidden = false,
                    onToggleHidden = {},
                    onClick = { onCategoryClick(progress.route) },
                    dragHandleModifier = Modifier,
                    modifier = Modifier.animateItem()
                )
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
    editing: Boolean,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onClick: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            // Tapping a card opens its list; while editing there's nowhere to go, so it's inert and
            // the drag handle and visibility toggle do the work instead.
            .then(if (editing) Modifier else Modifier.clickable(onClick = onClick))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    // A hidden card stays in the edit list, just dimmed, so it can be reordered and
                    // switched back on where it sits.
                    .graphicsLayer { alpha = if (hidden) DIMMED_ALPHA else 1f }
            ) {
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
            if (editing) {
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (hidden) "Show on Home" else "Hide from Home",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** How far a hidden card is faded while editing: clearly off, still readable and draggable. */
private const val DIMMED_ALPHA = 0.4f

/**
 * Saves a working list of route ids across configuration changes and process death. Route ids are
 * plain strings, so the list restores as a live [SnapshotStateList] the edit UI can keep mutating.
 */
private val stringListSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)
