package com.safemode.safekeepingforffx.ui.screens.home

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Drag-to-reorder for the Home cards, hand-rolled rather than pulled from a library.
 *
 * The whole trick is one formula: the dragged card is drawn at its normal slot plus a
 * [draggingItemTranslationY] of `where the finger started + how far it has moved - the slot's current
 * top`. When the card's centre crosses a neighbour's centre we reorder the underlying list, which
 * moves the card's slot; because the translation is measured against that slot every frame, the card
 * stays glued to the finger through the swap instead of jumping. Everything below keys off the card's
 * index in the data list, and the caller reorders that list in [onMove].
 *
 * [headerCount] is how many non-draggable rows sit above the cards in the same LazyColumn (the
 * "Your progress" header), so a data index maps to a list index by adding it.
 */
class HomeReorderState(
    val listState: LazyListState,
    private val headerCount: Int,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    /** The data index of the card being dragged, or null when nothing is in hand. */
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    private var draggedDistance by mutableFloatStateOf(0f)

    /** Absolute top of the dragged card's slot at the moment the drag began. */
    private var startSlotTop by mutableIntStateOf(0)

    val isDragging: Boolean get() = draggingIndex != null

    private val draggingLayoutInfo: LazyListItemInfo?
        get() = draggingIndex?.let { dataIndex ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == dataIndex + headerCount }
        }

    /** How far the dragged card is offset from its resting slot, for the card's `graphicsLayer`. */
    val draggingItemTranslationY: Float
        get() = draggingLayoutInfo?.let { startSlotTop + draggedDistance - it.offset } ?: 0f

    fun onDragStart(dataIndex: Int) {
        val info = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == dataIndex + headerCount }
        draggingIndex = dataIndex
        startSlotTop = info?.offset ?: 0
        draggedDistance = 0f
    }

    fun onDrag(deltaY: Float) {
        val fromIndex = draggingIndex ?: return
        draggedDistance += deltaY
        val dragged = draggingLayoutInfo ?: return

        val draggedCentre = dragged.offset + draggingItemTranslationY + dragged.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index >= headerCount &&
                item.index != dragged.index &&
                draggedCentre.toInt() in item.offset..(item.offset + item.size)
        } ?: return

        val toIndex = target.index - headerCount
        if (toIndex != fromIndex) {
            onMove(fromIndex, toIndex)
            draggingIndex = toIndex
        }
    }

    fun onDragEnd() {
        draggingIndex = null
        draggedDistance = 0f
        startSlotTop = 0
    }

    /**
     * How far to nudge the list this frame so a card dragged to the top or bottom edge keeps moving.
     * Zero when the card is comfortably inside the viewport. Driven by [rememberHomeReorderState]'s
     * frame loop rather than by drag events, so it keeps scrolling even when the finger holds still.
     */
    fun autoScrollDelta(): Float {
        val dragged = draggingLayoutInfo ?: return 0f
        val top = dragged.offset + draggingItemTranslationY
        val bottom = top + dragged.size
        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
        val margin = dragged.size.toFloat()
        return when {
            top < viewportStart + margin ->
                (top - (viewportStart + margin)).coerceAtLeast(-MAX_AUTO_SCROLL_PER_FRAME)
            bottom > viewportEnd - margin ->
                (bottom - (viewportEnd - margin)).coerceAtMost(MAX_AUTO_SCROLL_PER_FRAME)
            else -> 0f
        }
    }

    private companion object {
        /** Caps a single frame's auto-scroll so reaching an edge glides rather than lurches. */
        const val MAX_AUTO_SCROLL_PER_FRAME = 24f
    }
}

@Composable
fun rememberHomeReorderState(
    listState: LazyListState,
    headerCount: Int,
    onMove: (from: Int, to: Int) -> Unit
): HomeReorderState {
    val state = remember(listState) { HomeReorderState(listState, headerCount, onMove) }

    // While a card is in hand, scroll a touch every frame if it's parked against an edge. The loop
    // runs only for the duration of a drag: it starts when a drag begins and is cancelled the moment
    // the card is dropped, because the effect is keyed on whether a drag is in progress.
    LaunchedEffect(state.isDragging) {
        if (!state.isDragging) return@LaunchedEffect
        while (true) {
            val delta = state.autoScrollDelta()
            if (delta != 0f) state.listState.scrollBy(delta)
            withFrameNanos { }
        }
    }
    return state
}
