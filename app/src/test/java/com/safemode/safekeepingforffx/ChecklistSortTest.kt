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
            storyStage = it.storyStage,
            stageNote = it.stageNote
        )
    }

    private fun List<ChecklistItem>.stageOf(id: String) = first { it.id == id }.storyStage

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
    fun `every celestial weapon entry explains what getting it there involves`() {
        val missing = CelestialWeapons.items.filter { it.stageNote.isNullOrBlank() }
        assertEquals(emptyList<ReferenceItem>(), missing)
    }

    @Test
    fun `grouped order leaves the sequence exactly as declared`() {
        assertEquals(
            celestial.map { it.id },
            celestial.inOrder(ChecklistSort.GROUPED).map { it.id }
        )
    }

    @Test
    fun `grouped order drops the stage notes and keeps the original headers`() {
        val grouped = celestial.inOrder(ChecklistSort.GROUPED)

        assertTrue(grouped.all { it.stageNote == null })
        assertEquals("Tidus - Caladbolg", grouped.first { it.id == "celestial_tidus_weapon" }.section)
    }

    @Test
    fun `chronological order keeps the stage notes`() {
        val sorted = celestial.inOrder(ChecklistSort.CHRONOLOGICAL)
        assertTrue(sorted.all { !it.stageNote.isNullOrBlank() })
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

    /**
     * The three that are easy to get wrong by reasoning from "when would I actually do this"
     * instead of "when does the game first allow it". Each one was mis-staged once already.
     */
    @Test
    fun `availability is judged by what the game allows, not by what is sensible`() {
        // Blitzball opens at save spheres the moment Luca ends. The Sigil being a long grind is a
        // fact about the player's time, not about when the game hands it over.
        assertEquals(StoryStage.LUCA, celestial.stageOf("celestial_wakka_sigil"))

        // Reachable by walking back south to the Thunder Plains; it does not wait for the airship.
        assertEquals(StoryStage.CALM_LANDS, celestial.stageOf("celestial_kimahri_weapon"))

        // The inverse trap: the butterfly hunt is playable from your first visit to the woods, but
        // its prize table only includes the Sigil once you have the airship.
        assertEquals(StoryStage.AIRSHIP, celestial.stageOf("celestial_kimahri_sigil"))
    }

    @Test
    fun `Tidus's crest and sigil are not transposed`() {
        // The Sun Crest is the Zanarkand one, behind Yunalesca; the Sun Sigil is the 0-0-0 chocobo
        // race back in the Calm Lands. Swapping these is the most common way to get Tidus wrong.
        assertEquals(StoryStage.ZANARKAND_RUINS, celestial.stageOf("celestial_tidus_crest"))
        assertEquals(StoryStage.CALM_LANDS, celestial.stageOf("celestial_tidus_sigil"))
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
