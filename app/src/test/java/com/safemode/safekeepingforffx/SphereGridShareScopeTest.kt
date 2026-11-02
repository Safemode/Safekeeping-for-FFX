package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridBounds
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridNode
import com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edits and paths are stored for both grids at once, but a shared build or saved route carries only
 * the grid in view. Share and Save are offered only when that grid has work on it, or they would
 * produce an empty route the library can't show.
 */
class SphereGridShareScopeTest {

    private val standard = GridData(
        nodes = listOf(
            SphereGridNode("n1", 0f, 0f, NodeContent.Empty),
            SphereGridNode("n2", 10f, 0f, NodeContent.Attribute(NodeType.HP, 200))
        ),
        edges = emptyList(),
        bounds = GridBounds(0f, 0f, 10f, 0f)
    )

    private val edit = NodeContent.Attribute(NodeType.STRENGTH, 4)

    @Test
    fun nothingAnywhereHasNothingToShare() {
        assertFalse(SphereGridUiState(grid = standard).hasAnythingToShare)
    }

    @Test
    fun anEditOnThisGridCanBeShared() {
        assertTrue(SphereGridUiState(grid = standard, overrides = mapOf("n1" to edit)).hasAnythingToShare)
    }

    @Test
    fun aPathOnThisGridCanBeShared() {
        assertTrue(SphereGridUiState(grid = standard, activated = setOf("n2")).hasAnythingToShare)
    }

    @Test
    fun workOnlyOnTheOtherGridHasNothingToShareHere() {
        val state = SphereGridUiState(
            grid = standard,
            overrides = mapOf("x5" to edit),
            activated = setOf("x7", "x8")
        )
        assertFalse(state.hasAnythingToShare)
    }
}
