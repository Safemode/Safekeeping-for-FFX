package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A lock is a gate until someone opens it. Opening is a shared, grid-wide change (a blank override),
 * so the gate reads as blank for every character and can then be edited like any empty node - these
 * checks pin that shared behaviour, independent of whose path is selected.
 */
class SphereGridLockTest {

    private fun lockNode(id: String = "n1", level: Int = 1) =
        SphereGridNode(id, 0f, 0f, NodeContent.Lock(level))

    @Test
    fun aLockIsAGatedLockUntilOpened() {
        val node = lockNode()
        val state = SphereGridUiState()
        assertTrue("A fresh lock is still a gate", state.isLockedGate(node))
        assertEquals("A gated lock shows its lock content", NodeContent.Lock(1), state.current(node))
    }

    @Test
    fun openingALockMakesItABlankNodeForEveryone() {
        val node = lockNode()
        // Opening writes a shared blank override - not tied to any character's activation set.
        val opened = SphereGridUiState(overrides = mapOf("n1" to NodeContent.Empty), activated = emptySet())
        assertFalse("An opened gate is no longer locked", opened.isLockedGate(node))
        assertEquals("An opened gate reads as blank", NodeContent.Empty, opened.current(node))
        assertTrue("A blank node is editable", opened.current(node).isEditable)
    }

    @Test
    fun editingAnOpenedGateIsSharedContent() {
        val node = lockNode()
        val edited = SphereGridUiState(
            overrides = mapOf("n1" to NodeContent.Attribute(NodeType.STRENGTH, 3))
        )
        assertFalse(edited.isLockedGate(node))
        assertEquals(NodeContent.Attribute(NodeType.STRENGTH, 3), edited.current(node))
    }

    @Test
    fun aNonLockNodeIsNeverAGate() {
        val statNode = SphereGridNode("n2", 0f, 0f, NodeContent.Attribute(NodeType.HP, 200))
        assertFalse(SphereGridUiState().isLockedGate(statNode))
    }
}
