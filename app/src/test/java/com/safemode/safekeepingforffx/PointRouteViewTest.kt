package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.LockOnPath
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.PathMode
import com.safemode.safekeepingforffx.data.repository.SphereGridRepository
import com.safemode.safekeepingforffx.ui.screens.spheregrid.PointRouteViewState
import com.safemode.safekeepingforffx.ui.screens.spheregrid.costSummary
import com.safemode.safekeepingforffx.ui.screens.spheregrid.pointStepCaption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stepping through a point-to-point route, node by node. */
class PointRouteViewTest {

    /** a -> b -> L(lock, Lv.2) -> d: three moves, one lock on the way. */
    private val path = GridPath(
        nodeIds = listOf("a", "b", "L", "d"),
        moveQuarters = 12,
        locks = listOf(LockOnPath("L", 2))
    )

    private fun view(step: Int = 0) = PointRouteViewState(
        routeId = null,
        name = null,
        gridType = GridType.STANDARD,
        character = GridCharacter.TIDUS,
        mode = PathMode.SHORTEST,
        path = path,
        stepIndex = step
    )

    @Test
    fun theFirstStepStandsOnTheStart() {
        val v = view()
        assertEquals(3, v.stepCount)
        assertEquals("a", v.currentNodeId)
        assertEquals(listOf("a"), v.walked)
        assertEquals(path.nodeIds, v.remaining)
        assertTrue("Nothing is numbered before the first move", v.orderLabels.isEmpty())
        assertEquals(setOf("L"), v.locksAhead)
    }

    @Test
    fun steppingWalksThePathAndNumbersIt() {
        val v = view(step = 2)
        assertEquals("L", v.currentNodeId)
        assertEquals(listOf("a", "b", "L"), v.walked)
        assertEquals(listOf("L", "d"), v.remaining)
        assertEquals(mapOf("b" to 1, "L" to 2), v.orderLabels)
        assertTrue("A lock reached is a lock opened", v.locksAhead.isEmpty())
    }

    @Test
    fun theLastStepReachesTheDestination() {
        val v = view(step = 3)
        assertEquals("d", v.currentNodeId)
        assertEquals(listOf("d"), v.remaining)
        assertEquals(3, v.orderLabels["d"])
    }

    @Test
    fun stepOfFindsWhereANodeFalls() {
        assertEquals(0, view().stepOf("a"))
        assertEquals(2, view().stepOf("L"))
        assertNull(view().stepOf("elsewhere"))
    }

    @Test
    fun anUnsavedPathIsNotSaved() {
        assertFalse(view().isSaved)
        assertTrue(view().copy(routeId = 4, name = "Mine").isSaved)
    }

    @Test
    fun captionsSayWhatEachNodeNeeds() {
        assertEquals(
            "Lv.2 Lock: open it with a Lv.2 Key Sphere",
            pointStepCaption(NodeContent.Lock(2), alreadyActivated = false)
        )
        assertEquals("Blank node: nothing to activate", pointStepCaption(NodeContent.Empty, false))
        assertEquals(
            "Strength +2: Power Sphere or Attribute Sphere",
            pointStepCaption(NodeContent.Attribute(NodeType.STRENGTH, 2), alreadyActivated = false)
        )
        assertEquals(
            "Strength +2: already activated",
            pointStepCaption(NodeContent.Attribute(NodeType.STRENGTH, 2), alreadyActivated = true)
        )
    }

    @Test
    fun costSummaryReadsInOneLine() {
        assertEquals("3 steps, 3 Sphere Levels · 1x Lv.2 Key Sphere", path.costSummary())
        assertEquals(
            "1 step, 1 Sphere Level · no locks",
            GridPath(listOf("a", "b"), 4, emptyList()).costSummary()
        )
    }

    @Test
    fun nodeIdsSurviveTheDatabaseEncoding() {
        val ids = listOf("n1", "n22", "n333")
        assertEquals(ids, SphereGridRepository.decodeNodeIds(SphereGridRepository.encodeNodeIds(ids)))
        assertTrue(SphereGridRepository.decodeNodeIds("").isEmpty())
    }
}
