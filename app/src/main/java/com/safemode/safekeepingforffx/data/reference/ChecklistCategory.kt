package com.safemode.safekeepingforffx.data.reference

/**
 * One trackable list - Al Bhed Primers, Jecht Spheres, Celestial Weapons.
 *
 * [id] is written to the progress table as `categoryId` and must never change once shipped.
 * [note] is optional advice shown above the list, for categories where *when* you collect things
 * matters as much as where.
 */
data class ChecklistCategory(
    val id: String,
    val label: String,
    val items: List<ReferenceItem>,
    val note: String? = null,
    /** Extra advice that only applies where Dark Aeons exist. Hidden on the original PS2 release. */
    val darkAeonNote: String? = null,
    /**
     * False for pure reference lists that aren't a to-do: no checkboxes, no progress bar, no
     * reset, and they stay off the Home screen summary because "0 of 125" would be meaningless.
     */
    val trackProgress: Boolean = true,
    /**
     * False to keep a fully tracked list off the Home screen summary. The list still has
     * checkboxes, a progress bar on its own screen, and a reset - it just isn't one of the
     * headline collections Home reports on.
     */
    val showOnHome: Boolean = true
)
