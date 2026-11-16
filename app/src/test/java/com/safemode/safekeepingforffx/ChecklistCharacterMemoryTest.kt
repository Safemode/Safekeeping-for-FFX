package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.ui.screens.checklist.ChecklistCharacterMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChecklistCharacterMemoryTest {

    @Test
    fun `a list finds the character it was left on`() {
        val memory = ChecklistCharacterMemory()
        memory.forCategory("overdrive_modes").value = GridCharacter.LULU

        // A new ViewModel for the same list - the one built when the player comes back - reads the same.
        assertSame(memory.forCategory("overdrive_modes"), memory.forCategory("overdrive_modes"))
        assertEquals(GridCharacter.LULU, memory.forCategory("overdrive_modes").value)
    }

    @Test
    fun `each list keeps its own character`() {
        val memory = ChecklistCharacterMemory()
        memory.forCategory("overdrive_modes").value = GridCharacter.AURON
        assertEquals(GridCharacter.DEFAULT, memory.forCategory("another_list").value)
    }
}
