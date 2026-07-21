package com.safemode.safekeepingforffx.data.reference

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * The current format version an exported code is written in. v2 records a single ordered timeline of
 * edits and activations (so a route replays exactly as it happened, edits included) plus an optional
 * route [SphereGridBuild.name]. v1 (unordered edits map + paths) codes are still accepted by
 * [SphereGridBuildCodec.decode] and folded into the timeline as "order unknown".
 */
private const val BUILD_FORMAT_VERSION = 2

/** Versions [SphereGridBuildCodec.decode] will read. Anything else is refused as incompatible. */
private val SUPPORTED_BUILD_VERSIONS = setOf(1, 2)

/**
 * How much of the player's Sphere Grid work an exported build carries. A build is only ever the two
 * pieces of player data - shared content edits and per-character paths - since everything else is
 * reproducible from the bundled grid asset.
 */
enum class BuildScope(val label: String) {
    EDITS_AND_CURRENT("Edits + this character"),
    EDITS_AND_ALL("Edits + all characters"),
    CURRENT_PATH("This character's path only"),
    EDITS_ONLY("Grid edits only");

    /** True when an export of this scope carries the shared grid content edits. */
    val includesEdits: Boolean get() = this != CURRENT_PATH

    /** True when an export of this scope carries every character's path, not just the current one. */
    val includesAllPaths: Boolean get() = this == EDITS_AND_ALL

    /** True when an export of this scope carries at least one character's path. */
    val includesAnyPath: Boolean get() = this != EDITS_ONLY
}

/**
 * One thing the player did, on the shared timeline: edited a node's content, or activated a node for
 * a character. Edits are grid-wide; activations belong to one character. A route is just an ordered
 * list of these, which is what lets a replay reveal edits and activations in the order they happened.
 */
sealed interface RouteEvent {
    val nodeId: String

    data class Edit(override val nodeId: String, val content: NodeContent) : RouteEvent

    data class Activate(val character: GridCharacter, override val nodeId: String) : RouteEvent
}

/**
 * A decoded, shareable Sphere Grid build - also the payload of a saved route. [events] is the ordered
 * timeline of everything the build carries (edits and activations interleaved); it is non-empty for a
 * valid build. [name] is the route's label when it came from the library; null for a plain build.
 */
data class SphereGridBuild(
    val gridType: GridType,
    val events: List<RouteEvent>,
    val name: String? = null
)

/**
 * Turns a [SphereGridBuild] into a compact copy/paste code and back. The code is plain JSON (so it
 * stays inspectable and unit-testable without Android); node content reuses [NodeContent.encode].
 *
 * [decode] is deliberately lenient about *contents* but strict about the *envelope*: an unreadable or
 * wrong-version envelope fails outright, while individual events that don't decode or name an unknown
 * character are simply dropped. Node-id existence against a real grid is not checked here - the
 * repository does that when it applies the build.
 */
object SphereGridBuildCodec {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(RawBuild::class.java)

    fun encode(build: SphereGridBuild): String {
        val raw = RawBuild(
            v = BUILD_FORMAT_VERSION,
            grid = build.gridType.name,
            name = build.name,
            // Each event is a short array: an edit is ["E", nodeId, encoded]; an activation is
            // ["A", characterName, nodeId]. The array order is the timeline order.
            events = build.events.map { event ->
                when (event) {
                    is RouteEvent.Edit -> listOf("E", event.nodeId, event.content.encode())
                    is RouteEvent.Activate -> listOf("A", event.character.name, event.nodeId)
                }
            }
        )
        return adapter.toJson(raw)
    }

    fun decode(text: String): Result<SphereGridBuild> {
        val raw = runCatching { adapter.fromJson(text.trim()) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("This isn't a valid build code."))
        if (raw.v !in SUPPORTED_BUILD_VERSIONS) {
            return Result.failure(IllegalArgumentException("This build code is from a different version."))
        }
        val gridType = GridType.entries.firstOrNull { it.name == raw.grid }
            ?: return Result.failure(IllegalArgumentException("This build code names an unknown grid."))

        val events = if (raw.events != null) {
            raw.events.mapNotNull(::parseEvent)
        } else {
            legacyEvents(raw)
        }

        if (events.isEmpty()) {
            return Result.failure(IllegalArgumentException("This build code is empty."))
        }

        return Result.success(SphereGridBuild(gridType, events, raw.name))
    }

    /** One v2 event array back into a [RouteEvent], or null if it's malformed or undecodable. */
    private fun parseEvent(tokens: List<String>): RouteEvent? = when (tokens.getOrNull(0)) {
        "E" -> {
            val nodeId = tokens.getOrNull(1)
            val encoded = tokens.getOrNull(2)
            if (nodeId != null && encoded != null) {
                NodeContent.decode(encoded)?.let { RouteEvent.Edit(nodeId, it) }
            } else {
                null
            }
        }
        "A" -> {
            val character = GridCharacter.entries.firstOrNull { it.name == tokens.getOrNull(1) }
            val nodeId = tokens.getOrNull(2)
            if (character != null && nodeId != null) RouteEvent.Activate(character, nodeId) else null
        }
        else -> null
    }

    /** A v1 code had no timeline: fold its edits then its paths into events, order unknown. */
    private fun legacyEvents(raw: RawBuild): List<RouteEvent> {
        val edits = raw.edits.orEmpty().mapNotNull { (nodeId, encoded) ->
            NodeContent.decode(encoded)?.let { RouteEvent.Edit(nodeId, it) }
        }
        val activations = raw.paths.orEmpty().flatMap { (name, ids) ->
            val character = GridCharacter.entries.firstOrNull { it.name == name }
            if (character != null) ids.map { RouteEvent.Activate(character, it) } else emptyList()
        }
        return edits + activations
    }
}

@JsonClass(generateAdapter = true)
internal data class RawBuild(
    val v: Int = 0,
    val grid: String = "",
    val name: String? = null,
    /** v2: the ordered timeline, each event a short `[kind, ...]` array. */
    val events: List<List<String>>? = null,
    /** v1 only: unordered `nodeId -> encodedContent`. Read on decode, never written. */
    val edits: Map<String, String>? = null,
    /** v1 only: unordered `characterName -> nodeIds`. Read on decode, never written. */
    val paths: Map<String, List<String>>? = null
)
