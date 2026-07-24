package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.ui.screens.spheregrid.AttributeCatalog
import com.safemode.safekeepingforffx.ui.screens.spheregrid.MaxStatAttributeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The node editor's short list, shown unless the player turns the full catalog on. It has to be
 * exactly HP +300, MP +40 and the +4 attributes - a wrong entry here silently changes what a
 * max-stats plan can place.
 */
class NodeEditorCatalogTest {

    @Test
    fun shortListIsTheMaxStatSpheresOnly() {
        val expected = listOf(
            NodeContent.Attribute(NodeType.HP, 300),
            NodeContent.Attribute(NodeType.MP, 40),
            NodeContent.Attribute(NodeType.STRENGTH, 4),
            NodeContent.Attribute(NodeType.DEFENSE, 4),
            NodeContent.Attribute(NodeType.MAGIC, 4),
            NodeContent.Attribute(NodeType.MAGIC_DEFENSE, 4),
            NodeContent.Attribute(NodeType.AGILITY, 4),
            NodeContent.Attribute(NodeType.ACCURACY, 4),
            NodeContent.Attribute(NodeType.EVASION, 4),
            NodeContent.Attribute(NodeType.LUCK, 4)
        )
        assertEquals(expected.toSet(), MaxStatAttributeCatalog.toSet())
    }

    @Test
    fun shortListLeavesOutTheSmallerSpheres() {
        listOf(
            NodeContent.Attribute(NodeType.HP, 200),
            NodeContent.Attribute(NodeType.MP, 10),
            NodeContent.Attribute(NodeType.MP, 20),
            NodeContent.Attribute(NodeType.STRENGTH, 1),
            NodeContent.Attribute(NodeType.LUCK, 3)
        ).forEach { dropped ->
            assertFalse(
                "${dropped.attribute} +${dropped.value} should not be in the short list",
                dropped in MaxStatAttributeCatalog
            )
        }
    }

    /** The short list is a filter of the full one, so nothing can appear that the editor lacks. */
    @Test
    fun shortListIsASubsetOfTheFullCatalog() {
        assertTrue(AttributeCatalog.containsAll(MaxStatAttributeCatalog))
        assertTrue(MaxStatAttributeCatalog.size < AttributeCatalog.size)
    }

    /** Every stat that has +1..+4 nodes contributes its +4, so no stat is unreachable. */
    @Test
    fun everyGradedStatKeepsItsPlusFour() {
        val graded = NodeType.attributes - setOf(NodeType.HP, NodeType.MP)
        graded.forEach { attribute ->
            assertTrue(
                "$attribute should keep a +4 in the short list",
                MaxStatAttributeCatalog.any { it.attribute == attribute && it.value == 4 }
            )
        }
    }
}
