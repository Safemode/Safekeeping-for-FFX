package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * A single "the player has found this" record.
 *
 * Only user progress lives in the database. The reference data that describes each item (name,
 * location, missable flag) is compiled into the app under `data/reference`, so correcting a
 * location description in a future release is a Kotlin edit rather than a schema migration.
 *
 * Every tracker category shares this one table, keyed by [categoryId]. New categories add rows,
 * never columns, which is what keeps the database at version 1 indefinitely.
 */
@Entity(
    tableName = "checklist_progress",
    primaryKeys = ["categoryId", "itemId"],
    indices = [Index("categoryId")]
)
data class ChecklistProgressEntity(
    val categoryId: String,
    val itemId: String,
    val isChecked: Boolean,
    val updatedAt: Long
)
