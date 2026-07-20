package com.safemode.safekeepingforffx.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs

/** Below this many pixels of movement we ignore the scroll, so the header can't flicker. */
private const val DIRECTION_THRESHOLD = 8

/**
 * True while the list is at the top or being scrolled up - i.e. when it's worth spending screen
 * space on the header. Scrolling down collapses it to give the list more room.
 *
 * The end-of-list case is handled explicitly rather than left to the direction heuristic. Expanding
 * the header shrinks the list's viewport; when the list is already at its end there is nothing
 * below to take up the slack, so the content shifts back down and the first visible item's offset
 * *decreases*. That reads as an upward scroll, which re-expands the header, which shifts the
 * content again - a loop that settles on whichever emission happened to come last. A fast fling
 * that ends exactly at the bottom tended to settle expanded, leaving the banners on screen.
 */
@Composable
fun rememberHeaderExpanded(listState: LazyListState): Boolean {
    var expanded by remember(listState) { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.canScrollForward
            )
        }.collect { (index, offset, canScrollForward) ->
            val atTop = index == 0 && offset == 0
            val movedRows = index != lastIndex
            val scrollingUp = if (movedRows) index < lastIndex else offset < lastOffset
            // A row change is always a big enough move to trust the direction.
            val movedEnough = movedRows || abs(offset - lastOffset) > DIRECTION_THRESHOLD

            when {
                // Checked first, so a list short enough to need no scrolling keeps its header.
                atTop -> expanded = true
                !canScrollForward -> expanded = false
                movedEnough -> expanded = scrollingUp
            }

            lastIndex = index
            lastOffset = offset
        }
    }

    return expanded
}
