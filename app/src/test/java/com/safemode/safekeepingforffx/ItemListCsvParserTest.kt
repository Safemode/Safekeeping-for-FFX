package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the real bundled asset, not a fixture. */
class ItemListCsvParserTest {

    private val category by lazy {
        val asset = File("src/main/assets/item_list.csv")
        assertTrue("Missing asset: ${asset.absolutePath}", asset.exists())
        ItemListCsvParser.parse(asset.readText())
    }

    @Test
    fun `is a reference list rather than a checklist`() {
        assertFalse(category.trackProgress)
    }

    @Test
    fun `every row becomes an item`() {
        // 118 data rows in the source, minus the header and the trailing blank line.
        assertEquals(118, category.items.size)
    }

    @Test
    fun `ids are unique`() {
        val ids = category.items.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `type becomes the tag`() {
        val potion = category.items.first { it.title == "Potion" }
        assertEquals("Restorative", potion.tag)
        assertEquals("Restores 200 HP of one character.", potion.detail)

        // Every row in this CSV has a type, so none should be untagged.
        assertEquals(emptyList<String>(), category.items.filter { it.tag == null }.map { it.title })
    }

    @Test
    fun `quoted descriptions containing commas survive`() {
        val powerSphere = category.items.first { it.title == "Power Sphere" }
        assertEquals(
            "Activates Strength, Defense, or HP nodes on Sphere Grid.",
            powerSphere.detail
        )
    }

    @Test
    fun `non-ascii punctuation decodes correctly`() {
        // The asset was Windows-1252; this fails with a replacement character if it regresses.
        val elixir = category.items.first { it.title == "Elixir" }
        assertEquals("Fully restores one character’s HP & MP.", elixir.detail)
        assertFalse(category.items.any { it.detail.contains('�') })
    }

    @Test
    fun `rows carry no location`() {
        // Items are descriptions, not collectibles, so the row skips that line entirely.
        assertTrue(category.items.all { it.location.isBlank() })
    }
}
