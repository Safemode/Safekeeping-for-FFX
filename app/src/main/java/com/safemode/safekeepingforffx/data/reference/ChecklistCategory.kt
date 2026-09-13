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
    val showOnHome: Boolean = true,
    /**
     * An item that stands for "everything else in this list", like the platinum trophy. It ticks
     * itself when the last of the others is ticked, and unticks when one of them is unticked.
     */
    val completionItemId: String? = null,
    /**
     * True for a list every character works through on their own, like the Overdrive Modes: the
     * screen gets a character picker and each character's ticks are stored apart. Stars stay shared,
     * since a favorite marks the entry rather than one character's progress on it.
     */
    val perCharacter: Boolean = false,
    /**
     * True for a list that only exists on releases with Dark Aeons. On the original NA PS2 release
     * it drops off the Home summary, card and totals both, but keeps its place in the drawer.
     */
    val requiresDarkAeons: Boolean = false
) {
    /** Whether this list has anything to collect on [version]. See [requiresDarkAeons]. */
    fun isAvailableOn(version: GameVersion): Boolean = !requiresDarkAeons || version.hasDarkAeons

    /**
     * Where [character]'s ticks for this list are stored. A list that isn't [perCharacter] keeps its
     * progress under [id] and ignores the character.
     *
     * Per-character progress is a suffixed key rather than a column of its own, so the progress
     * table, backups and "reset all" handle it with no changes at all.
     */
    fun progressKey(character: GridCharacter?): String =
        if (perCharacter && character != null) "${id}_${character.name.lowercase()}" else id

    /** Every key this list's progress is spread across: one per character, or just [id]. */
    val progressKeys: List<String>
        get() = if (perCharacter) GridCharacter.entries.map { progressKey(it) } else listOf(id)

    /**
     * The items ticked under every one of [progressKeys], given ticks grouped by key. For a
     * per-character list that means learned by everyone; for any other list it is just its ticks.
     */
    fun checkedEverywhere(checkedByKey: Map<String, Set<String>>): Set<String> =
        progressKeys.map { checkedByKey[it].orEmpty() }.reduce { acc, ids -> acc intersect ids }

    /**
     * Whether this list can also be shown in story order. Every item has to carry a stage or the
     * ordering would silently strand the ones that don't, so it is all or nothing.
     */
    val hasStoryOrder: Boolean = items.isNotEmpty() && items.all { it.storyStage != null }

    /**
     * What [completionItemId] should be set to now that [changedItemId] has been ticked or unticked,
     * given [checkedIds] - everything ticked *after* that change. Null means leave it as it is.
     *
     * Ticking or unticking the completion item by hand is never overridden: it only follows the
     * others when one of the others moves. Only an untick can clear it, so a completion item ticked
     * by hand on a partial list isn't wiped by the next thing you tick.
     */
    fun completionUpdate(checkedIds: Set<String>, changedItemId: String): Boolean? {
        val completionId = completionItemId ?: return null
        if (changedItemId == completionId) return null

        val othersDone = items.all { it.id == completionId || it.id in checkedIds }
        val completionChecked = completionId in checkedIds
        val wasUntick = changedItemId !in checkedIds

        return when {
            othersDone && !completionChecked -> true
            !othersDone && completionChecked && wasUntick -> false
            else -> null
        }
    }
}
