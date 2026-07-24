package com.safemode.safekeepingforffx.ui.screens.checklist

import com.safemode.safekeepingforffx.domain.ChecklistItem

/**
 * How a checklist orders itself. Only offered for categories that carry story stages - see
 * [com.safemode.safekeepingforffx.data.reference.ChecklistCategory.hasStoryOrder].
 *
 * [GROUPED] answers "what do I still need for Auron?", [CHRONOLOGICAL] answers "what can I pick up
 * where I am now?". Both are useful at different points in a playthrough, so neither is a setting -
 * it's a switch on the list itself.
 */
enum class ChecklistSort(val label: String, val description: String) {
    GROUPED("Grouped", "In the list's own groups"),
    CHRONOLOGICAL("Chronological", "Soonest available in the story first");

    companion object {
        val DEFAULT = GROUPED
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
    ChecklistSort.GROUPED -> this
    ChecklistSort.CHRONOLOGICAL -> sortedBy { it.storyStage?.ordinal ?: Int.MAX_VALUE }
        .map { it.copy(section = it.storyStage?.label, tag = it.tag ?: it.shortSection) }
}

/** "Tidus - Caladbolg" is too wide for a badge; "Tidus" says the same thing. */
private val ChecklistItem.shortSection: String?
    get() = section?.substringBefore(" - ")
