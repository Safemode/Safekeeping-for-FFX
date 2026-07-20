package com.safemode.safekeepingforffx.data.reference

import androidx.annotation.DrawableRes

/**
 * Static description of one trackable thing in the game. Shared by every category.
 *
 * [id] is the storage contract: it is what gets written to the progress table. Never renumber or
 * rename an existing id, or players silently lose their checkmarks on update.
 */
data class ReferenceItem(
    val id: String,
    val title: String,
    val location: String,
    val detail: String,
    /** Null when the item can simply be picked up whenever you reach it. */
    val caution: Caution? = null,
    /** Optional grouping header, e.g. "Tidus - Caladbolg". Null means the list is flat. */
    val section: String? = null,
    /** Optional neutral label shown beside the title, e.g. "Weapon" or "Armor". */
    val tag: String? = null,
    /**
     * Bundled in-game screenshot showing exactly where the item sits, or null if none exists.
     * A drawable rather than a URL so the screen works with no connection.
     */
    @param:DrawableRes val imageRes: Int? = null
)
