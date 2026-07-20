package com.safemode.safekeepingforffx.data.reference

/** Columns: Item Name, Type, Description. */
private const val COLUMN_COUNT = 3

/**
 * Turns the bundled item CSV into a [ChecklistCategory].
 *
 * It is a plain reference list - `trackProgress = false` - so it has no checkboxes, no progress
 * bar and no reset, and stays off the Home screen summary. Type becomes the row's tag, matching
 * how Weapon and Armor Abilities labels its entries.
 */
object ItemListCsvParser {

    const val CATEGORY_ID = "item_list"
    const val LABEL = "Item List"

    fun parse(text: String): ChecklistCategory {
        val items = mutableListOf<ReferenceItem>()
        val seen = mutableSetOf<String>()

        CsvReader.parse(text).forEachIndexed { index, row ->
            // Row 0 is the header.
            if (index == 0 || CsvReader.isBlank(row)) return@forEachIndexed
            if (row.size < COLUMN_COUNT) return@forEachIndexed

            val name = row[0].trim()
            val type = row[1].trim()
            val description = row[2].trim()
            if (name.isEmpty()) return@forEachIndexed

            // Two rows sharing a name would collide as list keys and crash the LazyColumn.
            var id = "item_" + MixTable.slug(name)
            if (!seen.add(id)) {
                id = "${id}_$index"
                seen.add(id)
            }

            items += ReferenceItem(
                id = id,
                title = name,
                // Nothing to locate: these are descriptions, not collectibles.
                location = "",
                detail = description,
                tag = type.ifEmpty { null }
            )
        }

        return ChecklistCategory(
            id = CATEGORY_ID,
            label = LABEL,
            items = items,
            trackProgress = false
        )
    }
}
