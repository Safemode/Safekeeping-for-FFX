package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.CelestialWeapons
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.data.reference.StoryStage
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.ui.screens.checklist.ChecklistSort
import com.safemode.safekeepingforffx.ui.screens.checklist.inOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistSortTest {

    private fun items(category: List<ReferenceItem>): List<ChecklistItem> = category.map {
        ChecklistItem(
            id = it.id,
            title = it.title,
            location = it.location,
            detail = it.detail,
            caution = it.caution,
            isChecked = false,
            section = it.section,
            tag = it.tag,
            imageRes = it.imageRes,
            storyStage = it.storyStage
        )
    }

    private val celestial = items(CelestialWeapons.items)

    @Test
    fun `every celestial weapon entry carries a story stage`() {
        val missing = CelestialWeapons.items.filter { it.storyStage == null }
        assertEquals(emptyList<ReferenceItem>(), missing)
        assertTrue(CelestialWeapons.category.hasStoryOrder)
    }

    @Test
    fun `categories without stages are not offered story order`() {
        assertFalse(AlBhedPrimers.category.hasStoryOrder)
    }

    @Test
    fun `grouped order leaves the list exactly as declared`() {
        assertEquals(celestial, celestial.inOrder(ChecklistSort.GROUPED))
    }

    @Test
    fun `chronological order never sends you backwards through the story`() {
        val stages = celestial.inOrder(ChecklistSort.CHRONOLOGICAL).map { it.storyStage!!.ordinal }
        assertEquals(stages.sorted(), stages)
    }

    @Test
    fun `chronological order keeps every entry exactly once`() {
        val sorted = celestial.inOrder(ChecklistSort.CHRONOLOGICAL)
        assertEquals(celestial.size, sorted.size)
        assertEquals(celestial.map { it.id }.toSet(), sorted.map { it.id }.toSet())
    }

    @Test
    fun `chronological order opens on the Moon Crest and ends after the airship`() {
        val sorted = celestial.inOrder(ChecklistSort.CHRONOLOGICAL)
        assertEquals("Moon Crest", sorted.first().title)
        assertEquals(StoryStage.BESAID, sorted.first().storyStage)
        assertEquals(StoryStage.AIRSHIP, sorted.last().storyStage)
    }

    @Test
    fun `chronological order groups by stage and badges the group it came from`() {
        val caladbolg = celestial.inOrder(ChecklistSort.CHRONOLOGICAL)
            .first { it.id == "celestial_tidus_weapon" }

        assertEquals("Calm Lands", caladbolg.section)
        // The header no longer says whose weapon this is, so the row has to.
        assertEquals("Tidus", caladbolg.tag)
    }

    @Test
    fun `the Celestial Mirror still comes before the weapons it unlocks`() {
        val sorted = celestial.inOrder(ChecklistSort.CHRONOLOGICAL).map { it.id }
        val mirror = sorted.indexOf("celestial_mirror_celestial")
        assertTrue("The Celestial Mirror is missing from the list", mirror >= 0)
        listOf("celestial_tidus_weapon", "celestial_yuna_weapon").forEach { weapon ->
            assertTrue(
                "$weapon should not be listed before the Celestial Mirror",
                sorted.indexOf(weapon) > mirror
            )
        }
    }

    @Test
    fun `stages a player reaches first sort first`() {
        assertEquals(
            listOf(
                StoryStage.BESAID,
                StoryStage.LUCA,
                StoryStage.MIIHEN_HIGHROAD,
                StoryStage.GUADOSALAM,
                StoryStage.THUNDER_PLAINS,
                StoryStage.MACALANIA_WOODS,
                StoryStage.BIKANEL_ISLAND,
                StoryStage.CALM_LANDS,
                StoryStage.MT_GAGAZET,
                StoryStage.ZANARKAND_RUINS,
                StoryStage.AIRSHIP
            ),
            StoryStage.entries.toList()
        )
    }
}
