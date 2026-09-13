package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.data.reference.Trophies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionItemTest {

    private fun item(id: String) = ReferenceItem(id = id, title = id, location = "", detail = "")

    private val category = ChecklistCategory(
        id = "cat",
        label = "Cat",
        items = listOf(item("platinum"), item("a"), item("b"), item("c")),
        completionItemId = "platinum"
    )

    @Test
    fun `ticking the last other item ticks the completion item`() {
        assertEquals(true, category.completionUpdate(setOf("a", "b", "c"), changedItemId = "c"))
    }

    @Test
    fun `nothing happens while another item is still unticked`() {
        assertNull(category.completionUpdate(setOf("a", "b"), changedItemId = "b"))
    }

    @Test
    fun `unticking another item unticks the completion item`() {
        assertEquals(false, category.completionUpdate(setOf("platinum", "a", "b"), changedItemId = "c"))
    }

    @Test
    fun `ticking another item never clears a completion item ticked by hand`() {
        assertNull(category.completionUpdate(setOf("platinum", "a"), changedItemId = "a"))
    }

    @Test
    fun `the completion item itself is left to the player`() {
        assertNull(category.completionUpdate(setOf("a", "b", "c"), changedItemId = "platinum"))
        assertNull(category.completionUpdate(setOf("platinum"), changedItemId = "platinum"))
    }

    @Test
    fun `already ticked completion item is left alone`() {
        assertNull(category.completionUpdate(setOf("platinum", "a", "b", "c"), changedItemId = "c"))
    }

    @Test
    fun `lists without a completion item are unaffected`() {
        val plain = category.copy(completionItemId = null)
        assertNull(plain.completionUpdate(setOf("a", "b", "c"), changedItemId = "c"))
    }

    @Test
    fun `trophies complete on the platinum trophy`() {
        val completionId = Trophies.category.completionItemId
        assertEquals("trophy_completion", completionId)
        assertTrue(Trophies.items.any { it.id == completionId })

        val allOthers = Trophies.items.map { it.id }.filter { it != completionId }.toSet()
        assertEquals(true, Trophies.category.completionUpdate(allOthers, allOthers.last()))
    }
}
