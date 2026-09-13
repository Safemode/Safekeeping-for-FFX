package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.ui.util.ItemSpan
import com.safemode.safekeepingforffx.ui.util.leadingItemsScrollRoom
import org.junit.Assert.assertEquals
import org.junit.Test

class LeadingItemsScrollRoomTest {

    /** A search field, a banner and a divider - the shape ChecklistScreen leads with. */
    private val leading = listOf(100, 300, 2)

    /** Lays [sizes] out top to bottom, scrolled by [scroll], keeping only what the viewport shows. */
    private fun room(
        rows: List<Int>,
        viewport: Int,
        scroll: Int,
        current: Int = 0
    ): Int {
        val sizes = leading + rows
        var top = -scroll
        val visible = sizes.mapIndexedNotNull { index, size ->
            val span = ItemSpan(index, top, size)
            top += size
            span.takeIf { it.offset + it.size > 0 && it.offset < viewport }
        }
        return leadingItemsScrollRoom(
            visible = visible,
            leadingItemCount = leading.size,
            lastContentIndex = sizes.lastIndex,
            viewportHeight = viewport,
            current = current
        )
    }

    @Test
    fun barelyOverflowingListGetsExactlyTheShortfall() {
        // 402 of leading items plus 750 of rows overflows 1000 by only 152, so 250 more is needed for
        // the leading items to clear: 402 + 750 + 250 = 402 + 1000.
        assertEquals(250, room(rows = List(5) { 150 }, viewport = 1000, scroll = 100))
        assertEquals(250, room(rows = List(5) { 150 }, viewport = 1000, scroll = 152))
    }

    @Test
    fun listThatFitsGetsNoRoom() {
        assertEquals(0, room(rows = List(5) { 150 }, viewport = 1200, scroll = 0, current = 250))
    }

    @Test
    fun longListGetsNoRoomOnceItsEndIsReached() {
        val rows = List(12) { 150 }
        val maxScroll = leading.sum() + rows.sum() - 1000
        assertEquals(0, room(rows = rows, viewport = 1000, scroll = maxScroll, current = 250))
    }

    @Test
    fun keepsTheCurrentRoomWhileTheLastRowIsOffScreen() {
        assertEquals(77, room(rows = List(5) { 150 }, viewport = 1000, scroll = 0, current = 77))
    }

    @Test
    fun noRowsMeansNoRoom() {
        assertEquals(0, room(rows = emptyList(), viewport = 1000, scroll = 0, current = 250))
    }
}
