package com.safemode.safekeepingforffx.data.reference

/**
 * Minimal RFC 4180 reader, shared by the CSV-backed reference data.
 *
 * Hand-rolled because descriptions contain commas inside quoted fields, so splitting on commas
 * would silently corrupt them.
 */
object CsvReader {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes -> when {
                    // A doubled quote inside a quoted field is a literal quote.
                    char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }

                    char == '"' -> inQuotes = false
                    else -> field.append(char)
                }

                char == '"' -> inQuotes = true
                char == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }

                char == '\r' -> Unit
                char == '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row)
                    row = mutableListOf()
                }

                else -> field.append(char)
            }
            index++
        }

        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }

    /** True for a row that is just a trailing blank line rather than data. */
    fun isBlank(row: List<String>): Boolean = row.all { it.isBlank() }
}
