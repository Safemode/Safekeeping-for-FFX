package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridStartNodes
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pins the starting node each character opens on against the bundled grids. The ids are fixed, so the
 * guard is that each still names the node the description picked out - a blank the right number of
 * steps from the character's signature ability, with the neighbours that told it apart from the
 * blanks beside it. If either asset is ever renumbered these fail, rather than the planner quietly
 * opening somewhere in the middle of the board.
 */
class GridStartNodesTest {

    private val standard by lazy {
        SphereGridParser.parse(File("src/main/assets/sphere_grid.json").readText())
    }

    private val expert by lazy {
        SphereGridParser.parse(File("src/main/assets/expert_sphere_grid.json").readText(), "x")
    }

    private fun gridFor(type: GridType) = if (type == GridType.STANDARD) standard else expert

    private fun startOf(type: GridType, character: GridCharacter): String =
        requireNotNull(GridStartNodes.forCharacter(type, character)) {
            "${character.displayName} has no ${type.label} grid start node"
        }

    private fun adjacencyOf(type: GridType): Map<String, Set<String>> {
        val map = HashMap<String, MutableSet<String>>()
        gridFor(type).edges.forEach { edge ->
            map.getOrPut(edge.fromId) { mutableSetOf() }.add(edge.toId)
            map.getOrPut(edge.toId) { mutableSetOf() }.add(edge.fromId)
        }
        return map
    }

    private fun neighboursOf(type: GridType, nodeId: String): List<NodeContent> =
        adjacencyOf(type)[nodeId].orEmpty().map { contentOf(type, it) }

    private fun contentOf(type: GridType, nodeId: String): NodeContent =
        requireNotNull(gridFor(type).nodes.firstOrNull { it.id == nodeId }) {
            "$nodeId is not on the ${type.label} grid"
        }.original

    private fun assertBesideAbility(type: GridType, character: GridCharacter, ability: String) {
        assertTrue(
            "${character.displayName}'s ${type.label} start node should touch $ability",
            neighboursOf(type, startOf(type, character))
                .any { it is NodeContent.Ability && it.name == ability }
        )
    }

    private fun assertOnTheGrid(type: GridType) {
        GridCharacter.entries.forEach { character ->
            val id = startOf(type, character)
            assertTrue("$id should be on the ${type.label} grid", id in gridFor(type).nodeIds)
        }
    }

    private fun assertStartsAreDistinct(type: GridType) {
        val ids = GridCharacter.entries.map { startOf(type, it) }
        assertEquals("${type.label} start nodes should not be shared", ids.size, ids.toSet().size)
    }

    // --- Standard grid ---

    @Test
    fun everyStandardStartNodeIsOnTheGridAndBlank() {
        assertOnTheGrid(GridType.STANDARD)
        GridCharacter.entries.forEach { character ->
            val id = startOf(GridType.STANDARD, character)
            assertTrue(
                "${character.displayName} starts on $id, which is ${contentOf(GridType.STANDARD, id)}",
                contentOf(GridType.STANDARD, id) is NodeContent.Empty
            )
        }
    }

    @Test
    fun standardStartNodesAreDistinct() {
        assertStartsAreDistinct(GridType.STANDARD)
    }

    @Test
    fun standardStartNodesSitBesideTheirSignatureAbility() {
        mapOf(
            GridCharacter.YUNA to "Esuna",
            GridCharacter.AURON to "Power Break",
            GridCharacter.KIMAHRI to "Lancet",
            GridCharacter.WAKKA to "Dark Attack",
            GridCharacter.LULU to "Thunder",
            GridCharacter.RIKKU to "Steal"
        ).forEach { (character, ability) ->
            assertBesideAbility(GridType.STANDARD, character, ability)
        }
    }

    /** Tidus is the odd one out: the second blank out from Cheer, up against the Lv. 1 lock. */
    @Test
    fun tidusStartsTwoBlanksOutFromCheerAgainstTheLockLevelOne() {
        val start = startOf(GridType.STANDARD, GridCharacter.TIDUS)
        val neighbours = neighboursOf(GridType.STANDARD, start)
        assertTrue(
            "Tidus's start node should carry on toward Cheer through a blank",
            neighbours.any { it is NodeContent.Empty }
        )
        assertTrue(
            "Tidus's start node should sit against the Lv. 1 lock",
            neighbours.any { (it as? NodeContent.Lock)?.level == 1 }
        )
        val twoSteps = adjacencyOf(GridType.STANDARD)[start].orEmpty()
            .flatMap { neighboursOf(GridType.STANDARD, it) }
        assertTrue(
            "Cheer should be two steps from Tidus's start node",
            twoSteps.any { it is NodeContent.Ability && it.name == "Cheer" }
        )
    }

