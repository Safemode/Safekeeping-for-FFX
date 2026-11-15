package com.safemode.safekeepingforffx.data.reference

import java.util.PriorityQueue

/** What a planned path may do about locks nobody has opened yet. */
enum class PathMode(val label: String) {
    /** Fewest Sphere Levels, opening locks where that is shorter. Cheaper keys break ties. */
    SHORTEST("Shortest"),

    /**
     * Cheapest keys first, then fewest Sphere Levels. A key counts for its level, so one Lv.4 Key
     * Sphere weighs as much as four Lv.1s - the higher levels are the rare ones.
     */
    FEWEST_KEYS("Fewest keys"),

    /** Unopened locks are walls. Locks that have already been opened are ordinary nodes. */
    NO_LOCKS("No locks");

    companion object {
        val DEFAULT = SHORTEST
    }
}

/** An unopened lock a path runs through, and so a Key Sphere it needs. */
data class LockOnPath(val nodeId: String, val level: Int)

/**
 * A path across one grid, from its first node to its last.
 *
 * [moveQuarters] is the movement cost in quarter Sphere Levels: the game spends one Sphere Level to
 * move onto a node the character hasn't activated, or to move back across four they have.
 */
data class GridPath(
    val nodeIds: List<String>,
    val moveQuarters: Int,
    /** Unopened locks on the path, in the order it reaches them. */
    val locks: List<LockOnPath>
) {
    /** Nodes moved, not counting the one the path starts on. */
    val steps: Int get() = nodeIds.size - 1

    /** Sphere Levels to walk it, rounded up to a whole level. */
    val sphereLevels: Int get() = (moveQuarters + 3) / 4

    /** Key Spheres needed by lock level, lowest level first. */
    val keysByLevel: Map<Int, Int>
        get() = locks.groupingBy { it.level }.eachCount().toSortedMap()
}

/**
 * Finds the cheapest path between two nodes on a grid, reading lock state and the character's path
 * the same way the planner draws them.
 *
 * A lock counts as unopened while it holds its vanilla Lock content with no edit on it (see
 * [com.safemode.safekeepingforffx.ui.screens.spheregrid.SphereGridUiState.isLockedGate]). Opening one
 * writes a blank edit, so an opened lock is an ordinary node here.
 *
 * Dijkstra's algorithm with a two-part cost per node entered: movement (4 for a node the character
 * hasn't activated, 1 for one they have) and keys (the lock's level, for an unopened lock). [PathMode]
 * picks which part is compared first. The grids are under 900 nodes, so a search is far quicker than a
 * frame and nothing is cached between calls. Equal-cost paths are settled in node-id order, so the same
 * inputs always give the same path.
 */
object SphereGridPathfinder {

    /** Quarter Sphere Levels to move onto a node the character hasn't activated. */
    const val NEW_NODE_COST = 4

    /** Quarter Sphere Levels to move onto a node the character has activated. */
    const val ACTIVATED_NODE_COST = 1

    /**
     * Weight of the cost part compared first. The part compared second sums to well under this along
     * any path (at most 4 per node), so it only ever breaks ties and never outweighs the first part.
     */
    private const val PRIMARY = 1_000_000L

    /**
     * The cheapest path from [fromId] to [toId] under [mode], or null when there is none: an id that
     * isn't on [grid], or - with [PathMode.NO_LOCKS] - a pair that unopened locks keep apart, including
     * an unopened lock at either end.
     *
     * [overrides] are the shared node edits and [activated] the viewed character's path; both may hold
     * ids from the other grid, which are ignored.
     */
    fun find(
        grid: GridData,
        fromId: String,
        toId: String,
        mode: PathMode,
        overrides: Map<String, NodeContent>,
        activated: Set<String>
    ): GridPath? {
        val nodes = grid.nodesById
        val start = nodes[fromId] ?: return null
        if (toId !in nodes) return null

        fun lockLevel(id: String): Int? =
            (nodes[id]?.original as? NodeContent.Lock)?.level?.takeIf { id !in overrides }

        fun moveCost(id: String): Int = if (id in activated) ACTIVATED_NODE_COST else NEW_NODE_COST

        if (mode == PathMode.NO_LOCKS && (lockLevel(fromId) != null || lockLevel(toId) != null)) {
            return null
        }

        /** Cost of moving onto [id], or null when this mode can't enter it. */
        fun enterCost(id: String): Long? {
            val key = lockLevel(id)
            if (key != null && mode == PathMode.NO_LOCKS) return null
            val move = moveCost(id).toLong()
            val keys = (key ?: 0).toLong()
            return when (mode) {
                PathMode.FEWEST_KEYS -> keys * PRIMARY + move
                PathMode.SHORTEST, PathMode.NO_LOCKS -> move * PRIMARY + keys
            }
        }

        val best = HashMap<String, Long>()
        val previous = HashMap<String, String>()
        val settled = HashSet<String>()
        val queue = PriorityQueue<Pair<Long, String>>(compareBy({ it.first }, { it.second }))
        best[start.id] = 0L
        queue.add(0L to start.id)

        while (queue.isNotEmpty()) {
            val (cost, id) = queue.poll() ?: break
            if (!settled.add(id)) continue
            if (id == toId) break
            grid.neighbours[id].orEmpty().forEach { next ->
                if (next in settled) return@forEach
                val step = enterCost(next) ?: return@forEach
                val candidate = cost + step
                if (candidate < (best[next] ?: Long.MAX_VALUE)) {
                    best[next] = candidate
                    previous[next] = id
                    queue.add(candidate to next)
                }
            }
        }
        if (toId !in settled) return null

        val path = generateSequence(toId) { previous[it] }.toList().asReversed()
        return GridPath(
            nodeIds = path,
            moveQuarters = path.drop(1).sumOf { moveCost(it) },
            locks = path.mapNotNull { id -> lockLevel(id)?.let { LockOnPath(id, it) } }
        )
    }
}
