package com.safemode.safekeepingforffx.data.reference

/** How many of each fiend the Monster Arena owner wants before an area counts as complete. */
const val MAX_CAPTURES = 10

const val MONSTER_ARENA_ID = "monster_arena"
const val MONSTER_ARENA_LABEL = "Monster Arena"

/**
 * One capturable fiend, belonging to the area it is caught in.
 *
 * [details] holds every column after Area and Monster, keyed by its header and in header order.
 * Anything blank for a given fiend is left out entirely, so a row that only has a Gil cost carries
 * exactly one detail. Parsing them generically means new columns appear in the app without a code
 * change.
 */
data class Monster(
    val id: String,
    val name: String,
    val area: String,
    val details: Map<String, String> = emptyMap()
) {
    /**
     * Arena creations are unlocked by capturing fiends, not captured themselves, so they are shown
     * and countable but left out of capture totals - otherwise the progress bar could never fill.
     * They live in the "Area / Species / Original Creations" groups.
     */
    val isCapturable: Boolean get() = !area.endsWith(CREATIONS_SUFFIX, ignoreCase = true)
}

private const val CREATIONS_SUFFIX = "Creations"

/** Area and Monster are fixed; everything after them is a detail column. */
private const val FIXED_COLUMNS = 2

/**
 * Turns the bundled fiend CSV into the capture list, preserving file order so areas appear in the
 * order the player reaches them rather than alphabetically.
 */
object MonsterArenaCsvParser {

    fun parse(text: String): List<Monster> {
        val rows = CsvReader.parse(text)
        if (rows.isEmpty()) return emptyList()

        val detailLabels = rows.first().drop(FIXED_COLUMNS).map { it.trim() }
        val monsters = mutableListOf<Monster>()
        val seen = mutableSetOf<String>()

        rows.forEachIndexed { index, row ->
            // Row 0 is the header.
            if (index == 0 || CsvReader.isBlank(row)) return@forEachIndexed
            if (row.size < FIXED_COLUMNS) return@forEachIndexed

            val area = row[0].trim()
            val name = row[1].trim()
            if (area.isEmpty() || name.isEmpty()) return@forEachIndexed

            val details = LinkedHashMap<String, String>()
            detailLabels.forEachIndexed { column, label ->
                val value = row.getOrNull(column + FIXED_COLUMNS)?.trim().orEmpty()
                if (label.isNotEmpty() && value.isNotEmpty()) details[label] = value
            }

            // Area-qualified, because the same fiend can appear in more than one area and each is
            // captured separately.
            var id = MixTable.slug(area) + "_" + MixTable.slug(name)
            if (!seen.add(id)) {
                id = "${id}_$index"
                seen.add(id)
            }

            monsters += Monster(id = id, name = name, area = area, details = details)
        }

        return monsters
    }
}
