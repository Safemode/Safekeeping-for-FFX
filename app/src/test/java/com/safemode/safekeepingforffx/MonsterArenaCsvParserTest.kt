package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MonsterArenaCsvParserTest {

    private fun parse(csv: String) = MonsterArenaCsvParser.parse(csv)

    private val asset by lazy {
        val file = File("src/main/assets/monster_arena.csv")
        assertTrue("Missing asset: ${file.absolutePath}", file.exists())
        parse(file.readText())
    }

    @Test
    fun `groups monsters under their area in file order`() {
        val monsters = parse(
            """
            Area,Monster
            Besaid,Dingo
            Besaid,Condor
            Djose,Basilisk
            """.trimIndent()
        )

        assertEquals(listOf("Dingo", "Condor", "Basilisk"), monsters.map { it.name })
        // groupBy preserves insertion order, which is what keeps areas in story order.
        assertEquals(listOf("Besaid", "Djose"), monsters.groupBy { it.area }.keys.toList())
    }

    @Test
    fun `the same fiend in two areas is tracked separately`() {
        val monsters = parse(
            """
            Area,Monster
            Besaid,Water Flan
            Djose,Water Flan
            """.trimIndent()
        )

        assertEquals(2, monsters.size)
        assertEquals(2, monsters.map { it.id }.distinct().size)
    }

    @Test
    fun `ids stay unique even if a row is duplicated outright`() {
        val monsters = parse(
            """
            Area,Monster
            Besaid,Dingo
            Besaid,Dingo
            """.trimIndent()
        )

        assertEquals(2, monsters.map { it.id }.distinct().size)
    }

    @Test
    fun `blank and short rows are skipped`() {
        val monsters = parse(
            """
            Area,Monster
            Besaid,Dingo

            Besaid
            ,Nameless
            Djose,Basilisk
            """.trimIndent()
        )

        assertEquals(listOf("Dingo", "Basilisk"), monsters.map { it.name })
    }

    @Test
    fun `the bundled asset parses`() {
        assertEquals(137, asset.size)
        assertEquals(16, asset.map { it.area }.distinct().size)

        // 102 wild fiends plus 35 arena creations, which are unlocked rather than captured.
        assertEquals(102, asset.count { !it.area.endsWith("Creations") })
        assertEquals(35, asset.count { it.area.endsWith("Creations") })
    }

    @Test
    fun `asset ids and names are unique`() {
        assertEquals(asset.size, asset.map { it.id }.distinct().size)
        // The Omega Ruins Demonolith was renamed, so no name repeats across the file any more.
        assertEquals(asset.size, asset.map { it.name }.distinct().size)
    }

    @Test
    fun `detail columns are read from the header`() {
        val monsters = parse(
            """
            Area,Monster,Gil Cost,Common
            Besaid,Dingo,24,Potion
            """.trimIndent()
        )

        assertEquals(
            mapOf("Gil Cost" to "24", "Common" to "Potion"),
            monsters.single().details
        )
    }

    @Test
    fun `blank detail cells are dropped rather than shown empty`() {
        val monsters = parse(
            """
            Area,Monster,Gil Cost,Common,Rare
            Djose,Garm,132,,
            """.trimIndent()
        )

        assertEquals(mapOf("Gil Cost" to "132"), monsters.single().details)
    }

    @Test
    fun `quoted detail values keep their commas`() {
        val monsters = parse(
            """
            Area,Monster,Gil Cost,Amount
            Kilika,Ragora,72,"15,600"
            """.trimIndent()
        )

        assertEquals("15,600", monsters.single().details["Amount"])
    }

    @Test
    fun `asset details carry through`() {
        val dingo = asset.first { it.name == "Dingo" }
        assertEquals("24", dingo.details["Gil Cost"])
        assertEquals("Sleeping Powder", dingo.details["Rare"])
        assertEquals("Sleeping Powder (x4)", dingo.details["Bribe Item"])

        // Detail order follows the header, which is what the expanded row renders.
        assertEquals(
            listOf("Gil Cost", "Common", "Rare", "Win", "Bribe Amount", "Bribe Item"),
            dingo.details.keys.toList()
        )

        // Creations have drops but cannot be bribed, which the data says in words rather than by
        // leaving the column blank.
        val stratavis = asset.first { it.name == "Stratavis" }
        assertEquals(
            mapOf(
                "Gil Cost" to "6000",
                "Common" to "Smoke Bomb (x3)",
                "Rare" to "Stamina Tablet (x2)",
                "Win" to "Amulet (x4)",
                "Bribe Amount" to "Cannot Bribe",
                "Bribe Item" to "N/A"
            ),
            stratavis.details
        )
    }

    @Test
    fun `every fiend has a full set of details`() {
        // The table is complete, so nothing should be falling back to a partial row.
        val incomplete = asset.filter { it.details.size < 6 }
        assertEquals(emptyList<String>(), incomplete.map { "${it.name} (${it.details.size})" })
    }

    @Test
    fun `asset areas are in story order`() {
        // File order is display order, so this is the sequence the player walks through.
        assertEquals(
            listOf(
                "Besaid",
                "Kilika",
                "Mi'ihen Highroad",
                "Mushroom Rock Road",
                "Djose",
                "Thunder Plains",
                "Macalania Woods",
                "Bikanel",
                "The Calm Lands",
                "Cavern of the Stolen Fayth",
                "Mt. Gagazet",
                "Inside Sin",
                "Omega Ruins",
                "Area Creations",
                "Species Creations",
                "Original Creations"
            ),
            asset.map { it.area }.distinct()
        )
    }
}
