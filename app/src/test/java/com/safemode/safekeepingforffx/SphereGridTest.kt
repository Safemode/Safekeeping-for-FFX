package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the bundled Standard Sphere Grid asset straight off disk and checks the properties that
 * keep saved edits valid: the real grid is fully present, ids are unique (they are database keys),
 * every edge joins real nodes, and content survives an encode/decode round trip.
 */
class SphereGridTest {

    private val grid by lazy {
        SphereGridParser.parse(File("src/main/assets/sphere_grid.json").readText())
    }

    @Test
    fun isTheFullStandardGrid() {
        assertEquals(860, grid.totalNodes)
        assertEquals(85, grid.nodes.count { it.original is NodeContent.Ability })
        assertEquals(77, grid.nodes.count { it.original is NodeContent.Lock })
        // The editor's catalog is the same 85 abilities.
        assertEquals(85, grid.abilities.size)
    }

    @Test
    fun abilityNodesCarryTheirName() {
        val fira = grid.nodes.map { it.original }.filterIsInstance<NodeContent.Ability>()
            .firstOrNull { it.name == "Fira" }
        assertTrue("Fira should be a Black Magic ability on the grid", fira?.family == NodeType.BLACK_MAGIC)
    }

    @Test
    fun nodeIdsAreUnique() {
        val ids = grid.nodes.map { it.id }
        assertEquals("Node ids must be unique - they are database keys", ids.size, ids.toSet().size)
    }

    @Test
    fun everyEdgeJoinsRealNodes() {
        val ids = grid.nodes.map { it.id }.toSet()
        grid.edges.forEach { edge ->
            assertTrue("Edge from ${edge.fromId} references a missing node", edge.fromId in ids)
            assertTrue("Edge to ${edge.toId} references a missing node", edge.toId in ids)
        }
    }

    @Test
    fun lockNodesCarryTheirLevel() {
        grid.nodes.map { it.original }.filterIsInstance<NodeContent.Lock>().forEach {
            assertTrue("Lock level should be 1-4, was ${it.level}", it.level in 1..4)
        }
    }

    /**
     * The single-linked locks: ones joining only one node instead of gating a way through. All three
     * are correct - the real grid does have locks that lead nowhere, and each of these was checked
     * against the game. Each hangs off an ability hub and stops there: n244 off Copycat, n426 off
     * Zombie Attack, n864 off Doublecast.
     *
     * So this pins the set rather than forbidding it. A *new* single-linked lock appearing means a
     * connection went missing from the data, which is the shape the one real bug had: the Lv.2 lock
     * below Delay Buster (n355) hung off a plain stat node with an orphaned HP +200 beside it.
     */
    @Test
    fun singleLinkedLocksAreTheKnownSet() {
        val degree = grid.nodes.associate { node ->
            node.id to grid.edges.count { it.fromId == node.id || it.toId == node.id }
        }
        val singleLinked = grid.nodes
            .filter { it.original is NodeContent.Lock && degree.getValue(it.id) < 2 }
            .map { it.id }
            .toSet()

        assertEquals(
            "Locks that lead nowhere in the real grid - a new one here means a link went missing",
            setOf("n244", "n426", "n864"),
            singleLinked
        )
    }

    /** The Lv.2 lock below Delay Buster gates the HP +200 beside it - a link missing until 0.8. */
    @Test
    fun theLockBelowDelayBusterGatesItsNeighbour() {
        val linked = grid.edges.any { edge ->
            setOf(edge.fromId, edge.toId) == setOf("n355", "n357")
        }
        assertTrue("n355 (Lv.2 lock) should be linked to n357 (HP +200)", linked)
    }

    @Test
    fun contentEncodingRoundTrips() {
        val samples = listOf(
            NodeContent.Empty,
            NodeContent.Attribute(NodeType.STRENGTH, 2),
            NodeContent.Attribute(NodeType.HP, 200),
            NodeContent.Ability("Fira", NodeType.BLACK_MAGIC),
            NodeContent.Ability("Delay Attack", NodeType.SKILL),
            NodeContent.Lock(3)
        )
        samples.forEach { content ->
            assertEquals(content, NodeContent.decode(content.encode()))
        }
    }
}
