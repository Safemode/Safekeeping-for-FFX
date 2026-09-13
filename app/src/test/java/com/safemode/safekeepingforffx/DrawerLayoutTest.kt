package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.BlitzballKeyTechs
import com.safemode.safekeepingforffx.data.reference.DarkAeonsAndPenance
import com.safemode.safekeepingforffx.data.reference.EquipmentAbilities
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_LABEL
import com.safemode.safekeepingforffx.data.reference.SPHERE_GRID_LABEL
import com.safemode.safekeepingforffx.data.reference.Trophies
import com.safemode.safekeepingforffx.domain.ProgressCount
import com.safemode.safekeepingforffx.ui.navigation.DrawerEntry
import com.safemode.safekeepingforffx.ui.navigation.FfxDestination
import com.safemode.safekeepingforffx.ui.navigation.GroupCount
import com.safemode.safekeepingforffx.ui.navigation.drawerDestinations
import com.safemode.safekeepingforffx.ui.navigation.drawerLayout
import com.safemode.safekeepingforffx.ui.navigation.sumByGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DrawerLayoutTest {

    private val groups = drawerLayout.filterIsInstance<DrawerEntry.Group>()

    private fun labelsIn(groupId: String) = groups.single { it.id == groupId }.destinations.map { it.label }

    @Test
    fun `Home, Favorites and the Missables Timeline sit ungrouped at the top`() {
        assertEquals(
            listOf(FfxDestination.Home, FfxDestination.Favorites, FfxDestination.MissablesTimeline),
            drawerLayout.take(3).map { (it as DrawerEntry.Single).destination }
        )
        assertEquals(3, drawerLayout.count { it is DrawerEntry.Single })
    }

    @Test
    fun `groups hold the lists that were decided, in order`() {
        assertEquals(
            listOf("Collectibles", "Party & Aeons", "Blitzball", "Endgame", "Tools & Reference"),
            groups.map { it.label }
        )
        assertEquals(listOf("Al Bhed Primers", "Jecht Spheres", "Destruction Spheres"), labelsIn("collectibles"))
        assertEquals(listOf("Aeons", "Celestial Weapons", "Overdrive Modes", "Ronso Rage"), labelsIn("party_aeons"))
        assertEquals(listOf("Blitzball Key Techs", "Blitzball Recruits"), labelsIn("blitzball"))
        assertEquals(listOf(MONSTER_ARENA_LABEL, "Dark Aeons & Penance", "Trophies"), labelsIn("endgame"))
        assertEquals(
            listOf(SPHERE_GRID_LABEL, "Mix Calculator", ItemListCsvParser.LABEL, EquipmentAbilities.category.label),
            labelsIn("tools_reference")
        )
    }

    @Test
    fun `only Tools & Reference goes without a count`() {
        assertEquals(listOf("tools_reference"), groups.filterNot { it.showProgress }.map { it.id })
    }

    @Test
    fun `every screen appears exactly once and Settings stays pinned outside the groups`() {
        val flattened = drawerLayout.flatMap { entry ->
            when (entry) {
                is DrawerEntry.Single -> listOf(entry.destination)
                is DrawerEntry.Group -> entry.destinations
            }
        }
        assertEquals(flattened, drawerDestinations)
        assertEquals(19, flattened.size)
        assertEquals(flattened.size, flattened.map { it.route }.toSet().size)
        assertFalse(FfxDestination.Settings in flattened)
    }

    @Test
    fun `Endgame counts the arena and drops Dark Aeons on the original PS2 release`() {
        val counts = listOf(
            GroupCount("endgame", null, ProgressCount(101, 102)),
            GroupCount("endgame", DarkAeonsAndPenance.category, ProgressCount(0, 9)),
            GroupCount("endgame", Trophies.category, ProgressCount(34, 34)),
            GroupCount("blitzball", BlitzballKeyTechs.category, ProgressCount(2, 21))
        )

        assertEquals(ProgressCount(135, 145), sumByGroup(counts, GameVersion.INTERNATIONAL_HD)["endgame"])
        assertEquals(ProgressCount(135, 136), sumByGroup(counts, GameVersion.ORIGINAL_PS2)["endgame"])
        // Other groups are untouched by the release.
        assertEquals(ProgressCount(2, 21), sumByGroup(counts, GameVersion.ORIGINAL_PS2)["blitzball"])
    }
}
