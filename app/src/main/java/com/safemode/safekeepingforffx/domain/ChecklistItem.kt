package com.safemode.safekeepingforffx.domain

import androidx.annotation.DrawableRes
import com.safemode.safekeepingforffx.data.reference.Caution
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
    val section: String? = null,
    val tag: String? = null,
    @param:DrawableRes val imageRes: Int? = null,
    /** Earliest point in the story this is reachable, when the category tracks that. */
    val storyStage: StoryStage? = null,
    /** What collecting it there involves. Only carried while the list is in story order. */
    val stageNote: String? = null
)
