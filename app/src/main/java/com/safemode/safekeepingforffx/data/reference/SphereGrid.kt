package com.safemode.safekeepingforffx.data.reference

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

const val SPHERE_GRID_ID = "sphere_grid"
const val SPHERE_GRID_LABEL = "Sphere Grid Planner"
const val SPHERE_GRID_ASSET = "sphere_grid.json"
const val EXPERT_SPHERE_GRID_ASSET = "expert_sphere_grid.json"

/**
 * The two grid layouts the planner can show. [asset] is the bundled data file, or null for a grid
 * that hasn't been built yet - selecting it shows an "unavailable" placeholder rather than nodes.
 * Node ids are namespaced per grid via [idPrefix] (Standard uses "n...", Expert uses "x..."), so a
 * single edit/activation table can hold both without collisions.
 */
enum class GridType(val label: String, val asset: String?, val idPrefix: String) {
    STANDARD("Standard", SPHERE_GRID_ASSET, "n"),
    EXPERT("Expert", EXPERT_SPHERE_GRID_ASSET, "x");

    val isAvailable: Boolean get() = asset != null

    companion object {
        val DEFAULT = STANDARD
    }
}

/** The seven playable characters, each tracked as its own path through the shared grid. */
enum class GridCharacter(val displayName: String) {
    TIDUS("Tidus"),
    YUNA("Yuna"),
    AURON("Auron"),
    KIMAHRI("Kimahri"),
    WAKKA("Wakka"),
    LULU("Lulu"),
    RIKKU("Rikku");

    companion object {
        val DEFAULT = TIDUS
    }
}

/**
 * The kinds of node on the grid, mirroring the Standard Sphere Grid legend. Colours are deliberately
 * left out of this layer - the reference data stays free of any Compose dependency, so the UI owns
 * the mapping from type to colour. A node's type here is really its *content family*: an attribute,
 * one of the four ability families, a lock, or empty.
 */
enum class NodeType(val isAbility: Boolean = false, val isLock: Boolean = false) {
    HP,
    MP,
    STRENGTH,
    DEFENSE,
    MAGIC,
    MAGIC_DEFENSE,
    AGILITY,
    ACCURACY,
    EVASION,
    LUCK,
    WHITE_MAGIC(isAbility = true),
    BLACK_MAGIC(isAbility = true),
    SKILL(isAbility = true),
    SPECIAL(isAbility = true),
    LOCK(isLock = true),
    EMPTY;

    companion object {
        /** Unknown codes fall back to [EMPTY] rather than throwing on a stray data value. */
        fun fromCode(code: String): NodeType = entries.firstOrNull { it.name == code } ?: EMPTY

        /** The ten stat attributes, in legend order - the palette the node editor offers. */
        val attributes: List<NodeType> =
            listOf(HP, MP, STRENGTH, DEFENSE, MAGIC, MAGIC_DEFENSE, AGILITY, ACCURACY, EVASION, LUCK)
    }
}

/**
 * What sits on a node - either originally (vanilla) or after the player has edited it. This is the
 * real Sphere Grid content model: a stat with a value, one of the 85 abilities, a lock, or empty.
 * [displayType] is the [NodeType] the UI colours the node by.
 */
sealed interface NodeContent {
    val displayType: NodeType

    data object Empty : NodeContent {
        override val displayType: NodeType get() = NodeType.EMPTY
    }

    data class Attribute(val attribute: NodeType, val value: Int) : NodeContent {
        override val displayType: NodeType get() = attribute
    }

    data class Ability(val name: String, val family: NodeType) : NodeContent {
        override val displayType: NodeType get() = family
    }

    data class Lock(val level: Int) : NodeContent {
        override val displayType: NodeType get() = NodeType.LOCK
    }

    /** True for content the player is allowed to overwrite. Locks are permanent gates in-game. */
    val isEditable: Boolean get() = this !is Lock

    /** A compact, stable string for the database. Ability names never contain the delimiter. */
    fun encode(): String = when (this) {
        Empty -> "E"
        is Attribute -> "A|${attribute.name}|$value"
        is Ability -> "B|$name|${family.name}"
        is Lock -> "L|$level"
    }

