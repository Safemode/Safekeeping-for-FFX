package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.data.reference.computeCreationProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Exercises the creation unlock rules against the real bundled fiend list. */
class CreationUnlocksTest {

    private val asset: List<Monster> by lazy {
        val file = File("src/main/assets/monster_arena.csv")
        assertTrue("Missing asset: ${file.absolutePath}", file.exists())
        MonsterArenaCsvParser.parse(file.readText())
    }

    private fun idOf(name: String) = asset.first { it.name == name }.id

    private fun feedersOf(target: String) =
        asset.filter { it.isCapturable && it.details["Creation Unlock"] == target }

    @Test
    fun `nothing is unlocked before anything is captured`() {
        val progress = computeCreationProgress(asset, emptyMap())
        val creations = asset.filter { !it.isCapturable }

        // Every creation is accounted for, and every one is still locked.
        assertEquals(creations.size, progress.size)
        assertTrue(progress.values.none { it.unlocked })
    }

    @Test
    fun `every area and species creation resolves a real requirement`() {
        // Guards the area-name aliases and the punctuation-folding name match: a mapping that
        // silently found no feeders would show as a zero requirement here.
        val progress = computeCreationProgress(asset, emptyMap())
        val mapped = asset.filter { it.area == "Area Creations" || it.area == "Species Creations" }
        val broken = mapped.filter { progress.getValue(it.id).required == 0 }

        assertEquals(emptyList<String>(), broken.map { it.name })
    }

    @Test
    fun `a species creation unlocks once every feeder meets its amount`() {
        val feeders = feedersOf("Fenrir")
        assertTrue(feeders.isNotEmpty())

        val counts = feeders.associate { it.id to 3 }.toMutableMap()
        assertTrue(computeCreationProgress(asset, counts).getValue(idOf("Fenrir")).unlocked)

        // One feeder short of three and it locks again, and the tally reflects it.
        counts[feeders.first().id] = 2
        val locked = computeCreationProgress(asset, counts).getValue(idOf("Fenrir"))
        assertFalse(locked.unlocked)
        assertEquals(feeders.size, locked.required)
        assertEquals(feeders.size - 1, locked.current)
    }

    @Test
    fun `species names spelled differently from their feeders still match`() {
        // Fiends feed "One Eye" and "Iron Clad"; the creation rows are "One-Eye" and "Ironclad".
        for ((creation, feed) in listOf("One-Eye" to "One Eye", "Ironclad" to "Iron Clad")) {
            val feeders = feedersOf(feed)
            assertTrue("No feeders for $feed", feeders.isNotEmpty())
            val counts = feeders.associate {
                it.id to (it.details["Creation Unlock Amount Needed"]?.toInt() ?: 1)
            }
            assertTrue(creation, computeCreationProgress(asset, counts).getValue(idOf(creation)).unlocked)
        }
    }

    @Test
    fun `an area creation unlocks with one of each fiend from its area`() {
        val besaid = asset.filter { it.isCapturable && it.area == "Besaid" }
        val counts = besaid.associate { it.id to 1 }
        assertTrue(computeCreationProgress(asset, counts).getValue(idOf("Stratavis")).unlocked)
    }

    @Test
    fun `an area creation resolves an area named differently in its condition`() {
        // "One of each fiend from Djose Highroad" has to reach the "Djose" area column.
        val djose = asset.filter { it.isCapturable && it.area == "Djose" }
        val counts = djose.associate { it.id to 1 }
        val jormungand = computeCreationProgress(asset, counts).getValue(idOf("Jormungand"))

        assertTrue(jormungand.unlocked)
        assertEquals(djose.size, jormungand.required)
    }

    @Test
    fun `original creations keyed to every fiend step up with the count`() {
        val capturable = asset.filter { it.isCapturable }

        val atOne = computeCreationProgress(asset, capturable.associate { it.id to 1 })
        assertTrue(atOne.getValue(idOf("Neslug")).unlocked)          // 1 of every fiend
        assertFalse(atOne.getValue(idOf("Ultima Buster")).unlocked)  // needs 5
        assertFalse(atOne.getValue(idOf("Nemesis")).unlocked)        // needs 10

        val atTen = computeCreationProgress(asset, capturable.associate { it.id to 10 })
        assertTrue(atTen.getValue(idOf("Neslug")).unlocked)
        assertTrue(atTen.getValue(idOf("Ultima Buster")).unlocked)
        assertTrue(atTen.getValue(idOf("Nemesis")).unlocked)
    }

    @Test
    fun `shinryu unlocks at two of each underwater fiend`() {
        val underwater = feedersOf("Shinryu")
        assertEquals(3, underwater.size)

        val progress = computeCreationProgress(asset, underwater.associate { it.id to 2 })
            .getValue(idOf("Shinryu"))
        assertTrue(progress.unlocked)
        assertEquals(3, progress.required)
    }

    @Test
    fun `an original creation counting area conquests unlocks once enough areas are done`() {
        // Catching one of each fiend in two whole areas unlocks two Area Creations, which is what
        // Earth Eater is waiting on.
        val twoAreas = asset.filter {
            it.isCapturable && (it.area == "Besaid" || it.area == "Kilika")
        }
        val earthEater = computeCreationProgress(asset, twoAreas.associate { it.id to 1 })
            .getValue(idOf("Earth Eater"))

        assertTrue(earthEater.unlocked)
        assertEquals(2, earthEater.required)
        assertEquals(2, earthEater.current)
    }
}
