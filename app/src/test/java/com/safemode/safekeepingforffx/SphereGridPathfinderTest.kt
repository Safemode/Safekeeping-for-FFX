package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridBounds
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.GridStartNodes
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.PathMode
import com.safemode.safekeepingforffx.data.reference.SphereGridEdge
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import com.safemode.safekeepingforffx.data.reference.SphereGridPathfinder
import com.safemode.safekeepingforffx.ui.screens.spheregrid.PathRequest
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SphereGridPathfinderTest {

    // --- A small hand-built grid for each rule ---

    private val stat = NodeContent.Attribute(NodeType.STRENGTH, 1)

    /** Builds a grid from "a-b" link strings; ids starting with "L<level>" are locks of that level. */
    private fun grid(vararg links: String): GridData {
        val pairs = links.map { it.split("-").let { (a, b) -> a to b } }
        val ids = pairs.flatMap { listOf(it.first, it.second) }.distinct()
        val nodes = ids.map { id ->
            val level = id.takeIf { it.startsWith("L") }?.substring(1, 2)?.toInt()
            SphereGridNode(id, 0f, 0f, if (level != null) NodeContent.Lock(level) else stat)
        }
        return GridData(nodes, pairs.map { SphereGridEdge(it.first, it.second) }, GridBounds(0f, 0f, 1f, 1f))
    }

    private fun find(
        grid: GridData,
        from: String,
        to: String,
        mode: PathMode = PathMode.SHORTEST,
        overrides: Map<String, NodeContent> = emptyMap(),
        activated: Set<String> = emptySet()
    ) = SphereGridPathfinder.find(grid, from, to, mode, overrides, activated)

    /** A lock on the short way round, and a three-node detour that avoids it. */
    private val lockOrDetour = grid("a-L1x", "L1x-b", "a-c1", "c1-c2", "c2-c3", "c3-b")

    @Test
    fun shortestOpensALockWhenThatIsShorter() {
        val path = assertFound(find(lockOrDetour, "a", "b"))
        assertEquals(listOf("a", "L1x", "b"), path.nodeIds)
        assertEquals(2, path.sphereLevels)
        assertEquals(mapOf(1 to 1), path.keysByLevel)
    }

    @Test
    fun noLocksTakesTheDetour() {
        val path = assertFound(find(lockOrDetour, "a", "b", PathMode.NO_LOCKS))
        assertEquals(listOf("a", "c1", "c2", "c3", "b"), path.nodeIds)
        assertTrue(path.locks.isEmpty())
    }

    @Test
    fun noLocksFindsNothingWhenOnlyALockConnects() {
        val g = grid("a-L2x", "L2x-b")
        assertNull(find(g, "a", "b", PathMode.NO_LOCKS))
        assertNotNull(find(g, "a", "b", PathMode.SHORTEST))
    }

    @Test
    fun anOpenedLockIsAnOrdinaryNode() {
        val opened = mapOf("L1x" to NodeContent.Empty)
        val path = assertFound(find(lockOrDetour, "a", "b", PathMode.NO_LOCKS, overrides = opened))
        assertEquals(listOf("a", "L1x", "b"), path.nodeIds)
        assertTrue("An opened lock needs no key", path.locks.isEmpty())
    }

    @Test
    fun anOpenedLockThatWasThenEditedIsStillOpen() {
        val edited = mapOf("L1x" to NodeContent.Attribute(NodeType.MAGIC, 4))
        val path = assertFound(find(lockOrDetour, "a", "b", PathMode.NO_LOCKS, overrides = edited))
        assertEquals(listOf("a", "L1x", "b"), path.nodeIds)
    }

    @Test
    fun movingBackAlongTheCharactersOwnPathIsCheaper() {
        // Two new nodes one way, three already-activated nodes the other: 4+4+4 against 1+1+1+4.
        val g = grid("a-x", "x-y", "y-b", "a-p", "p-q", "q-r", "r-b")
        val path = assertFound(find(g, "a", "b", activated = setOf("p", "q", "r")))
        assertEquals(listOf("a", "p", "q", "r", "b"), path.nodeIds)
        assertEquals(7, path.moveQuarters)
        assertEquals(2, path.sphereLevels)
    }

    @Test
    fun fewestKeysTakesALowerLockTheLongWayRound() {
        val g = grid("a-L4x", "L4x-b", "a-c", "c-L1y", "L1y-d", "d-b")
        assertEquals(listOf("a", "L4x", "b"), assertFound(find(g, "a", "b")).nodeIds)
        val cheap = assertFound(find(g, "a", "b", PathMode.FEWEST_KEYS))
        assertEquals(listOf("a", "c", "L1y", "d", "b"), cheap.nodeIds)
        assertEquals(mapOf(1 to 1), cheap.keysByLevel)
    }

    @Test
    fun shortestBreaksATieWithTheCheaperKey() {
        val g = grid("a-L3x", "L3x-b", "a-L1y", "L1y-b")
        assertEquals(listOf("a", "L1y", "b"), assertFound(find(g, "a", "b")).nodeIds)
    }

    @Test
    fun aLockedDestinationNeedsItsKey() {
        val g = grid("a-c", "c-L2x")
        assertNull(find(g, "a", "L2x", PathMode.NO_LOCKS))
        val path = assertFound(find(g, "a", "L2x"))
        assertEquals(listOf("a", "c", "L2x"), path.nodeIds)
        assertEquals(mapOf(2 to 1), path.keysByLevel)
    }

    @Test
    fun aPathToItselfIsEmpty() {
        val path = assertFound(find(lockOrDetour, "a", "a"))
        assertEquals(listOf("a"), path.nodeIds)
        assertEquals(0, path.steps)
        assertEquals(0, path.sphereLevels)
    }

    @Test
    fun anIdOffTheGridFindsNothing() {
        assertNull(find(lockOrDetour, "a", "x999"))
        assertNull(find(lockOrDetour, "x999", "a"))
    }

    // --- The real grids ---

    private val standard by lazy {
        SphereGridParser.parse(File("src/main/assets/sphere_grid.json").readText())
    }

    private val expert by lazy {
        SphereGridParser.parse(File("src/main/assets/expert_sphere_grid.json").readText(), "x")
    }

    private fun start(type: GridType, character: GridCharacter) =
        requireNotNull(GridStartNodes.forCharacter(type, character))

    @Test
    fun standardStartsAreWalledOffByLocksUntilTheyAreOpened() {
        val tidus = start(GridType.STANDARD, GridCharacter.TIDUS)
        val yuna = start(GridType.STANDARD, GridCharacter.YUNA)
        assertNull(
            "Every character starts in their own locked-off region of the Standard grid",
            find(standard, tidus, yuna, PathMode.NO_LOCKS)
        )

        val through = assertFound(find(standard, tidus, yuna))
        assertValid(standard, through)
        assertTrue(through.locks.isNotEmpty())

        val opened = through.locks.associate { it.nodeId to NodeContent.Empty }
        val walled = assertFound(find(standard, tidus, yuna, PathMode.NO_LOCKS, overrides = opened))
        assertEquals("Opening those locks clears the way", through.nodeIds, walled.nodeIds)
        assertTrue(walled.locks.isEmpty())
    }

    @Test
    fun everyStartReachesEveryOtherOnBothGrids() {
        listOf(GridType.STANDARD to standard, GridType.EXPERT to expert).forEach { (type, g) ->
            GridCharacter.entries.forEach { a ->
                GridCharacter.entries.forEach { b ->
                    PathMode.entries.filter { it != PathMode.NO_LOCKS }.forEach { mode ->
                        val path = find(g, start(type, a), start(type, b), mode)
                        assertNotNull("${type.label}: ${a.displayName} to ${b.displayName} ($mode)", path)
                        assertValid(g, path!!)
                    }
                }
            }
        }
    }

    @Test
    fun theSameRequestAlwaysGivesTheSamePath() {
        val from = start(GridType.EXPERT, GridCharacter.LULU)
        val to = start(GridType.EXPERT, GridCharacter.AURON)
        val first = find(expert, from, to)
        repeat(5) { assertEquals(first, find(expert, from, to)) }
    }

    // --- The planner state around it ---

    @Test
    fun aPlanWithNoStartBeginsWhereTheCharacterIs() {
        val home = start(GridType.STANDARD, GridCharacter.WAKKA)
        val state = SphereGridUiState(
            isLoading = false,
            gridType = GridType.STANDARD,
            grid = standard,
            character = GridCharacter.WAKKA
        )
        val target = standard.neighbours.getValue(home).first()
        val plan = requireNotNull(
            state.planPath(PathRequest(GridType.STANDARD, fromId = null, toId = target, mode = PathMode.SHORTEST))
        )
        assertEquals(home, plan.fromId)
        assertEquals(listOf(home, target), plan.path?.nodeIds)
        assertEquals("Both nodes are new to Wakka", listOf(home, target), plan.nodesToActivate)

        val walked = state.copy(activated = setOf(home), lastActivatedNodeId = home)
        val again = walked.planPath(PathRequest(GridType.STANDARD, null, target, PathMode.SHORTEST))
        assertEquals("A node already taken is left out", listOf(target), again?.nodesToActivate)
    }

    @Test
    fun aPlanForTheOtherGridIsNotShown() {
        val state = SphereGridUiState(isLoading = false, gridType = GridType.STANDARD, grid = standard)
        assertNull(state.planPath(PathRequest(GridType.EXPERT, null, "x1", PathMode.SHORTEST)))
    }

    // --- Helpers ---

    private fun assertFound(path: GridPath?): GridPath {
        assertNotNull("Expected a path", path)
        return path!!
    }

    /** Each step is a real link, and the cost and locks agree with the nodes walked. */
    private fun assertValid(g: GridData, path: GridPath) {
        path.nodeIds.zipWithNext().forEach { (a, b) ->
            assertTrue("$a and $b are linked", b in g.neighbours[a].orEmpty())
        }
        assertEquals(path.steps * SphereGridPathfinder.NEW_NODE_COST, path.moveQuarters)
        val lockIds = path.nodeIds.filter { g.nodesById.getValue(it).original is NodeContent.Lock }
        assertEquals(lockIds, path.locks.map { it.nodeId })
    }
}
