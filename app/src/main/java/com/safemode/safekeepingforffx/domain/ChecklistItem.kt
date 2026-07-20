package com.safemode.safekeepingforffx.domain

import androidx.annotation.DrawableRes
import com.safemode.safekeepingforffx.data.reference.Caution

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
    @param:DrawableRes val imageRes: Int? = null
)
