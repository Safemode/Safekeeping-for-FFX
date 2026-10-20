package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import com.safemode.safekeepingforffx.ui.screens.spheregrid.AttributeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * [NodeContent.decode] reads both the player's stored edits and build codes pasted in from anywhere,
 * so it has to accept everything the app itself can produce and refuse content that makes no sense.
 */
class NodeContentCodecTest {

    /** Every vanilla node, plus every ability the editor offers on that grid. */
    @Test
    fun everythingOnBothGridsRoundTrips() {
        GridType.entries.forEach { type ->
            val asset = type.asset ?: return@forEach
            val grid = SphereGridParser.parse(File("src/main/assets/$asset").readText(), type.idPrefix)
            grid.nodes.forEach { node ->
                assertEquals("${node.id} on $type", node.original, NodeContent.decode(node.original.encode()))
            }
            grid.abilities.forEach { ability ->
                val content = NodeContent.Ability(ability.name, ability.family)
                assertEquals("${ability.name} on $type", content, NodeContent.decode(content.encode()))
            }
        }
    }

    @Test
    fun everyAttributeTheEditorOffersRoundTrips() {
        AttributeCatalog.forEach { assertEquals(it, NodeContent.decode(it.encode())) }
    }

    @Test
    fun attributeValuesOutsideTheRangeAreRefused() {
        assertNull(NodeContent.decode("A|STRENGTH|0"))
        assertNull(NodeContent.decode("A|STRENGTH|-4"))
        assertNull(NodeContent.decode("A|HP|2147483647"))
    }

    @Test
    fun anAttributeMustNameAStat() {
        assertNull(NodeContent.decode("A|WHITE_MAGIC|4"))
        assertNull(NodeContent.decode("A|LOCK|4"))
        assertNull(NodeContent.decode("A|NOT_A_STAT|4"))
    }

    @Test
    fun anAbilityMustHaveANameAndAnAbilityFamily() {
        assertNull(NodeContent.decode("B||WHITE_MAGIC"))
        assertNull(NodeContent.decode("B|Cure|STRENGTH"))
        assertNull(NodeContent.decode("B|Cure|NOT_A_FAMILY"))
        assertEquals(
            NodeContent.Ability("Cure", NodeType.WHITE_MAGIC),
            NodeContent.decode("B|Cure|WHITE_MAGIC")
        )
    }

    @Test
    fun lockLevelsOutsideOneToFourAreRefused() {
        assertNull(NodeContent.decode("L|0"))
        assertNull(NodeContent.decode("L|5"))
        assertEquals(NodeContent.Lock(4), NodeContent.decode("L|4"))
    }
}
