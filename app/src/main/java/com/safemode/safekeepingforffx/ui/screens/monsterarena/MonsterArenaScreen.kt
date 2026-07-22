package com.safemode.safekeepingforffx.ui.screens.monsterarena

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.data.reference.CreationProgress
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterColumns
import com.safemode.safekeepingforffx.data.reference.monsterType
import com.safemode.safekeepingforffx.ui.components.Banner
import com.safemode.safekeepingforffx.ui.components.SearchField
import com.safemode.safekeepingforffx.ui.components.SectionHeader
import com.safemode.safekeepingforffx.ui.util.rememberHeaderExpanded

/**
 * Material's minimum touch target, and so the height of an [IconButton]. Rows reserve it whether
 * or not they have a button, which is what keeps every row the same height and stops the layout
 * jumping when a button appears.
 */
private val ACTION_SLOT = 48.dp

/**
 * Capture tracker for the Monster Arena: every fiend, grouped by the area it is caught in, counted
 * from zero to ten. Counts are written straight through to the database on each tap, so there is
 * nothing to save and nothing to lose.
 */
@Composable
fun MonsterArenaScreen(
    modifier: Modifier = Modifier,
    onSearchDismissChange: ((() -> Unit)?) -> Unit = {},
    viewModel: MonsterArenaViewModel = viewModel(factory = MonsterArenaViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val headerExpanded = rememberHeaderExpanded(listState)

    // Store the id rather than the monster so the expansion survives rotation. One at a time: two
    // open rows would push everything else off screen.
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    // The creation whose auto-capture confirmation is open. Stored by id, not by object, so it
    // survives rotation and always reflects the latest computed targets.
    var autoCaptureId by rememberSaveable { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Same as the other categories: back clears an active search before it leaves the screen.
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

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.isEmpty) {
            Text(
                text = "No fiends are listed yet. Add them to monster_arena.csv.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${state.capturedCount} of ${state.totalCount} fiends fully captured",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            // The slot is always laid out, so the button appearing at the first capture doesn't
            // shove the summary text down. Hidden at zero: a destructive control shouldn't sit
            // there offering to undo nothing.
            Box(
                modifier = Modifier.size(ACTION_SLOT),
                contentAlignment = Alignment.Center
            ) {
                if (state.hasProgress) {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset capture counts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = headerExpanded || state.isSearching,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                placeholder = "Search fiends, areas, types, drops"
            )
        }

        AnimatedVisibility(
            // Hidden outright when help is switched off in Settings, not merely collapsed.
            visible = headerExpanded && state.showHelp,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Banner(
                Icons.Outlined.Info,
                "Tap a fiend to see its details. Long-press a creation to capture the fiends " +
                    "it needs."
            )
        }
        HorizontalDivider()

        if (state.hasNoMatches) {
            Text(
                text = "Nothing matches \"${state.query.trim()}\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(state = listState) {
            state.byArea.forEach { (area, captures) ->
                item(key = "area_$area") { SectionHeader(area) }

                items(captures, key = { it.monster.id }) { capture ->
                    val isExpanded = capture.monster.id == expandedId

                    MonsterRow(
                        capture = capture,
                        progress = state.creationProgress[capture.monster.id],
                        expanded = isExpanded,
                        onClick = {
                            expandedId = if (isExpanded) null else capture.monster.id
                        },
                        // Only capture-based creations carry a target set, so only they take a
                        // long-press; everything else leaves it null and behaves as before.
                        onLongClick = if (state.autoCaptures.containsKey(capture.monster.id)) {
                            { autoCaptureId = capture.monster.id }
                        } else {
                            null
                        },
                        onDecrement = { viewModel.adjust(capture, -1) },
                        onIncrement = { viewModel.adjust(capture, 1) }
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MonsterDetails(capture.monster)
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    val autoCapture = autoCaptureId?.let { state.autoCaptures[it] }
    if (autoCapture != null) {
        AutoCaptureDialog(
            auto = autoCapture,
            onConfirm = {
                viewModel.autoCapture(autoCapture)
                autoCaptureId = null
            },
            onDismiss = { autoCaptureId = null }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Monster Arena?") },
            text = {
                Text(
                    "Every capture count in this category goes back to zero. Your other lists " +
                        "are untouched. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetCaptures()
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
 * Columns already surfaced on the collapsed row itself - the fiend's type, and a creation's
 * condition and reward - so the expanded view drops them rather than repeating them.
 */
private val INLINE_DETAIL_KEYS = setOf(
    MonsterColumns.MONSTER_TYPE,
    MonsterColumns.UNLOCK_CONDITION,
    MonsterColumns.UNLOCK_REWARD
)

/** The extra CSV columns, revealed under the fiend rather than over the page. */
@Composable
private fun MonsterDetails(monster: Monster, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        val rows = monster.details.filterKeys { it !in INLINE_DETAIL_KEYS }
        if (rows.isEmpty()) {
            Text(
                text = "No extra information recorded for this fiend yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        // Blank columns were dropped at parse time, so every row here has a value.
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(104.dp)
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonsterRow(
    capture: MonsterCapture,
    progress: CreationProgress?,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The stepper buttons consume their own taps, so a row-level click can safely open the
            // details without stealing them. A long-press, where offered, auto-captures a creation's
            // fiends.
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "Capture the fiends this creation needs"
            )
            // Matches the height a stepper gives a row, so creations - which have no stepper - sit
            // at the same rhythm as everything else rather than reading as a denser list.
            .heightIn(min = ACTION_SLOT + 8.dp)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
        )

        // Name plus its subtext - the fiend's type, or a creation's unlock note and reward - kept
        // in one column so every second line starts at the same place all the way down the list.
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = capture.monster.name,
                    style = MaterialTheme.typography.titleSmall
                )
                // A quiet mark so the long-press is discoverable per row, not just from the hint at
                // the top of the list. Only shown where the long-press actually does something.
                if (onLongClick != null) {
                    Icon(
                        imageVector = Icons.Outlined.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                    )
                }
            }
            if (capture.monster.isCapturable) {
                capture.monster.monsterType?.let { type ->
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (progress != null) {
                CreationSubtext(progress)
            }
        }

        // Arena creations are unlocked rather than captured, so they carry no count at all - a lock
        // that opens once their condition is met stands in for the stepper.
        if (capture.monster.isCapturable) {
            if (capture.isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Fully captured",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(20.dp)
                )
            }

            IconButton(onClick = onDecrement, enabled = capture.count > 0) {
                Icon(Icons.Filled.Remove, contentDescription = "One fewer ${capture.monster.name}")
            }

            Text(
                text = "${capture.count} / $MAX_CAPTURES",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (capture.isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                // Fixed width so the buttons don't shuffle sideways as the number changes.
                modifier = Modifier.width(64.dp)
            )

            IconButton(onClick = onIncrement, enabled = capture.count < MAX_CAPTURES) {
                Icon(Icons.Filled.Add, contentDescription = "One more ${capture.monster.name}")
            }
        } else {
            val unlocked = progress?.unlocked == true
            Icon(
                imageVector = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = if (unlocked) "Unlocked" else "Locked",
                tint = if (unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp)
            )
        }
    }
}

/**
 * Above this many fiends the per-fiend list is dropped from the confirmation - the "every fiend"
 * originals would otherwise print the whole bestiary. The summary line carries the meaning anyway.
 */
private const val MAX_LISTED_FIENDS = 12

/**
 * Confirms a creation's long-press auto-capture before it writes anything: it says how many fiends
 * it will set and to what, lists them when the list is short enough to be useful, and is clear that
 * higher counts are left untouched.
 */
@Composable
private fun AutoCaptureDialog(
    auto: CreationAutoCapture,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-capture ${auto.creationName}?") },
        text = {
            Column {
                val amount = auto.uniformAmount
                val amountPhrase = if (amount != null) {
                    "to $amount / $MAX_CAPTURES each"
                } else {
                    "to their required counts"
                }
                Text(
                    "Sets the ${auto.fiends.size} fiends ${auto.creationName} needs $amountPhrase, " +
                        "which unlocks it. Any already higher are left as they are."
                )
                if (auto.fiends.size <= MAX_LISTED_FIENDS) {
                    Spacer(Modifier.height(8.dp))
                    auto.fiends.forEach { fiend ->
                        Text(
                            text = "${fiend.name}  -  ${fiend.amount} / $MAX_CAPTURES",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Capture") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * The two lines under a creation's name: what unlocks it (with a captured-so-far tally while it is
 * still locked) and what it pays out. Turns green the moment the creation opens.
 */
@Composable
private fun CreationSubtext(progress: CreationProgress, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (progress.requirement.isNotEmpty()) {
            val tally = if (!progress.unlocked && progress.required > 0) {
                "  -  ${progress.current} / ${progress.required}"
            } else {
                ""
            }
            Text(
                text = progress.requirement + tally,
                style = MaterialTheme.typography.bodySmall,
                color = if (progress.unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (progress.reward.isNotEmpty()) {
            Text(
                text = "Unlock Reward: ${progress.reward}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
