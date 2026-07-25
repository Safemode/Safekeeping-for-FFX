package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * A single "keep this where I can find it" record.
 *
 * Keyed by category plus item id, the same pair [ChecklistProgressEntity] uses, because item ids are
 * only unique inside a category. Every category that can be favorited shares this one table, so
 * adding another one later adds rows rather than columns.
 *
 * Nothing about the item itself is copied in - only the key. The Favorites screen looks each one
 * back up in the list that owns it, so a favorite always shows the current text, and an item a later
 * release renames or drops stops resolving instead of lingering as a stale copy.
 */
@Entity(
    tableName = "favorite",
    primaryKeys = ["categoryId", "itemId"],
    indices = [Index("categoryId")]
)
data class FavoriteEntity(
    val categoryId: String,
    val itemId: String,
    /** When it was starred. Orders favorites within a list, oldest first. */
    val createdAt: Long
)
