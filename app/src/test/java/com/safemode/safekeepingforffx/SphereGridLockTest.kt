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

    // --- What the player may rewrite, which the full-node-editor setting governs ---

    private fun abilityNode(id: String = "a1") =
        SphereGridNode(id, 0f, 0f, NodeContent.Ability("Cure", NodeType.WHITE_MAGIC))

    private fun statNode(id: String = "s1") =
        SphereGridNode(id, 0f, 0f, NodeContent.Attribute(NodeType.STRENGTH, 4))

    @Test
    fun abilityNodesAreActivateOnlyByDefault() {
        val state = SphereGridUiState()
        assertFalse("An ability node is not editable by default", state.canEdit(abilityNode()))
        assertTrue("A stat node stays editable", state.canEdit(statNode()))
    }

    @Test
    fun theFullEditorSettingUnlocksAbilityNodes() {
        val state = SphereGridUiState(fullNodeEditor = true)
        assertTrue("The setting makes ability nodes editable", state.canEdit(abilityNode()))
    }

    /** The setting governs abilities only - it must never make a gated lock editable. */
    @Test
    fun theFullEditorSettingDoesNotUnlockGates() {
        val state = SphereGridUiState(fullNodeEditor = true)
        assertFalse("A gated lock is still not editable", state.canEdit(lockNode()))
    }

    /** An edit that put an ability on a node also makes that node activate-only once shut off. */
    @Test
    fun anAbilityWrittenByAnEditIsAlsoProtected() {
        val node = statNode("s2")
        val state = SphereGridUiState(
            overrides = mapOf("s2" to NodeContent.Ability("Holy", NodeType.WHITE_MAGIC))
        )
        assertFalse("The current content decides, not the original", state.canEdit(node))
    }
}
