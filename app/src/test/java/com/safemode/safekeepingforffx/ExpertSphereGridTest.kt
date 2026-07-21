package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Expert Sphere Grid, transcribed from the game-extracted data in Grayfox96/FFX-Sphere-Grid-viewer.
 * Checks the structural invariants that keep saved edits valid: the full grid is present, ids carry the
 * Expert namespace so they never collide with the Standard grid's, the whole grid is one connected web,
 * every edge joins real nodes, and it shares the same 85-ability editor catalog. It is deliberately
 * smaller than the Standard grid.
 */
class ExpertSphereGridTest {

    private val standard by lazy {
        SphereGridParser.parse(File("src/main/assets/sphere_grid.json").readText(), GridType.STANDARD.idPrefix)
    }

    private val expert by lazy {
        SphereGridParser.parse(
            File("src/main/assets/expert_sphere_grid.json").readText(),
            GridType.EXPERT.idPrefix
        )
    }

    @Test
    fun isTheFullExpertGrid() {
        assertEquals(805, expert.totalNodes)
        assertEquals(85, expert.nodes.count { it.original is NodeContent.Ability })
        // 52 locks: Lv1x10, Lv2x12, Lv3x18, Lv4x12.
        assertEquals(52, expert.nodes.count { it.original is NodeContent.Lock })
        assertEquals(225, expert.nodes.count { it.original is NodeContent.Empty })
        // Same 85 abilities the editor offers.
        assertEquals(85, expert.abilities.size)
    }

    @Test
    fun isSmallerThanTheStandardGrid() {
        assertTrue(
            "Expert grid (${expert.totalNodes}) should have fewer nodes than Standard (${standard.totalNodes})",
            expert.totalNodes < standard.totalNodes
        )
    }

    @Test
    fun isOneConnectedWeb() {
        val adjacency = HashMap<String, MutableSet<String>>()
        expert.edges.forEach { e ->
            adjacency.getOrPut(e.fromId) { mutableSetOf() }.add(e.toId)
            adjacency.getOrPut(e.toId) { mutableSetOf() }.add(e.fromId)
        }
        val start = expert.nodes.first().id
        val seen = mutableSetOf(start)
        val stack = ArrayDeque(listOf(start))
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            adjacency[n]?.forEach { m -> if (seen.add(m)) stack.addLast(m) }
        }
        assertEquals(
            "Every Expert node should be reachable through the grid",
            expert.totalNodes,
            seen.size
        )
    }

    @Test
    fun nodeIdsUseTheExpertNamespace() {
        assertTrue(
            "Every Expert node id must start with the Expert prefix so it can't collide with Standard",
            expert.nodes.all { it.id.startsWith(GridType.EXPERT.idPrefix) }
        )
        val ids = expert.nodes.map { it.id }
        assertEquals("Node ids must be unique - they are database keys", ids.size, ids.toSet().size)
    }

    @Test
    fun standardAndExpertIdsNeverCollide() {
        val overlap = standard.nodes.map { it.id }.toSet() intersect expert.nodes.map { it.id }.toSet()
        assertTrue("Standard and Expert node ids must be disjoint, found $overlap", overlap.isEmpty())
    }

    @Test
    fun everyEdgeJoinsRealNodes() {
        val ids = expert.nodes.map { it.id }.toSet()
        expert.edges.forEach { edge ->
            assertTrue("Edge from ${edge.fromId} references a missing node", edge.fromId in ids)
            assertTrue("Edge to ${edge.toId} references a missing node", edge.toId in ids)
        }
    }

    @Test
    fun lockNodesCarryAValidLevel() {
        expert.nodes.map { it.original }.filterIsInstance<NodeContent.Lock>().forEach {
            assertTrue("Lock level should be 1-4, was ${it.level}", it.level in 1..4)
        }
    }

    @Test
    fun sharesTheStandardAbilityCatalog() {
        // Same game, same 85 abilities - the editor offers the full catalog on the Expert grid too.
        assertEquals(85, expert.abilities.size)
    }
}
