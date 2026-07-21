package com.safemode.safekeepingforffx.data.reference

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * The current format version an exported code is written in. v2 adds ordering (edits and each
 * character's path are ordered lists) and an optional route [SphereGridBuild.name]. v1 (unordered)
 * codes are still accepted by [SphereGridBuildCodec.decode] and treated as "order unknown".
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
 * A decoded, shareable Sphere Grid build - also the payload of a saved route. A null section means
 * that section was not part of the build: [edits] null = the recipient's grid edits are left alone;
 * [paths] null = no character path is imported. An empty (non-null) list means "replace with
 * nothing", which is a meaningful clear.
 *
 * Both [edits] and each entry of [paths] are **ordered** - the order things were done - so a route
 * can be replayed step by step. A v1 code decodes into these lists too, just in an arbitrary order.
 * [name] is the route's label when it came from the library; null for a plain build.
 */
data class SphereGridBuild(
    val gridType: GridType,
    val edits: List<Pair<String, NodeContent>>?,
    val paths: Map<GridCharacter, List<String>>?,
    val name: String? = null
)

/**
 * Turns a [SphereGridBuild] into a compact copy/paste code and back. The code is plain JSON (so it
 * stays inspectable and unit-testable without Android); node content reuses [NodeContent.encode].
 *
 * [decode] is deliberately lenient about *contents* but strict about the *envelope*: an unreadable or
 * wrong-version envelope fails outright, while individual entries that don't decode or name an unknown
 * character or grid-less node are simply dropped. Node-id existence against a real grid is not checked
 * here - the repository does that when it applies the build.
 */
object SphereGridBuildCodec {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(RawBuild::class.java)

    fun encode(build: SphereGridBuild): String {
        val raw = RawBuild(
            v = BUILD_FORMAT_VERSION,
            grid = build.gridType.name,
            name = build.name,
            // v2 ordered edits: a list of [nodeId, encodedContent] pairs in the order made.
            editList = build.edits?.map { (nodeId, content) -> listOf(nodeId, content.encode()) },
            paths = build.paths?.mapKeys { it.key.name }
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

        // v2 carries ordered [id, encoded] pairs; v1 carries an unordered id -> encoded map.
        val edits: List<Pair<String, NodeContent>>? = when {
            raw.editList != null -> raw.editList.mapNotNull { pair ->
                val nodeId = pair.getOrNull(0)
                val encoded = pair.getOrNull(1)
                if (nodeId != null && encoded != null) {
                    NodeContent.decode(encoded)?.let { nodeId to it }
                } else {
                    null
                }
            }
            raw.edits != null -> raw.edits.mapNotNull { (nodeId, encoded) ->
                NodeContent.decode(encoded)?.let { nodeId to it }
            }
            else -> null
        }

        val paths = raw.paths?.mapNotNull { (name, ids) ->
            GridCharacter.entries.firstOrNull { it.name == name }?.let { it to ids }
        }?.toMap()

        if (edits == null && paths == null) {
            return Result.failure(IllegalArgumentException("This build code is empty."))
        }

        return Result.success(SphereGridBuild(gridType, edits, paths, raw.name))
    }
}

@JsonClass(generateAdapter = true)
internal data class RawBuild(
    val v: Int = 0,
    val grid: String = "",
    val name: String? = null,
    /** v2: ordered edits, each a `[nodeId, encodedContent]` pair. */
    val editList: List<List<String>>? = null,
    /** v1 only: unordered `nodeId -> encodedContent`. Read on decode, never written. */
    val edits: Map<String, String>? = null,
    val paths: Map<String, List<String>>? = null
)
