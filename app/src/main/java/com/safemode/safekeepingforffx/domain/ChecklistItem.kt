package com.safemode.safekeepingforffx.domain

import androidx.annotation.DrawableRes
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.StoryStage

/**
 * Static reference data merged with the player's progress - what the UI actually renders.
 */
data class ChecklistItem(
    val id: String,
    val title: String,
    val location: String,
    val detail: String,
    val caution: Caution?,
    val isChecked: Boolean,
    /** Starred, so it also appears in Favorites. Independent of [isChecked]. */
    val isFavorite: Boolean = false,
    val section: String? = null,
    val tag: String? = null,
    @param:DrawableRes val imageRes: Int? = null,
    /** Earliest point in the story this is reachable, when the category tracks that. */
    val storyStage: StoryStage? = null,
    /** What collecting it there involves. Only carried while the list is in story order. */
    val stageNote: String? = null
)

/**
 * Dark Aeons don't exist on the original PS2 release, so a "guarded" warning there would be wrong.
 * Missable stays - Home is destroyed in every version.
 *
 * Lives here rather than on one screen because every list that renders an item has to apply it, and
 * a version-appropriate warning is a property of the item, not of the screen showing it.
 */
fun ChecklistItem.forVersion(version: GameVersion): ChecklistItem =
    if (!version.hasDarkAeons && caution is Caution.Guarded) copy(caution = null) else this
