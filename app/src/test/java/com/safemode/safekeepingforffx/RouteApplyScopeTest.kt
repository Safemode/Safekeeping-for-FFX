package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Narrowing a multi-character route to one character before it is applied. The repository replaces
 * only the characters a build carries activations for, so dropping the others here is what leaves
 * their live paths untouched.
 */
class RouteApplyScopeTest {

    private val edit = RouteEvent.Edit("n1", NodeContent.Attribute(NodeType.STRENGTH, 4))
    private val tidusA = RouteEvent.Activate(GridCharacter.TIDUS, "n1")
    private val yunaA = RouteEvent.Activate(GridCharacter.YUNA, "n2")
    private val tidusB = RouteEvent.Activate(GridCharacter.TIDUS, "n3")
    private val auronA = RouteEvent.Activate(GridCharacter.AURON, "n4")

    private val build = SphereGridBuild(
        gridType = GridType.STANDARD,
        events = listOf(edit, tidusA, yunaA, tidusB, auronA),
        name = "Three paths"
    )

    @Test
    fun charactersListsEveryPathInTheRoute() {
        assertEquals(
            listOf(GridCharacter.TIDUS, GridCharacter.YUNA, GridCharacter.AURON),
            build.characters
        )
    }

    @Test
    fun narrowingKeepsOnlyThatCharactersActivations() {
        val narrowed = build.forCharacterOnly(GridCharacter.TIDUS)
        assertEquals(listOf(GridCharacter.TIDUS), narrowed.characters)
        assertEquals(listOf(edit, tidusA, tidusB), narrowed.events)
    }

    /** Edits are grid-wide and the path depends on them, so they ride along however it is narrowed. */
    @Test
    fun narrowingKeepsTheGridEdits() {
        val narrowed = build.forCharacterOnly(GridCharacter.YUNA)
        assertTrue(narrowed.events.contains(edit))
        assertEquals(listOf(edit, yunaA), narrowed.events)
    }

    @Test
    fun narrowingPreservesTheRestOfTheBuild() {
        val narrowed = build.forCharacterOnly(GridCharacter.AURON)
        assertEquals(build.gridType, narrowed.gridType)
        assertEquals(build.name, narrowed.name)
    }

    /** A character with no path in the route narrows to edits only - never to someone else's path. */
    @Test
    fun narrowingToAnAbsentCharacterLeavesOnlyEdits() {
        val narrowed = build.forCharacterOnly(GridCharacter.LULU)
        assertEquals(listOf(edit), narrowed.events)
        assertTrue(narrowed.characters.isEmpty())
    }
}
