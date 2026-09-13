package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.DestructionSpheres
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.JechtSpheres
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.data.reference.StoryStage
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import com.safemode.safekeepingforffx.ui.screens.missables.TimelineItem
import com.safemode.safekeepingforffx.ui.screens.missables.UNSTAGED_LABEL
import com.safemode.safekeepingforffx.ui.screens.missables.buildTimelineStages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissablesTimelineTest {

    private fun item(
        id: String,
        stage: StoryStage?,
        caution: Caution?,
        checked: Boolean = false
    ) = TimelineItem(
        categoryId = "cat",
        categoryLabel = "Cat",
        item = ChecklistItem(
            id = id,
            title = id,
            location = "",
            detail = "",
            caution = caution,
            isChecked = checked,
            storyStage = stage
        )
    )

    private fun ReferenceItem.toTimelineItem(category: ChecklistCategory) = TimelineItem(
        categoryId = category.id,
        categoryLabel = category.label,
        item = ChecklistItem(
            id = id, title = title, location = location, detail = detail, caution = caution,
            isChecked = false, section = section, tag = tag, imageRes = imageRes,
            storyStage = storyStage, stageNote = stageNote
        )
    )

    private val sample = listOf(
        item("guarded_calm", StoryStage.CALM_LANDS, Caution.Guarded("Dark Bahamut")),
        item("missable_besaid", StoryStage.BESAID, Caution.Missable),
        item("missable_unstaged", null, Caution.Missable),
        item("plain", StoryStage.LUCA, caution = null)
    )

    @Test
    fun `items with no caution never appear`() {
        val ids = buildTimelineStages(sample, GameVersion.INTERNATIONAL_HD)
            .flatMap { it.items }
            .map { it.item.id }
        assertTrue("plain" !in ids)
    }

    @Test
    fun `stages come out in story order with the unstaged bucket last`() {
        val labels = buildTimelineStages(sample, GameVersion.INTERNATIONAL_HD).map { it.label }
        assertEquals(
            listOf(StoryStage.BESAID.label, StoryStage.CALM_LANDS.label, UNSTAGED_LABEL),
            labels
        )
    }

    @Test
    fun `guarded items drop on the original PS2 release but missable ones stay`() {
        val ps2 = buildTimelineStages(sample, GameVersion.ORIGINAL_PS2)
            .flatMap { it.items }
            .map { it.item.id }
        assertEquals(listOf("missable_besaid", "missable_unstaged"), ps2)
    }

    @Test
    fun `guarded items are kept on the international and HD release`() {
        val hd = buildTimelineStages(sample, GameVersion.INTERNATIONAL_HD)
            .flatMap { it.items }
            .map { it.item.id }
        assertTrue("guarded_calm" in hd)
    }

    @Test
    fun `real destruction sphere data is all guarded, so PS2 shows none of it`() {
        val items = DestructionSpheres.category.items.map {
            it.toTimelineItem(DestructionSpheres.category)
        }
        // Two of the six carry a Dark-Aeon guard; the rest carry no caution at all.
        assertEquals(2, buildTimelineStages(items, GameVersion.INTERNATIONAL_HD).sumOf { it.items.size })
        assertEquals(0, buildTimelineStages(items, GameVersion.ORIGINAL_PS2).sumOf { it.items.size })
    }

    @Test
    fun `the Home primers get a Home section of their own`() {
        val items = AlBhedPrimers.category.items.map {
            it.toTimelineItem(AlBhedPrimers.category)
        }
        val stages = buildTimelineStages(items, GameVersion.ORIGINAL_PS2)

        val home = stages.single { it.label == StoryStage.HOME.label }
        assertEquals(listOf("albhed_19", "albhed_20", "albhed_21"), home.items.map { it.item.id })
        // Only the four truly missable primers survive on PS2: Home's three, then Bevelle's one.
        assertEquals(listOf(StoryStage.HOME.label, StoryStage.BEVELLE.label), stages.map { it.label })
        assertEquals(listOf("albhed_22"), stages.last().items.map { it.item.id })
    }

    @Test
    fun `every flagged item the timeline draws from has a stage`() {
        // An unstaged item drops into the catch-all at the bottom, away from where it is found -
        // which is how Primer II ended up outside the Besaid section.
        val unstaged = favoriteSources.categories.flatMap { category ->
            category.items
                .filter { it.caution != null && it.storyStage == null }
                .map { "${category.id}/${it.id}" }
        }
        assertEquals(emptyList<String>(), unstaged)
    }

    @Test
    fun `Jecht Spheres are staged by when they appear, not by where they sit`() {
        // Every sphere after the first only appears once Spherimorph is beaten, so the Besaid one is
        // a Macalania Woods backtrack, not a Besaid pickup.
        val stages = JechtSpheres.items.associate { it.id to it.storyStage }
        assertEquals(StoryStage.MACALANIA_WOODS, stages["jecht_02"])
        assertEquals(StoryStage.MT_GAGAZET, stages["jecht_10"])
    }
}
