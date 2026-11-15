package com.safemode.safekeepingforffx.ui.screens.spheregrid

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridPath
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.PathMode

/**
 * Stepping through a point-to-point route one node at a time, like a route replay: read-only, drawn
 * over the live grid without touching the player's progress.
 *
 * [stepIndex] counts moves: 0 is standing on the start, and step k has moved onto the path's k-th
 * node after it. [routeId] is null for a planned path that hasn't been saved to the library yet.
 */
data class PointRouteViewState(
    val routeId: Long?,
    val name: String?,
    val gridType: GridType,
    val character: GridCharacter,
    val mode: PathMode,
    val path: GridPath,
    val stepIndex: Int = 0
) {
    val stepCount: Int get() = path.steps

    val isSaved: Boolean get() = routeId != null

    /** The node reached at the current step. */
    val currentNodeId: String get() = path.nodeIds[stepIndex.coerceIn(0, path.nodeIds.lastIndex)]

    /** The nodes walked so far, start included. */
    val walked: List<String> get() = path.nodeIds.take(stepIndex + 1)

    /** The rest of the path from the current node on, drawn as still to walk. */
    val remaining: List<String> get() = path.nodeIds.drop(stepIndex)

    /** Each walked node's step number; the start has none, since nothing was done to reach it. */
    val orderLabels: Map<String, Int>
        get() = path.nodeIds.withIndex().drop(1).take(stepIndex).associate { it.value to it.index }

    /** Locks on the path not yet reached, which still need their key. */
    val locksAhead: Set<String>
        get() {
            val walkedIds = walked.toSet()
            return path.locks.mapNotNullTo(LinkedHashSet()) { it.nodeId.takeIf { id -> id !in walkedIds } }
        }

    /** Where [nodeId] falls on the path (0 for the start), or null if the path doesn't touch it. */
    fun stepOf(nodeId: String): Int? = path.nodeIds.indexOf(nodeId).takeIf { it >= 0 }
}

/** The pan/zoom slot a point-to-point view of one grid is kept under, apart from the live views. */
internal fun pointViewKey(gridType: GridType): String = "POINT/${gridType.name}"

/**
 * What to do on a node of the path, for the step bar's caption: e.g. "Lv.1 Lock: open it with a
 * Lv.1 Key Sphere", or "Strength +2: Power Sphere or Attribute Sphere".
 */
internal fun pointStepCaption(content: NodeContent, alreadyActivated: Boolean): String = when (content) {
    is NodeContent.Lock -> "${content.label()}: open it with a Lv.${content.level} Key Sphere"
    NodeContent.Empty -> "Blank node: nothing to activate"
    else -> if (alreadyActivated) {
        "${content.label()}: already activated"
    } else {
        "${content.label()}: ${content.activationSpheres().joinToString(" or ")}"
    }
}

/** The path's cost and keys on one line, e.g. "5 steps, 5 Sphere Levels · 1x Lv.1 Key Sphere". */
internal fun GridPath.costSummary(): String {
    val steps = if (steps == 1) "1 step" else "$steps steps"
    val levels = if (sphereLevels == 1) "1 Sphere Level" else "$sphereLevels Sphere Levels"
    val keys = if (locks.isEmpty()) "no locks" else keySummary()
    return "$steps, $levels · $keys"
}
