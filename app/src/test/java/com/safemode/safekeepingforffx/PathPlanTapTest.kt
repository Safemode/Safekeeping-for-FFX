package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.PathMode
import com.safemode.safekeepingforffx.ui.screens.spheregrid.PathPlan
import com.safemode.safekeepingforffx.ui.screens.spheregrid.PathRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a tap on the grid picks the planned path's destination rather than activating a node. */
class PathPlanTapTest {

    private fun plan(fromId: String?, toId: String?) = PathPlan(
        request = PathRequest(GridType.STANDARD, fromId = fromId, toId = toId, mode = PathMode.SHORTEST),
        fromId = fromId ?: "n1",
        path = null
    )

    @Test
    fun `a start picked with Path from here lets a tap pick the destination`() {
        assertTrue(plan(fromId = "n5", toId = null).tapPicksDestination)
    }

    @Test
    fun `a tap can move the destination of a path from a picked start`() {
        assertTrue(plan(fromId = "n5", toId = "n9").tapPicksDestination)
    }

    @Test
    fun `a path from the character with no destination yet lets a tap pick one`() {
        assertTrue(plan(fromId = null, toId = null).tapPicksDestination)
    }

    @Test
    fun `a path from the character to a destination leaves taps activating`() {
        assertFalse(plan(fromId = null, toId = "n9").tapPicksDestination)
    }
}
