package com.safemode.safekeepingforffx.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/** The part of a laid-out list item this needs, kept apart from LazyListItemInfo so it can be tested. */
data class ItemSpan(val index: Int, val offset: Int, val size: Int)

/**
 * How tall a spacer after the last row has to be for a list's leading items - a search field, advice
 * banners - to scroll fully out of view.
 *
 * A list that overflows the screen by less than those leading items are tall runs out of scroll while
 * part of them is still showing, which leaves the bottom of a banner stuck at the top of the screen.
 * The room makes up exactly that shortfall. It is zero on a list long enough not to need it, and zero
 * on a list short enough to fit, where there is nothing to scroll for in the first place.
 *
 * The spacer must be the list's last item: everything before it is treated as content.
 */
@Composable
fun rememberLeadingItemsScrollRoom(listState: LazyListState, leadingItemCount: Int): Dp {
    var roomPx by remember(listState) { mutableIntStateOf(0) }

    LaunchedEffect(listState, leadingItemCount) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            roomPx = leadingItemsScrollRoom(
                visible = info.visibleItemsInfo.map { ItemSpan(it.index, it.offset, it.size) },
                leadingItemCount = leadingItemCount,
                // The last item is the spacer itself, which never counts towards its own height.
                lastContentIndex = info.totalItemsCount - 2,
                viewportHeight = info.viewportSize.height,
                current = roomPx
            )
        }
    }

    return with(LocalDensity.current) { roomPx.toDp() }
}

/**
 * The room in pixels, or [current] when what is on screen isn't enough to say. The rows' combined
 * height is only known once the first and last of them are laid out together, which on a list short
 * enough to need room happens well before the end of the scroll, so the room is in place in time.
 *
 * The spacer is never measured, so changing its height can't feed back into the next answer.
 */
internal fun leadingItemsScrollRoom(
    visible: List<ItemSpan>,
    leadingItemCount: Int,
    lastContentIndex: Int,
    viewportHeight: Int,
    current: Int
): Int {
    // Nothing follows the leading items, so there is nothing to scroll them away for.
    if (lastContentIndex < leadingItemCount) return 0

    val last = visible.firstOrNull { it.index == lastContentIndex } ?: return current
    val firstRow = visible
        .filter { it.index in leadingItemCount..lastContentIndex }
        .minByOrNull { it.index } ?: return current
    val rowsSpan = last.offset + last.size - firstRow.offset

    if (firstRow.index != leadingItemCount) {
        // Rows above have scrolled off, so the span is only a lower bound on the rows' height - but a
        // lower bound that already fills the screen is proof the list is long enough on its own.
        return if (rowsSpan >= viewportHeight) 0 else current
    }

    val first = visible.firstOrNull { it.index == 0 }
    if (first != null && last.offset + last.size - first.offset <= viewportHeight) return 0

    return (viewportHeight - rowsSpan).coerceAtLeast(0)
}
