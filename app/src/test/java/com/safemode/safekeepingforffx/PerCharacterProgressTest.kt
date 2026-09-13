package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.OverdriveModes
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerCharacterProgressTest {

    private val overdrive = OverdriveModes.category

    @Test
    fun `Overdrive Modes is the only per-character list`() {
        assertTrue(overdrive.perCharacter)
        favoriteSources.categories
            .filter { it.id != OverdriveModes.CATEGORY_ID }
            .forEach { assertFalse("${it.id} should not be per character", it.perCharacter) }
    }

    @Test
    fun `each character's ticks are stored under a key of their own`() {
        assertEquals("overdrive_modes_tidus", overdrive.progressKey(GridCharacter.TIDUS))
        assertEquals("overdrive_modes_rikku", overdrive.progressKey(GridCharacter.RIKKU))

        val keys = overdrive.progressKeys
        assertEquals(GridCharacter.entries.size, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        // None may land on the shared key, or one character's ticks would show for another.
        assertFalse(overdrive.id in keys)
    }

    @Test
    fun `other lists ignore the character and keep their own key`() {
        assertEquals(AlBhedPrimers.CATEGORY_ID, AlBhedPrimers.category.progressKey(GridCharacter.YUNA))
        assertEquals(listOf(AlBhedPrimers.CATEGORY_ID), AlBhedPrimers.category.progressKeys)
    }

    @Test
    fun `a per-character favorite is only done once every character has it`() {
        val stoic = "overdrive_mode_stoic"
        val warrior = "overdrive_mode_warrior"
        val everyoneStoic = overdrive.progressKeys.associateWith { setOf(stoic) }
        val onlyTidusWarrior = everyoneStoic + (overdrive.progressKey(GridCharacter.TIDUS) to setOf(stoic, warrior))

        assertEquals(setOf(stoic), overdrive.checkedEverywhere(onlyTidusWarrior))
        assertEquals(emptySet<String>(), overdrive.checkedEverywhere(emptyMap()))
    }

    @Test
    fun `other lists count their own ticks as done`() {
        val ticks = mapOf(AlBhedPrimers.CATEGORY_ID to setOf("albhed_01"))
        assertEquals(setOf("albhed_01"), AlBhedPrimers.category.checkedEverywhere(ticks))
    }
}
