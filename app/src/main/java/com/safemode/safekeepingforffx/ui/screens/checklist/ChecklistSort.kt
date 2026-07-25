package com.safemode.safekeepingforffx.ui.screens.checklist

import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.ui.components.SortOption

/**
 * How a checklist orders itself. Only offered for categories that carry story stages - see
 * [com.safemode.safekeepingforffx.data.reference.ChecklistCategory.hasStoryOrder].
 *
 * [GROUPED] answers "what do I still need for Auron?", [CHRONOLOGICAL] answers "what can I pick up
 * where I am now?". Both are useful at different points in a playthrough, so this is a switch on the
 * list rather than an app-wide preference - but it is remembered per list, because which question
 * you are asking of a given list doesn't change just because you left the screen.
 */
enum class ChecklistSort(
    override val label: String,
    override val description: String
) : SortOption {
    GROUPED("Grouped", "In the list's own groups"),
    CHRONOLOGICAL("Chronological", "Soonest available in the story first");

    companion object {
        val DEFAULT = GROUPED

        /**
         * Resolves what was saved. Stored by name so reordering this enum can't change what an
         * existing install reads, and an unknown name falls back to [DEFAULT] rather than failing -
         * a value written by a newer build should leave the list readable, not empty.
         */
        fun fromStored(name: String?): ChecklistSort =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Story order reuses the section machinery rather than adding a second kind of header: the stage
 * becomes the group, and the group the item came from moves onto the row as a badge so you can
 * still tell whose crest you are looking at.
 *
 * [sortedBy] is stable, so items sharing a stage keep the order they were declared in - which is
 * the order they read best in, prerequisites first. Anything without a stage sinks to the bottom
 * rather than being dropped, though a category is only offered this order when every item has one.
 */
internal fun List<ChecklistItem>.inOrder(sort: ChecklistSort): List<ChecklistItem> = when (sort) {
    // Stage notes are answers to "why is this listed here?", so they only make sense once a stage
    // is on screen. Dropped rather than hidden by the row, which has no idea what order it is in.
    ChecklistSort.GROUPED -> map { it.copy(stageNote = null) }
    ChecklistSort.CHRONOLOGICAL -> sortedBy { it.storyStage?.ordinal ?: Int.MAX_VALUE }
        .map { it.copy(section = it.storyStage?.label, tag = it.tag ?: it.shortSection) }
}

/** "Tidus - Caladbolg" is too wide for a badge; "Tidus" says the same thing. */
private val ChecklistItem.shortSection: String?
    get() = section?.substringBefore(" - ")
