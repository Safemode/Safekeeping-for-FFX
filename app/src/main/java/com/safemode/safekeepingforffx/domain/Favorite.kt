package com.safemode.safekeepingforffx.domain

/**
 * A starred item, as the pair that identifies it: which list it lives in, and its id inside that
 * list. Item ids are only unique within a category, so neither half means anything alone.
 */
data class Favorite(
    val categoryId: String,
    val itemId: String,
    /** When it was starred. Orders favorites within a list, oldest first. */
    val createdAt: Long
)
