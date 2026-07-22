package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.data.reference.computeCreationProgress
import com.safemode.safekeepingforffx.data.reference.creationCaptureTargets
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
    fun `a species name spelled differently from its feeders still matches`() {
        // Iron Giant fiends feed "Iron Clad" while the creation row is named "Ironclad", so the
        // match only works because punctuation and spacing are folded away.
        val creation = asset.first { it.name == "Ironclad" }
        val feeders = asset.filter { it.isCapturable && it.details["Monster Type"] == "Iron Giant" }
        assertTrue(feeders.isNotEmpty())
        assertTrue(feeders.all { it.details["Creation Unlock"] == "Iron Clad" })

        val counts = feeders.associate { it.id to it.details["Creation Unlock Amount Needed"]!!.toInt() }
        assertTrue(computeCreationProgress(asset, counts).getValue(creation.id).unlocked)
    }

    @Test
    fun `species auto-capture targets each feeder at its required amount`() {
        // The worked example: Nega Elemental wants every Element fiend, three each.
        val nega = asset.first { it.name == "Nega Elemental" }
        val elements = asset.filter { it.isCapturable && it.details["Monster Type"] == "Element" }
        val expected = elements.associate {
            it.id to it.details["Creation Unlock Amount Needed"]!!.toInt()
        }

        val targets = creationCaptureTargets(nega, asset)
        assertEquals(expected, targets)
        assertTrue(computeCreationProgress(asset, targets).getValue(nega.id).unlocked)
    }

    @Test
    fun `area auto-capture targets one of each fiend in the area`() {
        val stratavis = asset.first { it.name == "Stratavis" }
        val besaid = asset.filter { it.isCapturable && it.area == "Besaid" }

        val targets = creationCaptureTargets(stratavis, asset)
        assertEquals(besaid.map { it.id }.toSet(), targets.keys)
        assertTrue(targets.values.all { it == 1 })
        assertTrue(computeCreationProgress(asset, targets).getValue(stratavis.id).unlocked)
    }

    @Test
    fun `original every-fiend auto-capture targets the whole capturable list`() {
        val nemesis = asset.first { it.name == "Nemesis" }
        val capturable = asset.filter { it.isCapturable }

        val targets = creationCaptureTargets(nemesis, asset)
        assertEquals(capturable.map { it.id }.toSet(), targets.keys)
        assertTrue(targets.values.all { it == 10 })
        assertTrue(computeCreationProgress(asset, targets).getValue(nemesis.id).unlocked)
    }

    @Test
    fun `underwater auto-capture targets only the underwater fiends`() {
        val shinryu = asset.first { it.name == "Shinryu" }
        val underwater = feedersOf("Shinryu")

        val targets = creationCaptureTargets(shinryu, asset)
        assertEquals(underwater.map { it.id }.toSet(), targets.keys)
        assertTrue(targets.values.all { it == 2 })
        assertTrue(computeCreationProgress(asset, targets).getValue(shinryu.id).unlocked)
    }

    @Test
    fun `conquest-gated originals offer no auto-capture`() {
        // These four turn on other creations being conquered, not on captures, so they are excluded.
        for (name in listOf("Earth Eater", "Greater Sphere", "Catastrophe", "Th'uban")) {
            val creation = asset.first { it.name == name }
            assertTrue(name, creationCaptureTargets(creation, asset).isEmpty())
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
