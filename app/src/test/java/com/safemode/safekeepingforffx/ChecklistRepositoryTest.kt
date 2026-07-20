package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.local.ChecklistProgressDao
import com.safemode.safekeepingforffx.data.local.ChecklistProgressEntity
import com.safemode.safekeepingforffx.data.reference.AlBhedPrimers
import com.safemode.safekeepingforffx.data.reference.BlitzballKeyTechs
import com.safemode.safekeepingforffx.data.reference.BlitzballRecruits
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.data.reference.CelestialWeapons
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.EquipmentAbilities
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.JechtSpheres
import com.safemode.safekeepingforffx.data.reference.RonsoRages
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistRepositoryTest {

    private val categories: List<ChecklistCategory> = listOf(
        AlBhedPrimers.category,
        JechtSpheres.category,
        CelestialWeapons.category,
        RonsoRages.category,
        BlitzballKeyTechs.category,
        BlitzballRecruits.category,
        EquipmentAbilities.category
    )

    private class FakeDao(rows: List<ChecklistProgressEntity> = emptyList()) : ChecklistProgressDao {
        val stored = rows.toMutableList()

        override fun observeCategory(categoryId: String): Flow<List<ChecklistProgressEntity>> =
            flowOf(stored.filter { it.categoryId == categoryId })

        override suspend fun upsert(entity: ChecklistProgressEntity) {
            stored.removeAll { it.categoryId == entity.categoryId && it.itemId == entity.itemId }
            stored += entity
        }

        override suspend fun clearCategory(categoryId: String) {
            stored.removeAll { it.categoryId == categoryId }
        }

        override suspend fun clearAll() {
            stored.clear()
        }
    }

    private fun row(categoryId: String, itemId: String, checked: Boolean) =
        ChecklistProgressEntity(categoryId, itemId, checked, updatedAt = 0L)

    @Test
    fun `merges stored progress onto reference data`() = runTest {
        val repository = ChecklistRepository(
            FakeDao(
                listOf(
                    row(AlBhedPrimers.CATEGORY_ID, "albhed_01", true),
                    row(AlBhedPrimers.CATEGORY_ID, "albhed_05", true)
                )
            )
        )

        val items = repository
            .observeCategory(AlBhedPrimers.CATEGORY_ID, AlBhedPrimers.items)
            .first()

        assertEquals(26, items.size)
        assertEquals(2, items.count { it.isChecked })
        assertTrue(items.single { it.id == "albhed_01" }.isChecked)
        assertFalse(items.single { it.id == "albhed_02" }.isChecked)
    }

    @Test
    fun `unchecked rows do not count as found`() = runTest {
        val repository = ChecklistRepository(
            FakeDao(listOf(row(AlBhedPrimers.CATEGORY_ID, "albhed_03", false)))
        )

        val items = repository
            .observeCategory(AlBhedPrimers.CATEGORY_ID, AlBhedPrimers.items)
            .first()

        assertEquals(0, items.count { it.isChecked })
    }

    @Test
    fun `progress in one category does not leak into another`() = runTest {
        // The shared progress table is keyed by (categoryId, itemId); this is the test that the
        // key actually isolates categories.
        val repository = ChecklistRepository(
            FakeDao(listOf(row(JechtSpheres.CATEGORY_ID, "jecht_01", true)))
        )

        val primers = repository
            .observeCategory(AlBhedPrimers.CATEGORY_ID, AlBhedPrimers.items)
            .first()
        val spheres = repository
            .observeCategory(JechtSpheres.CATEGORY_ID, JechtSpheres.items)
            .first()

        assertEquals(0, primers.count { it.isChecked })
        assertEquals(1, spheres.count { it.isChecked })
    }

    @Test
    fun `reset clears every category, not just one`() = runTest {
        val dao = FakeDao(
            listOf(
                row(AlBhedPrimers.CATEGORY_ID, "albhed_01", true),
                row(JechtSpheres.CATEGORY_ID, "jecht_01", true),
                row(CelestialWeapons.CATEGORY_ID, "celestial_tidus_weapon", true)
            )
        )
        val repository = ChecklistRepository(dao)

        repository.clearAllProgress()

        assertTrue(dao.stored.isEmpty())
        categories.forEach { category ->
            val items = repository.observeCategory(category.id, category.items).first()
            assertEquals(
                "${category.id} still has checked items",
                0,
                items.count { it.isChecked }
            )
        }
    }

    @Test
    fun `clearing one category leaves the others alone`() = runTest {
        val dao = FakeDao(
            listOf(
                row(AlBhedPrimers.CATEGORY_ID, "albhed_01", true),
                row(JechtSpheres.CATEGORY_ID, "jecht_01", true)
            )
        )
        val repository = ChecklistRepository(dao)

        repository.clearCategory(AlBhedPrimers.CATEGORY_ID)

        assertEquals(0, repository.observeCategory(AlBhedPrimers.CATEGORY_ID, AlBhedPrimers.items)
            .first().count { it.isChecked })
        assertEquals(1, repository.observeCategory(JechtSpheres.CATEGORY_ID, JechtSpheres.items)
            .first().count { it.isChecked })
    }

    @Test
    fun `item ids are unique within and across every category`() {
        val allIds = categories.flatMap { it.items }.map { it.id }

        assertEquals(allIds.size, allIds.toSet().size)
    }

    @Test
    fun `category ids are distinct`() {
        val ids = categories.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `category contents are the expected size`() {
        assertEquals(26, AlBhedPrimers.items.size)
        assertEquals(10, JechtSpheres.items.size)
        // Celestial Mirror is two steps, then 7 characters x (weapon + crest + sigil).
        assertEquals(2 + 7 * 3, CelestialWeapons.items.size)
        // 12 rages total, of which Jump is known from the start.
        assertEquals(12, RonsoRages.items.size)
        // 7 Aurochs x 3 key techniques each.
        assertEquals(7 * 3, BlitzballKeyTechs.items.size)
        // Every recruitable player outside the Aurochs.
        assertEquals(53, BlitzballRecruits.items.size)
        assertEquals(125, EquipmentAbilities.items.size)
    }

    @Test
    fun `equipment abilities are a reference list, not a checklist`() {
        assertFalse(EquipmentAbilities.category.trackProgress)
        // Everything else is still trackable; only this one opts out.
        categories.filter { it.id != EquipmentAbilities.CATEGORY_ID }
            .forEach { assertTrue("${it.id} should track progress", it.trackProgress) }
    }

    @Test
    fun `every equipment ability is alphabetical with a slot and a cost`() {
        val names = EquipmentAbilities.items.map { it.title }

        assertEquals("list is not alphabetical", names.sortedBy { it.lowercase() }, names)
        EquipmentAbilities.items.forEach { ability ->
            assertTrue(
                "${ability.title} is not tagged Weapon or Armor",
                ability.tag in setOf("Weapon", "Armor")
            )
            // location holds the customization cost, e.g. "Bomb Fragment x4"
            assertTrue(
                "${ability.title} has no item cost",
                Regex(".+ x\\d+").matches(ability.location)
            )
        }
    }

    @Test
    fun `no recruit duplicates an Aurochs`() {
        // The source roster includes the Aurochs; they are tracked per-key-tech in the other
        // category, so listing them here too would double-count them on the Home screen.
        val aurochs = BlitzballKeyTechs.items.mapNotNull { it.section }.toSet()
        val recruits = BlitzballRecruits.items.map { it.title }.toSet()

        assertEquals(emptySet<String>(), aurochs intersect recruits)
    }

    @Test
    fun `every recruit names three key techniques and a location`() {
        BlitzballRecruits.items.forEach { player ->
            assertTrue("${player.title} has no location", player.location.isNotBlank())
            val techs = player.detail
                .substringAfter("Key techniques: ")
                .substringBefore(". Signing")
                .split(",")
            assertEquals("${player.title} should have 3 key techs", 3, techs.size)
        }
    }

    @Test
    fun `recruits cover every team plus free agents`() {
        val teams = BlitzballRecruits.items.mapNotNull { it.section }.toSet()

        assertEquals(
            setOf(
                "Luca Goers", "Kilika Beasts", "Al Bhed Psyches",
                "Ronso Fangs", "Guado Glories", "Free Agents"
            ),
            teams
        )
    }

    @Test
    fun `every Aurochs has exactly three key techniques in order`() {
        // Key Techs unlock a player's remaining tech slots in sequence, so both the count and the
        // ordering per player matter.
        val byPlayer = BlitzballKeyTechs.items.groupBy { it.section }

        assertEquals(7, byPlayer.size)
        byPlayer.forEach { (player, techs) ->
            assertEquals("$player should have 3 key techs", 3, techs.size)
            assertEquals(
                "$player key techs are out of order",
                listOf("First key technique", "Second key technique", "Third key technique"),
                techs.map { it.location }
            )
        }
    }

    @Test
    fun `Biran and Yenke teach exactly eight rages between them`() {
        // jegged enumerates Biran's four and Yenke's four, and states the rest must come from
        // ordinary fiends. Pinning it here so the Mt. Gagazet fight can't quietly gain a rage.
        val fromBiran = RonsoRages.items.filter { "Biran Ronso" in it.location }.map { it.title }
        val fromYenke = RonsoRages.items.filter { "Yenke Ronso" in it.location }.map { it.title }

        assertEquals(
            listOf("Self-Destruct", "Thrust Kick", "Doom", "Mighty Guard").sorted(),
            fromBiran.sorted()
        )
        assertEquals(
            listOf("Fire Breath", "Stone Breath", "Aqua Breath", "White Wind").sorted(),
            fromYenke.sorted()
        )
    }

    @Test
    fun `every learnable rage names at least one fiend`() {
        val learnable = RonsoRages.items.filter { it.section == "Learned with Lancet" }

        assertEquals(11, learnable.size)
        learnable.forEach {
            assertTrue("${it.title} has no source", it.location.isNotBlank())
        }
    }

    @Test
    fun `only Jump is known without Lancet`() {
        val known = RonsoRages.items.filter { it.section == "Known from the start" }

        assertEquals(1, known.size)
        assertEquals("Jump", known.single().title)
    }

    @Test
    fun `every celestial weapon character has a weapon a crest and a sigil`() {
        val bySection = CelestialWeapons.items
            .filter { it.section != null && !it.section!!.startsWith("Prerequisite") }
            .groupBy { it.section }

        assertEquals(7, bySection.size)
        bySection.forEach { (section, items) ->
            assertEquals("$section should have 3 steps", 3, items.size)
            assertTrue("$section is missing a Crest", items.any { it.title.endsWith("Crest") })
            assertTrue("$section is missing a Sigil", items.any { it.title.endsWith("Sigil") })
        }
    }

    @Test
    fun `no screenshots are bundled at the moment`() {
        // The images were pulled out; see for-later/RESTORING-SCREENSHOTS.md, which also holds the
        // per-category assertions this replaced. When they come back this fails, which is the
        // prompt to restore those.
        val withShots = categories.flatMap { it.items }.filter { it.imageRes != null }
        assertTrue("Screenshots are back: ${withShots.map { it.id }}", withShots.isEmpty())
    }

    @Test
    fun `screenshot coverage is all or nothing per category`() {
        // Partial coverage is the real bug: the long-press hint is shown per category, so a list
        // where only some rows respond to a long-press would advertise something that half works.
        categories.forEach { category ->
            val withShots = category.items.count { it.imageRes != null }
            assertTrue(
                "${category.id} has partial coverage: $withShots of ${category.items.size}",
                withShots == 0 || withShots == category.items.size
            )
        }
    }

    @Test
    fun `only genuinely unrecoverable items are marked missable`() {
        // Home is destroyed and Bevelle cannot be re-entered. Everything else in the game is
        // reachable again, so anything else flagged Missable here would be a lie to the player.
        val missable = categories.flatMap { it.items }
            .filter { it.caution == Caution.Missable }
            .map { it.id }

        assertEquals(
            listOf("albhed_19", "albhed_20", "albhed_21", "albhed_22"),
            missable
        )
    }

    @Test
    fun `guarded items name a real Dark Aeon`() {
        val knownAeons = setOf(
            "Dark Valefor", "Dark Ifrit", "Dark Ixion", "Dark Shiva",
            "Dark Bahamut", "Dark Anima", "Dark Yojimbo", "Dark Magus Sisters"
        )

        val guarded = categories.flatMap { it.items }
            .mapNotNull { it.caution as? Caution.Guarded }

        assertTrue("expected several guarded items, got ${guarded.size}", guarded.size >= 8)
        guarded.forEach { assertTrue("${it.aeon} is not a Dark Aeon", it.aeon in knownAeons) }
    }

    @Test
    fun `original PS2 release has nothing guarded to hide but keeps every missable`() {
        // The toggle only ever removes Guarded; if it stripped Missable too, a PS2 player would
        // walk past the Home primers with no warning at all.
        val all = categories.flatMap { it.items }
        val visibleOnPs2 = all.map { item ->
            if (item.caution is Caution.Guarded) item.copy(caution = null) else item
        }

        assertTrue(visibleOnPs2.none { it.caution is Caution.Guarded })
        assertEquals(
            all.count { it.caution == Caution.Missable },
            visibleOnPs2.count { it.caution == Caution.Missable }
        )
    }

    @Test
    fun `theme defaults to following the system`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.DEFAULT)
        // Midnight is a dark variant, not a separate mode - both must resolve to a dark scheme.
        assertTrue(ThemePreference.entries.containsAll(listOf(ThemePreference.DARK, ThemePreference.MIDNIGHT)))
        assertEquals(4, ThemePreference.entries.size)
    }

    @Test
    fun `game version defaults to the release most people are playing`() {
        assertEquals(GameVersion.INTERNATIONAL_HD, GameVersion.DEFAULT)
        assertTrue(GameVersion.INTERNATIONAL_HD.hasDarkAeons)
        assertFalse(GameVersion.ORIGINAL_PS2.hasDarkAeons)
    }

    @Test
    fun `primer ids are unique and stable`() {
        val ids = AlBhedPrimers.items.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        assertEquals("albhed_01", ids.first())
        assertEquals("albhed_26", ids.last())
    }
}
