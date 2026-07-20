package com.safemode.safekeepingforffx.data.reference

/**
 * One item Rikku can Mix.
 *
 * Unlike checklist item ids, [id] is not a storage contract - nothing about Mix is persisted - so
 * it is derived from the name rather than hand-assigned.
 */
data class MixIngredient(val id: String, val name: String)

/** One possible outcome of a Mix, with the effect text shown to the player. */
data class MixResult(val id: String, val name: String, val description: String)

/**
 * A group of combinations sharing one item: [anchor] paired with any of [partners] gives the same
 * result. Collapses "Potion + Ability Distiller", "Potion + Ability Sphere" and so on into a
 * single readable line.
 */
data class MixCombination(val anchor: MixIngredient, val partners: List<MixIngredient>)

/**
 * What the import found that the caller should know about. Kept rather than logged and forgotten:
 * a silent drop in a twelve-thousand-row import is how a table ends up subtly wrong.
 */
data class MixTableReport(
    val dataRows: Int,
    val ingredientCount: Int,
    val uniquePairs: Int,
    /** 1-based row numbers that did not have the expected four columns. */
    val malformedRows: List<Int>,
    /** Pairs where two rows disagreed on the result. First row seen wins. */
    val conflictingPairs: List<String>
) {
    /** Every unordered pair of n items, self-pairs included, is n(n+1)/2. */
    val expectedPairs: Int get() = ingredientCount * (ingredientCount + 1) / 2

    val missingPairs: Int get() = (expectedPairs - uniquePairs).coerceAtLeast(0)

    val isClean: Boolean
        get() = malformedRows.isEmpty() && conflictingPairs.isEmpty() && missingPairs == 0
}

/**
 * The imported Mix table. Lookup is order-insensitive because the key is the pair sorted, which is
 * also what collapses the A+B / B+A duplicates in the source data.
 */
class MixTable(
    val ingredients: List<MixIngredient>,
    val results: List<MixResult>,
    private val byPair: Map<String, MixResult>,
    private val pairsByResult: Map<String, List<Pair<String, String>>>,
    val report: MixTableReport
) {
    private val ingredientsById: Map<String, MixIngredient> = ingredients.associateBy { it.id }

    private val combinationCache = HashMap<String, List<MixCombination>>()

    /** Null when this pair has no row in the source data. Never guesses. */
    fun resultFor(a: String, b: String): MixResult? = byPair[pairKey(a, b)]

    /** Every pair that produces [resultId], grouped so a shared item is named once. */
    fun combinationsFor(resultId: String): List<MixCombination> =
        combinationCache.getOrPut(resultId) {
            consolidate(pairsByResult[resultId].orEmpty())
        }

    /**
     * Greedy cover: repeatedly take the item involved in the most not-yet-listed pairs and emit it
     * with all of its remaining partners. Every pair appears exactly once, under whichever item
     * groups the most of them, which is what makes the output short enough to read.
     */
    private fun consolidate(pairs: List<Pair<String, String>>): List<MixCombination> {
        val remaining = pairs.toMutableSet()
        val combinations = mutableListOf<MixCombination>()

        while (remaining.isNotEmpty()) {
            val incidence = HashMap<String, MutableSet<String>>()
            remaining.forEach { (a, b) ->
                incidence.getOrPut(a) { mutableSetOf() }.add(b)
                // A self-pair must not count its item twice.
                if (a != b) incidence.getOrPut(b) { mutableSetOf() }.add(a)
            }

            // Sorted rather than plain max, so equal-sized groups come out in a stable order.
            val (anchorId, partnerIds) = incidence.entries
                .sortedWith(compareByDescending<Map.Entry<String, MutableSet<String>>> {
                    it.value.size
                }.thenBy { it.key })
                .first()

            partnerIds.forEach { remaining.remove(normalise(anchorId, it)) }

            val anchor = ingredientsById[anchorId] ?: continue
            val partners = partnerIds
                .mapNotNull { ingredientsById[it] }
                .sortedBy { it.name }
            combinations += MixCombination(anchor, partners)
        }

        return combinations.sortedWith(
            compareByDescending<MixCombination> { it.partners.size }.thenBy { it.anchor.name }
        )
    }

    companion object {
        val EMPTY = MixTable(
            ingredients = emptyList(),
            results = emptyList(),
            byPair = emptyMap(),
            pairsByResult = emptyMap(),
            report = MixTableReport(0, 0, 0, emptyList(), emptyList())
        )

        fun pairKey(a: String, b: String): String = if (a <= b) "$a+$b" else "$b+$a"

        /** The pair with its two ids in a fixed order, so A+B and B+A are one entry. */
        fun normalise(a: String, b: String): Pair<String, String> =
            if (a <= b) a to b else b to a

        /**
         * Ids come from the display name, which also does the de-duplicating: punctuation and
         * spacing differences collapse, so "Lv. 1 Key Sphere" and "Lv.1 Key Sphere" are one item.
         */
        fun slug(name: String): String = buildString {
            var pendingSeparator = false
            name.trim().lowercase().forEach { char ->
                when {
                    char.isLetterOrDigit() -> {
                        if (pendingSeparator && isNotEmpty()) append('_')
                        pendingSeparator = false
                        append(char)
                    }

                    // Apostrophes are dropped, not treated as a break, so "Underdog's Secret" and
                    // "Underdogs Secret" are the same item rather than two.
                    char == '\'' || char == '’' -> Unit

                    else -> pendingSeparator = true
                }
            }
        }
    }
}
