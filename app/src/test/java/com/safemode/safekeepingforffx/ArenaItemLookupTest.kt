package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.data.reference.itemNamesFoundInArena
import com.safemode.safekeepingforffx.data.reference.matchesSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The item list's tap sends you to the arena's search, so the two have to agree: a row is only
 * offered when that search would find something.
 */
class ArenaItemLookupTest {

    private val monsters by lazy {
        val asset = File("src/main/assets/monster_arena.csv")
        assertTrue("Missing asset: ${asset.absolutePath}", asset.exists())
        MonsterArenaCsvParser.parse(asset.readText())
    }

    private val itemNames by lazy {
        val asset = File("src/main/assets/item_list.csv")
        assertTrue("Missing asset: ${asset.absolutePath}", asset.exists())
        ItemListCsvParser.parse(asset.readText()).items.map { it.title }
    }

    private val dingo = Monster(
        id = "besaid_dingo",
        name = "Dingo",
        area = "Besaid",
        details = mapOf(
            "Common" to "Potion",
            "Rare" to "Sleeping Powder",
            "Bribe Item" to "Sleeping Powder (x4)",
            "Monster Type" to "Lupine"
        )
    )

    @Test
    fun `a search reaches the name, the area and every detail column`() {
        assertTrue(dingo.matchesSearch("Dingo"))
        assertTrue(dingo.matchesSearch("Besaid"))
        assertTrue(dingo.matchesSearch("Potion"))
        assertTrue(dingo.matchesSearch("Lupine"))
        assertFalse(dingo.matchesSearch("Elixir"))
    }

    @Test
    fun `searching ignores case`() {
        assertTrue(dingo.matchesSearch("sleeping powder"))
        assertTrue(dingo.matchesSearch("POTION"))
    }

    @Test
    fun `a name is only reported found when a fiend really carries it`() {
        val found = itemNamesFoundInArena(listOf("Potion", "Lupine", "Elixir"), listOf(dingo))
        assertEquals(setOf("Potion", "Lupine"), found)
    }

    @Test
    fun `blank names and an empty arena find nothing`() {
        assertEquals(emptySet<String>(), itemNamesFoundInArena(listOf("", "   "), listOf(dingo)))
        assertEquals(emptySet<String>(), itemNamesFoundInArena(listOf("Potion"), emptyList()))
    }

    @Test
    fun `a name cannot match by spanning two fields`() {
        // "Dingo" then "Besaid" are adjacent in the row but not one string, so a needle straddling
        // them must not match - otherwise the item list would offer a jump that finds nothing.
        assertFalse(dingo.matchesSearch("DingoBesaid"))
        assertEquals(
            emptySet<String>(),
            itemNamesFoundInArena(listOf("DingoBesaid"), listOf(dingo))
        )
    }

    @Test
    fun `every offered item finds fiends, and every withheld one would not`() {
        val found = itemNamesFoundInArena(itemNames, monsters)

        val offeredButEmpty = found.filter { name ->
            monsters.none { it.matchesSearch(name.trim()) }
        }
        assertEquals(emptyList<String>(), offeredButEmpty)

        val withheldButFindable = itemNames.filter { name ->
            name !in found && monsters.any { it.matchesSearch(name.trim()) }
        }
        assertEquals(emptyList<String>(), withheldButFindable)
    }

    @Test
    fun `the bundled assets line up`() {
        val found = itemNamesFoundInArena(itemNames, monsters)

        // Most of the item list is reachable from some fiend, so the tap is the rule, not a rarity.
        assertEquals(96, found.size)

        // Dropped, bribed and won items are all offered.
        assertTrue("Potion" in found)
        assertTrue("Sleeping Powder" in found)
        assertTrue("Power Sphere" in found)

        // Key items no fiend carries are not, which is the whole point of checking first.
        assertFalse("Flint" in found)
        assertFalse("Withered Bouquet" in found)
        assertFalse("Mark of Conquest" in found)
    }
}
