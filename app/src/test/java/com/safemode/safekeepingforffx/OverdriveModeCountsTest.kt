package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.OverdriveModes
import com.safemode.safekeepingforffx.ui.screens.checklist.characterNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverdriveModeCountsTest {

    private fun countFor(key: String, character: GridCharacter): Int? =
        OverdriveModes.items.single { it.id == "overdrive_mode_$key" }.characterCounts[character]

    @Test
    fun `Warrior takes 150 for Tidus and 200 for Yuna`() {
        assertEquals(150, countFor("warrior", GridCharacter.TIDUS))
        assertEquals(200, countFor("warrior", GridCharacter.YUNA))
    }

    @Test
    fun `every mode but Stoic has a figure for every character`() {
        OverdriveModes.items.forEach { mode ->
            if (mode.id == "overdrive_mode_stoic") {
                assertTrue("Stoic is known from the start", mode.characterCounts.isEmpty())
            } else {
                assertEquals("${mode.title} is missing a character", GridCharacter.entries.toSet(), mode.characterCounts.keys)
                mode.characterCounts.values.forEach { assertTrue("${mode.title} has a non-positive count", it > 0) }
            }
        }
    }

    @Test
    fun `spot checks against jegged`() {
        // Chosen where a transposed character would show: the page lists the cast in a different
        // order from the picker, and these are the figures that stand out in their rows.
        assertEquals(1000, countFor("coward", GridCharacter.AURON))
        assertEquals(35, countFor("loner", GridCharacter.AURON))
        assertEquals(180, countFor("loner", GridCharacter.YUNA))
        assertEquals(110, countFor("rook", GridCharacter.YUNA))
        assertEquals(260, countFor("daredevil", GridCharacter.AURON))
        assertEquals(60, countFor("tactician", GridCharacter.KIMAHRI))
        assertEquals(300, countFor("dancer", GridCharacter.LULU))
    }

    @Test
    fun `the row names the character and groups thousands`() {
        assertEquals("Tidus: 150 to learn", characterNote(GridCharacter.TIDUS, 150))
        assertEquals("Auron: 1,000 to learn", characterNote(GridCharacter.AURON, 1000))
    }
}
