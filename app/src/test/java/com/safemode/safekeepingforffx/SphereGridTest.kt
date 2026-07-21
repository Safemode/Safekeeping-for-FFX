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