    companion object {
        fun decode(raw: String): NodeContent? {
            val parts = raw.split("|")
            return when (parts.getOrNull(0)) {
                "E" -> Empty
                "A" -> {
                    val attr = parts.getOrNull(1)?.let(NodeType::fromCode)
                    val value = parts.getOrNull(2)?.toIntOrNull()
                    if (attr != null && value != null) Attribute(attr, value) else null
                }
                "B" -> if (parts.size >= 3) Ability(parts[1], NodeType.fromCode(parts[2])) else null
                "L" -> parts.getOrNull(1)?.toIntOrNull()?.let { Lock(it) }
                else -> null
            }
        }
    }
}

/** A single sphere on the grid, with the content it holds in the vanilla game. */
data class SphereGridNode(
    val id: String,
    val x: Float,
    val y: Float,
    val original: NodeContent
)

/** An undirected connection drawn as a line between two nodes. */
data class SphereGridEdge(val fromId: String, val toId: String)

/** Axis-aligned bounds of the whole grid, so the UI can fit it to the viewport. */
data class GridBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
}

/** One ability the editor can place, from the bundled catalog. */
data class GridAbility(val name: String, val family: NodeType)

/** The parsed grid: nodes, deduplicated edges, bounds, and the catalog the editor draws from. */
data class GridData(
    val nodes: List<SphereGridNode>,
    val edges: List<SphereGridEdge>,
    val bounds: GridBounds,
    val abilities: List<GridAbility> = emptyList()
) {
    val totalNodes: Int get() = nodes.size

    companion object {
        val EMPTY = GridData(emptyList(), emptyList(), GridBounds(0f, 0f, 0f, 0f))
    }
}

@JsonClass(generateAdapter = true)
internal data class RawGrid(
    val nodes: List<RawNode> = emptyList(),
    val abilities: List<RawAbility> = emptyList()
)

@JsonClass(generateAdapter = true)
internal data class RawNode(
    val id: Int,
    val x: Int,
    val y: Int,
    val t: String,
    val v: Int? = null,
    val l: Int? = null,
    val ab: String? = null,
    val c: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
internal data class RawAbility(val n: String, val f: String)

/**
 * Turns a bundled Sphere Grid asset into [GridData].
 *
 * The asset is the real grid - every node's position, content and connections come from
 * community-reconstructed game data (see the app's About screen for attribution). Connections in the
 * source are mutual, so edges are deduplicated to one line per pair here. [idPrefix] namespaces the
 * emitted node ids per grid (see [GridType.idPrefix]) so Standard and Expert can share one edit and
 * activation table without their integer node ids colliding.
 */
object SphereGridParser {

    private const val BOUNDS_MARGIN = 120f

    fun parse(json: String, idPrefix: String = "n"): GridData {
        val moshi = Moshi.Builder().build()
        val raw = moshi.adapter(RawGrid::class.java).fromJson(json) ?: return GridData.EMPTY
        if (raw.nodes.isEmpty()) return GridData.EMPTY

        val nodes = raw.nodes.map { n ->
            SphereGridNode(
                id = "$idPrefix${n.id}",
                x = n.x.toFloat(),
                y = n.y.toFloat(),
                original = contentOf(n)
            )
        }

        val ids = raw.nodes.mapTo(HashSet()) { it.id }
        val seen = HashSet<Long>()
        val edges = ArrayList<SphereGridEdge>()
        raw.nodes.forEach { n ->
            n.c.forEach { m ->
                if (m in ids) {
                    val a = minOf(n.id, m)
                    val b = maxOf(n.id, m)
                    if (seen.add(a.toLong() * 1_000_000L + b)) {
                        edges += SphereGridEdge("$idPrefix$a", "$idPrefix$b")
                    }
                }
            }
        }

        val bounds = GridBounds(
            minX = nodes.minOf { it.x } - BOUNDS_MARGIN,
            minY = nodes.minOf { it.y } - BOUNDS_MARGIN,
            maxX = nodes.maxOf { it.x } + BOUNDS_MARGIN,
            maxY = nodes.maxOf { it.y } + BOUNDS_MARGIN
        )

        val abilities = raw.abilities.map { GridAbility(it.n, NodeType.fromCode(it.f)) }

        return GridData(nodes = nodes, edges = edges, bounds = bounds, abilities = abilities)
    }

    private fun contentOf(n: RawNode): NodeContent {
        val type = NodeType.fromCode(n.t)
        return when {
            type == NodeType.LOCK -> NodeContent.Lock(n.l ?: 1)
            type == NodeType.EMPTY -> NodeContent.Empty
            type.isAbility -> NodeContent.Ability(n.ab ?: "Ability", type)
            else -> NodeContent.Attribute(type, n.v ?: 0)
        }
    }
}
