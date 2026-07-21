package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved Sphere Grid route in the player's library - their own or one imported from someone else.
 * The route itself (grid, ordered edits, ordered per-character paths) is kept as an encoded build
 * code in [payload], the same string that gets shared, so saving, sharing and replaying all speak one
 * format. [gridType] is duplicated out of the payload only so the library can label rows without
 * decoding every one.
 */
@Entity(tableName = "sphere_grid_route")
data class SphereGridRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gridType: String,
    val createdAt: Long,
    val payload: String
)
