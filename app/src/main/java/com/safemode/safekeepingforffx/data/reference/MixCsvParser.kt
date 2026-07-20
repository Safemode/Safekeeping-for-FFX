package com.safemode.safekeepingforffx.data.reference

/** Columns: Item 1, Item 2, Result, Result Description. */
private const val COLUMN_COUNT = 4

/**
 * Turns the bundled Mix CSV into a [MixTable].
 *
 * Split out from the repository so it can be exercised without an emulator - a twelve-thousand-row
 * import is exactly the kind of thing that should be checked by a test rather than by eye.
 */
object MixCsvParser {

    fun parse(text: String): MixTable {
        val ingredients = LinkedHashMap<String, MixIngredient>()
        val results = HashMap<String, MixResult>()
        val byPair = HashMap<String, MixResult>()
        val pairsByResult = HashMap<String, MutableList<Pair<String, String>>>()
        val malformedRows = mutableListOf<Int>()
        val conflictingPairs = mutableListOf<String>()
        var dataRows = 0

        CsvReader.parse(text).forEachIndexed { index, row ->
            // Row 0 is the header.
            if (index == 0) return@forEachIndexed
            if (CsvReader.isBlank(row)) return@forEachIndexed

            if (row.size < COLUMN_COUNT) {
                malformedRows += index + 1
                return@forEachIndexed
            }

            val firstName = row[0].trim()
            val secondName = row[1].trim()
            val resultName = row[2].trim()
            val description = row[3].trim()

            if (firstName.isEmpty() || secondName.isEmpty() || resultName.isEmpty()) {
                malformedRows += index + 1
                return@forEachIndexed
            }
            dataRows++

            val firstId = ingredients.getOrPutIngredient(firstName)
            val secondId = ingredients.getOrPutIngredient(secondName)

            val resultId = MixTable.slug(resultName)
            val result = results.getOrPut(resultId) { MixResult(resultId, resultName, description) }

            val key = MixTable.pairKey(firstId, secondId)
            val existing = byPair[key]
            when {
                // The expected case for this data: A+B and B+A both present, agreeing.
                existing == null -> {
                    byPair[key] = result
                    pairsByResult.getOrPut(result.id) { mutableListOf() } +=
                        MixTable.normalise(firstId, secondId)
                }

                existing.id != result.id -> conflictingPairs += key
            }
        }

        return MixTable(
            ingredients = ingredients.values.sortedBy { it.name },
            results = results.values.sortedBy { it.name },
            byPair = byPair,
            pairsByResult = pairsByResult,
            report = MixTableReport(
                dataRows = dataRows,
                ingredientCount = ingredients.size,
                uniquePairs = byPair.size,
                malformedRows = malformedRows,
                conflictingPairs = conflictingPairs.distinct().sorted()
            )
        )
    }

    private fun LinkedHashMap<String, MixIngredient>.getOrPutIngredient(name: String): String {
        val id = MixTable.slug(name)
        getOrPut(id) { MixIngredient(id, name) }
        return id
    }

}
