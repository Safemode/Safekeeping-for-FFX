package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.MixCsvParser
import com.safemode.safekeepingforffx.data.reference.MixIngredient
import com.safemode.safekeepingforffx.data.reference.MixTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the real bundled asset, not a fixture. An import this size can only go wrong quietly, so
 * the checks are about coverage and contradictions rather than spot values.
 */
class MixCsvParserTest {


    private val table by lazy {
        val asset = File("src/main/assets/mix_recipes.csv")
        assertTrue("Missing asset: ${asset.absolutePath}", asset.exists())
        MixCsvParser.parse(asset.readText())
    }

    @Test
    fun `every row parses`() {
        assertEquals(emptyList<Int>(), table.report.malformedRows)
    }

    @Test
    fun `no pair resolves to two different results`() {
        assertEquals(emptyList<String>(), table.report.conflictingPairs)
    }

    @Test
    fun `every pair of every ingredient is covered`() {
        val ingredients = table.ingredients
        val missing = ingredients.flatMapIndexed { index: Int, first: MixIngredient ->
            ingredients.drop(index)
                .filter { table.resultFor(first.id, it.id) == null }
                .map { "${first.name} + ${it.name}" }
        }

        assertEquals("Pairs with no row in the CSV: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `lookup is order insensitive`() {
        val ingredients = table.ingredients
        assertTrue("No ingredients parsed", ingredients.size > 1)

        // Sweep the whole table rather than one lucky pair.
        ingredients.forEach { first ->
            ingredients.forEach { second ->
                val forward = table.resultFor(first.id, second.id)
                val backward = table.resultFor(second.id, first.id)
                assertEquals("${first.id} + ${second.id}", forward?.id, backward?.id)
            }
        }
    }

    @Test
    fun `descriptions survive the commas inside quoted fields`() {
        // Any result whose description contains a comma proves the quoted-field handling works.
        val withComma = table.ingredients
            .flatMap { first -> table.ingredients.mapNotNull { table.resultFor(first.id, it.id) } }
            .firstOrNull { it.description.contains(",") }

        assertNotNull("No description contained a comma - check the CSV quoting", withComma)
        assertTrue(withComma!!.description.isNotBlank())
    }

    @Test
    fun `every result has at least one combination`() {
        val empty = table.results.filter { table.combinationsFor(it.id).isEmpty() }
        assertEquals(emptyList<String>(), empty.map { it.name })
    }

    @Test
    fun `consolidation covers every pair exactly once`() {
        table.results.forEach { result ->
            val listed = table.combinationsFor(result.id).flatMap { combination ->
                combination.partners.map { MixTable.pairKey(combination.anchor.id, it.id) }
            }

            // No pair may appear under two anchors, or the same pair would be shown twice.
            assertEquals("Duplicates for ${result.name}", listed.size, listed.distinct().size)

            // And the set must be exactly the pairs that really produce this result.
            val expected = table.ingredients.flatMap { first ->
                table.ingredients.mapNotNull { second ->
                    MixTable.pairKey(first.id, second.id)
                        .takeIf { table.resultFor(first.id, second.id)?.id == result.id }
                }
            }.distinct()

            assertEquals("Coverage for ${result.name}", expected.sorted(), listed.sorted())
        }
    }

    @Test
    fun `consolidation groups rather than listing every pair separately`() {
        // Ultra Potion is the example case: one anchor should absorb many partners.
        val ultraPotion = table.results.first { it.name == "Ultra Potion" }
        val combinations = table.combinationsFor(ultraPotion.id)

        assertTrue("Expected grouping, got ${combinations.size} groups", combinations.isNotEmpty())
        assertTrue(
            "Largest group had only ${combinations.first().partners.size} partners",
            combinations.first().partners.size > 1
        )
    }

    @Test
    fun `slug collapses punctuation and spacing variants`() {
        assertEquals(MixTable.slug("Lv. 1 Key Sphere"), MixTable.slug("Lv.1 Key Sphere"))
        assertEquals(MixTable.slug("Hi-Potion"), MixTable.slug("hi potion"))
        assertEquals("underdogs_secret", MixTable.slug("Underdog's Secret"))
    }
}
