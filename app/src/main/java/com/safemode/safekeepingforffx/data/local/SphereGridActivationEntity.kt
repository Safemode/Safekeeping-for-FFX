package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity

/**
 * One activated node for one character - their path through the shared grid. Node content edits live
 * in `sphere_grid_node` and are shared across characters; this table records only who has taken
 * which node. A row present means activated; absent means not. [character] is a [GridCharacter] name.
 */
@Entity(tableName = "sphere_grid_activation", primaryKeys = ["character", "nodeId"])
data class SphereGridActivationEntity(
    val character: String,
    val nodeId: String
)
