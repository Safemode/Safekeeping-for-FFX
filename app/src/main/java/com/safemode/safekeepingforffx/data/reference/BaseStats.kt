package com.safemode.safekeepingforffx.data.reference

const val BASE_STATS_ASSET = "base_stats.csv"

/**
 * A character's stats before a single Sphere Grid node is activated - the numbers they join the
 * party with. The Sphere Grid only ever adds to these, so they are the floor every status readout
 * starts from.
 */
data class BaseStats(val character: GridCharacter, val values: Map<NodeType, Int>) {
    fun valueOf(attribute: NodeType): Int = values[attribute] ?: 0
}

/**
 * Turns the bundled base stats CSV into per-character starting values.
 *
 * Columns are matched by *name* rather than position, so the file can be reordered or regenerated
 * without touching this parser. Matching ignores case, spaces and underscores, which lets a header
 * read "MagicDefense" or "Magic Defense" and still land on [NodeType.MAGIC_DEFENSE]. Unknown columns
 * and unknown character names are skipped rather than throwing - a stray row should not take the
 * planner down.
 */
object BaseStatsCsvParser {

    fun parse(text: String): Map<GridCharacter, BaseStats> {
        val rows = CsvReader.parse(text).filterNot { CsvReader.isBlank(it) }
        val header = rows.firstOrNull() ?: return emptyMap()

        // Column index -> the attribute it holds. The name column has no attribute and is skipped.
        val columns = header.mapIndexed { index, cell -> index to attributeFor(cell) }
            .mapNotNull { (index, attribute) -> attribute?.let { index to it } }
            .toMap()
        if (columns.isEmpty()) return emptyMap()

        val nameColumn = header.indexOfFirst { normalize(it) == "character" }.takeIf { it >= 0 } ?: 0

        return rows.drop(1).mapNotNull { row ->
            val name = row.getOrNull(nameColumn)?.trim().orEmpty()
            val character = GridCharacter.entries
                .firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                ?: return@mapNotNull null

            val values = columns.mapNotNull { (index, attribute) ->
                row.getOrNull(index)?.trim()?.toIntOrNull()?.let { attribute to it }
            }.toMap()

            character to BaseStats(character, values)
        }.toMap()
    }

    private fun attributeFor(header: String): NodeType? {
        val key = normalize(header)
        return NodeType.attributes.firstOrNull { normalize(it.name) == key }
    }

    private fun normalize(value: String): String =
        value.filter { it.isLetterOrDigit() }.lowercase()
}
