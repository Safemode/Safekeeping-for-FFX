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

/**
 * True if [needle] appears anywhere the fiend carries text - its name, its area, or any detail
 * column, which is what lets a search reach a type, a dropped, stolen or bribed item, or a
 * creation's unlock and reward. Blank detail cells were dropped at parse time, so this only ever
 * matches real data.
 *
 * Lives here rather than in the arena screen because the item list asks the same question before
 * offering its jump: a row only becomes tappable when this would find something.
 */
fun Monster.matchesSearch(needle: String): Boolean =
    name.contains(needle, ignoreCase = true) ||
        area.contains(needle, ignoreCase = true) ||
        details.values.any { it.contains(needle, ignoreCase = true) }

/** The columns that name something the fiend actually hands over, in the order the CSV lists them. */
private val ITEM_COLUMNS = listOf(
    MonsterColumns.COMMON,
    MonsterColumns.RARE,
    MonsterColumns.WIN,
    MonsterColumns.BRIBE_ITEM,
    MonsterColumns.UNLOCK_REWARD
)

/**
 * The count the CSV writes after an item, as in "Power Sphere (x2)". Matched wherever it appears
 * rather than stripped off the end, because a single cell can name two items back to back -
 * "Power Sphere (x2) Al Bhed Potion (x2)" - and the count is the only thing separating them. The
 * optional space covers the one cell written "Smoke Bomb(x99)".
 */
private val ITEM_COUNT = Regex("""\s*\(x\d+\)""")

/**
 * The items named in one cell, without their counts. A cell with no count at all is a single item,
 * which is how most of them are written.
 */
private fun itemsInCell(cell: String): List<String> {
    val text = cell.trim()
    if (text.isEmpty()) return emptyList()

    val counts = ITEM_COUNT.findAll(text).toList()
    if (counts.isEmpty()) return listOf(text)

    // Each count closes the item named in front of it; anything after the last one is an item that
    // was written without a count.
    val items = mutableListOf<String>()
    var start = 0
    counts.forEach { count ->
        text.substring(start, count.range.first).trim().takeIf { it.isNotEmpty() }?.let { items += it }
        start = count.range.last + 1
    }
    text.substring(start).trim().takeIf { it.isNotEmpty() }?.let { items += it }
    return items
}

/**
 * True if this fiend hands [item] over - stolen, dropped, bribed for, or paid out when a creation
 * unlocks.
 *
 * Whole items only, unlike [matchesSearch]: a fiend that carries nothing but Hi-Potions is not an
 * answer to "where do I get Potions?", though Dual Horn, whose common steal really is a Potion,
 * still is.
 */
fun Monster.carriesItem(item: String): Boolean {
    val wanted = item.trim()
    if (wanted.isEmpty()) return false
    return ITEM_COLUMNS.any { column ->
        details[column]?.let { cell ->
            itemsInCell(cell).any { it.equals(wanted, ignoreCase = true) }
        } == true
    }
}

/**
 * Which of [names] some fiend in [monsters] carries, by the same whole-item test [carriesItem] uses.
 * Answering all of them at once costs one pass over the fiend list rather than one per name, which
 * matters when the item list asks about a hundred names before it can draw a row.
 */
fun itemNamesCarriedByFiends(names: Collection<String>, monsters: List<Monster>): Set<String> {
    if (names.isEmpty() || monsters.isEmpty()) return emptySet()
    val carried = monsters.flatMapTo(mutableSetOf()) { monster ->
        ITEM_COLUMNS.mapNotNull { monster.details[it] }
            .flatMap { itemsInCell(it) }
            .map { it.lowercase() }
    }
    return names.filterTo(mutableSetOf()) { it.trim().lowercase() in carried }
}

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
