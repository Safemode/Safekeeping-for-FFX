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
    @param:DrawableRes val imageRes: Int? = null,
    /**
     * Earliest point in the story this becomes reachable. Set it on every item in a category to
     * offer that list in story order as well as in its groups; leave it null and the category has
     * only its groups. See [ChecklistCategory.hasStoryOrder].
     */
    val storyStage: StoryStage? = null,
    /**
     * What getting this actually costs you *at* [storyStage] - the backtrack, the prerequisite, the
     * minigame you have to sit through. Shown only in story order, where it answers the question
     * that ordering raises: the list says you can have this now, so what do you have to do?
     *
     * [detail] says where the thing is and is true whenever you read it. This says what standing
     * here and going to get it involves, which is only meaningful once "here" is on screen.
     */
    val stageNote: String? = null
)
