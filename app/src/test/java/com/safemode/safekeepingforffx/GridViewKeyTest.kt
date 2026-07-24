package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.ui.screens.spheregrid.gridViewKey
import com.safemode.safekeepingforffx.ui.screens.spheregrid.routeViewKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The planner remembers one pan/zoom per slot. These pin what a slot is, because getting it wrong is
 * invisible until you switch characters: keyed by grid alone, every character shared one view, so
 * switching to someone else and back left the canvas parked wherever that other character had been
 * instead of returning to your own.
 */
class GridViewKeyTest {

    @Test
    fun eachCharacterOwnsTheirViewOfAGrid() {
        val keys = GridCharacter.entries.map { gridViewKey(GridType.STANDARD, it) }
        assertEquals("every character needs their own slot", keys.size, keys.toSet().size)
    }

    @Test
    fun aCharacterOwnsAViewPerGrid() {
        GridCharacter.entries.forEach { character ->
            assertFalse(
                "${character.displayName}'s two grids must not share a view",
                gridViewKey(GridType.STANDARD, character) == gridViewKey(GridType.EXPERT, character)
            )
        }
    }

    @Test
    fun aReplayNeverSharesASlotWithAnyCharacter() {
        val live = GridType.entries.flatMap { type ->
            GridCharacter.entries.map { gridViewKey(type, it) }
        }.toSet()
        GridType.entries.forEach { type ->
            assertFalse(
                "a route replay must not move where the player left a character",
                routeViewKey(type) in live
            )
        }
    }

    @Test
    fun eachGridsReplayIsRememberedSeparately() {
        assertFalse(routeViewKey(GridType.STANDARD) == routeViewKey(GridType.EXPERT))
    }
}
