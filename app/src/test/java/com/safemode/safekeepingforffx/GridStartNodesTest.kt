package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridStartNodes
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the starting node each character opens on against the bundled Standard grid. The ids are
 * fixed, so the guard is that each still names a blank node sitting the expected number of steps
 * from the character's signature ability - if the asset is ever renumbered, these fail rather than
 * quietly opening the planner somewhere in the middle of the board.
 */
class GridStartNodesTest {

    private val grid by lazy {
        SphereGridParser.parse(File("src/main/assets/sphere_grid.json").readText())
    }

    private val adjacency by lazy {
        val map = HashMap<String, MutableSet<String>>()
        grid.edges.forEach { edge ->
            map.getOrPut(edge.fromId) { mutableSetOf() }.add(edge.toId)
            map.getOrPut(edge.toId) { mutableSetOf() }.add(edge.fromId)
        }
        map
    }

    private fun startOf(character: GridCharacter): String =
        requireNotNull(GridStartNodes.forCharacter(GridType.STANDARD, character)) {
            "${character.displayName} has no Standard grid start node"
        }

    /** Node ids within [steps] hops of [from], excluding [from] itself. */
    private fun within(from: String, steps: Int): Set<String> {
        var frontier = setOf(from)
        val seen = mutableSetOf(from)
        repeat(steps) {
            frontier = frontier.flatMap { adjacency[it].orEmpty() }.filterTo(mutableSetOf()) { seen.add(it) }
        }
        return seen - from
    }

    private fun contentOf(nodeId: String): NodeContent =
        requireNotNull(grid.nodes.firstOrNull { it.id == nodeId }) { "$nodeId is not on the grid" }
            .original

    @Test
    fun everyCharacterStartsOnABlankNodeOfTheStandardGrid() {
        GridCharacter.entries.forEach { character ->
            val id = startOf(character)
            assertTrue(
                "${character.displayName} starts on $id, which is ${contentOf(id)}",
                contentOf(id) is NodeContent.Empty
            )
        }
    }

    @Test
    fun startNodesAreDistinct() {
        val ids = GridCharacter.entries.map { startOf(it) }
        assertEquals(GridCharacter.entries.size, ids.toSet().size)
    }

    @Test
    fun startNodesSitBesideTheirSignatureAbility() {
        val anchors = mapOf(
            GridCharacter.YUNA to "Esuna",
            GridCharacter.AURON to "Power Break",
            GridCharacter.KIMAHRI to "Lancet",
            GridCharacter.WAKKA to "Dark Attack",
            GridCharacter.LULU to "Thunder",
            GridCharacter.RIKKU to "Steal"
        )
        anchors.forEach { (character, ability) ->
            val neighbours = adjacency[startOf(character)].orEmpty().map { contentOf(it) }
            assertTrue(
                "${character.displayName}'s start node should touch $ability",
                neighbours.any { it is NodeContent.Ability && it.name == ability }
            )
        }
    }

    /** Tidus is the odd one out: the second blank out from Cheer, up against the Lv. 1 lock. */
    @Test
    fun tidusStartsTwoBlanksOutFromCheerAgainstTheLockLevelOne() {
        val start = startOf(GridCharacter.TIDUS)
        val neighbours = adjacency[start].orEmpty()
        assertTrue(
            "Tidus's start node should be blank on both sides toward Cheer",
            neighbours.any { contentOf(it) is NodeContent.Empty }
        )
        assertTrue(
            "Tidus's start node should sit against the Lv. 1 lock",
            neighbours.any { (contentOf(it) as? NodeContent.Lock)?.level == 1 }
        )
        val cheer = within(start, 2).map { contentOf(it) }
        assertTrue(
            "Cheer should be two steps from Tidus's start node",
            cheer.any { it is NodeContent.Ability && it.name == "Cheer" }
        )
    }

    /** The Expert grid redistributes everything; with no mapping it keeps the fitted opening view. */
    @Test
    fun expertGridHasNoStartNodes() {
        GridCharacter.entries.forEach { character ->
            assertNull(GridStartNodes.forCharacter(GridType.EXPERT, character))
        }
    }

    // --- The opening view the planner derives from those nodes ---

    private fun stateFor(
        character: GridCharacter,
        gridType: GridType = GridType.STANDARD,
        lastActivated: String? = null
    ) = SphereGridUiState(
        gridType = gridType,
        grid = if (gridType == GridType.STANDARD) grid else GridData.EMPTY,
        character = character,
        lastActivatedNodeId = lastActivated
    )

    @Test
    fun aCharacterWithNoPathOpensOnTheirStartNode() {
        GridCharacter.entries.forEach { character ->
            assertEquals(startOf(character), stateFor(character).homeNodeId)
        }
    }

    @Test
    fun aCharacterWithAPathOpensWhereTheyLeftOff() {
        val state = stateFor(GridCharacter.LULU, lastActivated = "n247")
        assertEquals("n247", state.homeNodeId)
    }

    @Test
    fun aGridWithoutStartNodesOpensNowhereInParticular() {
        assertNull(stateFor(GridCharacter.LULU, gridType = GridType.EXPERT).homeNodeId)
    }

    @Test
    fun startNodesAreOnTheGrid() {
        GridCharacter.entries.forEach { character ->
            val id = startOf(character)
            assertNotNull("$id should be on the Standard grid", grid.nodes.firstOrNull { it.id == id })
            assertTrue("$id should be in the grid's id set", id in grid.nodeIds)
        }
    }
}