    // --- Expert grid ---

    @Test
    fun everyExpertStartNodeIsOnTheGrid() {
        assertOnTheGrid(GridType.EXPERT)
    }

    @Test
    fun expertStartNodesAreDistinct() {
        assertStartsAreDistinct(GridType.EXPERT)
    }

    @Test
    fun expertStartNodesSitBesideTheirSignatureAbility() {
        mapOf(
            GridCharacter.TIDUS to "Cheer",
            GridCharacter.YUNA to "Extract Ability",
            GridCharacter.AURON to "Power Break",
            GridCharacter.LULU to "Blizzard",
            GridCharacter.RIKKU to "Use"
        ).forEach { (character, ability) ->
            assertBesideAbility(GridType.EXPERT, character, ability)
        }
    }

    /** Every Expert start is a blank except Kimahri's, who starts on Lancet itself. */
    @Test
    fun expertStartNodesAreBlankExceptKimahriOnLancet() {
        GridCharacter.entries.filter { it != GridCharacter.KIMAHRI }.forEach { character ->
            val id = startOf(GridType.EXPERT, character)
            assertTrue(
                "${character.displayName} starts on $id, which is ${contentOf(GridType.EXPERT, id)}",
                contentOf(GridType.EXPERT, id) is NodeContent.Empty
            )
        }
        val lancet = contentOf(GridType.EXPERT, startOf(GridType.EXPERT, GridCharacter.KIMAHRI))
        assertEquals(NodeContent.Ability("Lancet", NodeType.SPECIAL), lancet)
    }

    /** Wakka sits in the middle of the run of three blanks leading off Dark Attack. */
    @Test
    fun wakkaStartsInTheMiddleOfThreeBlanksOffDarkAttack() {
        val start = startOf(GridType.EXPERT, GridCharacter.WAKKA)
        val neighbours = adjacencyOf(GridType.EXPERT)[start].orEmpty()
        assertEquals("Wakka's start node should sit between two blanks", 2, neighbours.size)
        assertTrue(
            "both of Wakka's neighbours should be blank",
            neighbours.all { contentOf(GridType.EXPERT, it) is NodeContent.Empty }
        )
        // One of those two blanks is the one hanging off Dark Attack, which puts the run of three
        // against the ability the description named.
        assertTrue(
            "the run of three should hang off Dark Attack",
            neighbours.flatMap { neighboursOf(GridType.EXPERT, it) }
                .any { it is NodeContent.Ability && it.name == "Dark Attack" }
        )
    }

    /** The Blizzard blank that is Lulu's carries three further blanks; the other one carries none. */
    @Test
    fun lulusExpertStartCarriesThreeFurtherBlanks() {
        val neighbours = neighboursOf(GridType.EXPERT, startOf(GridType.EXPERT, GridCharacter.LULU))
        assertEquals(3, neighbours.count { it is NodeContent.Empty })
    }

    /** The Use blank that is Rikku's carries a blank and an HP node; the other one carries a blank. */
    @Test
    fun rikkusExpertStartCarriesABlankAndAnHpNode() {
        val neighbours = neighboursOf(GridType.EXPERT, startOf(GridType.EXPERT, GridCharacter.RIKKU))
        assertEquals(1, neighbours.count { it is NodeContent.Empty })
        assertTrue(
            "Rikku's start node should carry an HP node",
            neighbours.any { (it as? NodeContent.Attribute)?.attribute == NodeType.HP }
        )
    }

    // --- The opening view the planner derives from those nodes ---

    private fun stateFor(
        character: GridCharacter,
        gridType: GridType = GridType.STANDARD,
        grid: GridData = gridFor(gridType),
        lastActivated: String? = null
    ) = SphereGridUiState(
        gridType = gridType,
        grid = grid,
        character = character,
        lastActivatedNodeId = lastActivated
    )

    @Test
    fun aCharacterWithNoPathOpensOnTheirStartNode() {
        GridType.entries.forEach { type ->
            GridCharacter.entries.forEach { character ->
                assertEquals(
                    startOf(type, character),
                    stateFor(character, gridType = type).homeNodeId
                )
            }
        }
    }

    @Test
    fun aCharacterWithAPathOpensWhereTheyLeftOff() {
        assertEquals("n247", stateFor(GridCharacter.LULU, lastActivated = "n247").homeNodeId)
    }

    @Test
    fun aStartNodeThatIsNotOnTheLoadedGridIsIgnored() {
        val state = stateFor(GridCharacter.LULU, grid = GridData.EMPTY)
        assertNull(state.homeNodeId)
    }
}
