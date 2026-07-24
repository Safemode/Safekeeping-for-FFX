package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.data.reference.carriesItem
import com.safemode.safekeepingforffx.data.reference.itemNamesCarriedByFiends
import com.safemode.safekeepingforffx.data.reference.matchesSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The item list's tap narrows the arena to one item, so the two have to agree: a row is only
 * offered when at least one fiend really carries that item, matched whole rather than by substring.
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
            "Win" to "Power Sphere (x2)",
            "Bribe Item" to "Sleeping Powder (x4)",
            "Monster Type" to "Lupine"
        )
    )

    @Test
    fun `a fiend carries what it steals, drops and is bribed for`() {
        assertTrue(dingo.carriesItem("Potion"))
        assertTrue(dingo.carriesItem("Sleeping Powder"))
        assertTrue(dingo.carriesItem("Power Sphere"))
        assertTrue(dingo.carriesItem("potion"))
    }

    @Test
    fun `carrying an item is a whole-item test, not a substring one`() {
        // The bug this exists to stop: a Potion row listing every Hi-Potion fiend.
        assertFalse(dingo.carriesItem("Hi-Potion"))
        assertFalse(dingo.carriesItem("Tion"))
        assertFalse(dingo.carriesItem("Sphere"))

        // A fiend whose only potion is a Hi-Potion is not an answer for Potion, and vice versa.
        val hiOnly = dingo.copy(details = mapOf("Common" to "Hi-Potion (x2)"))
        assertFalse(hiOnly.carriesItem("Potion"))
        assertTrue(hiOnly.carriesItem("Hi-Potion"))
    }

    @Test
    fun `columns that are not items never match`() {
        // Type, area and name are searchable but they are not things the fiend hands over.
        assertFalse(dingo.carriesItem("Lupine"))
        assertFalse(dingo.carriesItem("Besaid"))
        assertFalse(dingo.carriesItem("Dingo"))
    }

    @Test
    fun `a creation's unlock reward counts as carried`() {
        val jormungand = Monster(
            id = "area_creations_jormungand",
            name = "Jormungand",
            area = "Area Creations",
            details = mapOf(
                "Unlock Condition" to "One of each fiend from Djose Highroad",
                "Unlock Reward" to "Petrify Grenade (x99)"
            )
        )

        assertTrue(jormungand.carriesItem("Petrify Grenade"))
        // The condition is a sentence about fiends, not a payout.
        assertFalse(jormungand.carriesItem("Djose Highroad"))
    }

    @Test
    fun `a cell naming two items counts for both`() {
        // Three cells in the asset are written this way, the count being all that separates them.
        val ghost = dingo.copy(details = mapOf("Win" to "Power Sphere (x2) Al Bhed Potion (x3)"))

        assertTrue(ghost.carriesItem("Power Sphere"))
        assertTrue(ghost.carriesItem("Al Bhed Potion"))
        assertFalse(ghost.carriesItem("Potion"))
    }

    @Test
    fun `a count written without its space still separates cleanly`() {
        // The asset is written with the space throughout, so this guards the next cell typed
        // without one rather than anything in the data today.
        val garuda = dingo.copy(details = mapOf("Bribe Item" to "Smoke Bomb(x99)"))
        assertTrue(garuda.carriesItem("Smoke Bomb"))
    }

    @Test
    fun `a name is only reported carried when a fiend really carries it`() {
        val carried = itemNamesCarriedByFiends(
            listOf("Potion", "Hi-Potion", "Lupine", "Elixir"),
            listOf(dingo)
        )
        assertEquals(setOf("Potion"), carried)
    }

    @Test
    fun `blank names and an empty arena carry nothing`() {
        assertEquals(emptySet<String>(), itemNamesCarriedByFiends(listOf("", "   "), listOf(dingo)))
        assertEquals(emptySet<String>(), itemNamesCarriedByFiends(listOf("Potion"), emptyList()))
    }

    @Test
    fun `the free-text search stays broader than the item filter`() {
        // Typing in the arena still reaches types and areas, which the item jump deliberately
        // does not.
        assertTrue(dingo.matchesSearch("Lupine"))
        assertTrue(dingo.matchesSearch("Besaid"))
        assertFalse(dingo.carriesItem("Lupine"))
    }

    @Test
    fun `every offered item finds fiends, and every withheld one would find none`() {
        val carried = itemNamesCarriedByFiends(itemNames, monsters)

        val offeredButEmpty = carried.filter { name -> monsters.none { it.carriesItem(name) } }
        assertEquals(emptyList<String>(), offeredButEmpty)

        val withheldButCarried = itemNames.filter { name ->
            name !in carried && monsters.any { it.carriesItem(name) }
        }
        assertEquals(emptyList<String>(), withheldButCarried)
    }

    @Test
    fun `the bundled assets line up`() {
        val carried = itemNamesCarriedByFiends(itemNames, monsters)

        // Stolen, dropped, bribed and rewarded items are all offered.
        assertTrue("Potion" in carried)
        assertTrue("Sleeping Powder" in carried)
        assertTrue("Power Sphere" in carried)
        assertTrue("Al Bhed Potion" in carried)

        // Key items no fiend hands over are not, which is the point of checking first.
        assertFalse("Flint" in carried)
        assertFalse("Withered Bouquet" in carried)
        assertFalse("Mark of Conquest" in carried)
    }

    @Test
    fun `tapping Potion lists the Potion fiends and no others`() {
        // The case that named this rule. Dual Horn's common steal really is a Potion, so it stays;
        // Garm carries only Hi-Potions and Xiphos only Mega-Potions, so they go.
        assertEquals(
            listOf("Dingo", "Mi'ihen Fang", "Raldo", "Voivre", "Dual Horn", "Lamashtu"),
            monsters.filter { it.carriesItem("Potion") }.map { it.name }
        )

        val garm = monsters.first { it.name == "Garm" }
        assertTrue(garm.carriesItem("Hi-Potion"))
        assertFalse(garm.carriesItem("Potion"))

        // Al Bhed Potion is its own item too, not a Potion and not a fallback for one.
        val sandWolf = monsters.first { it.name == "Sand Wolf" }
        assertTrue(sandWolf.carriesItem("Al Bhed Potion"))
        assertFalse(sandWolf.carriesItem("Potion"))
    }
}
