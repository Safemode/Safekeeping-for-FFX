package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A player's edit to one sphere-grid node: the content they've written over the vanilla node,
 * stored as an encoded string (see NodeContent.encode). A row exists only for a node that differs
 * from vanilla; absent means the node is untouched and shows its original content.
 */
@Entity(tableName = "sphere_grid_node")
data class SphereGridNodeEntity(
    @PrimaryKey val nodeId: String,
    val content: String
)
