package com.safemode.safekeepingforffx.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved point-to-point route: a path the path finder planned between two nodes, kept so the
 * player can step through it node by node. Kept apart from [SphereGridRouteEntity], which holds whole
 * builds (edits and every character's path); this is one character's walk from A to B.
 *
 * The path is stored as planned rather than re-planned when opened. Re-planning would give a
 * different answer as soon as the player activates nodes or opens locks, and a saved route should
 * read the same each time it is opened.
 *
 * - [gridType], [character] and [mode] are enum names, like everywhere else in the database.
 * - [nodeIds] is the path in walking order, start first, as comma-separated grid node ids.
 * - [lockIds] are the locks on it that were still unopened when it was planned, same encoding.
 * - [moveQuarters] is the movement cost at planning time, in quarter Sphere Levels.
 */
@Entity(tableName = "point_route")
data class PointRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gridType: String,
    val character: String,
    val mode: String,
    val nodeIds: String,
    val lockIds: String,
    val moveQuarters: Int,
    val createdAt: Long,
    val updatedAt: Long
)
