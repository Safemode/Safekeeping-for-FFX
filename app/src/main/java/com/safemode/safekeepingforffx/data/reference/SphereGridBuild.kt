package com.safemode.safekeepingforffx.data.reference

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

/**
 * The current format version of an exported build code. Bumped only if the envelope shape changes in
 * a way older apps can't read; [SphereGridBuildCodec.decode] refuses anything else.
 */
private const val BUILD_FORMAT_VERSION = 1

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
 * A decoded, shareable Sphere Grid build. A null section means that section was not part of the
 * build: [edits] null = the recipient's grid edits are left alone; [paths] null = no character path
 * is imported. An empty (non-null) map means "replace with nothing", which is a meaningful clear.
 */
data class SphereGridBuild(
    val gridType: GridType,
    val edits: Map<String, NodeContent>?,
    val paths: Map<GridCharacter, Set<String>>?
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
            edits = build.edits?.mapValues { it.value.encode() },
            paths = build.paths?.mapKeys { it.key.name }?.mapValues { it.value.toList() }
        )
        return adapter.toJson(raw)
    }

    fun decode(text: String): Result<SphereGridBuild> {
        val raw = runCatching { adapter.fromJson(text.trim()) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("This isn't a valid build code."))
        if (raw.v != BUILD_FORMAT_VERSION) {
            return Result.failure(IllegalArgumentException("This build code is from a different version."))
        }
        val gridType = GridType.entries.firstOrNull { it.name == raw.grid }
            ?: return Result.failure(IllegalArgumentException("This build code names an unknown grid."))
        if (raw.edits == null && raw.paths == null) {
            return Result.failure(IllegalArgumentException("This build code is empty."))
        }

        val edits = raw.edits?.mapNotNull { (nodeId, encoded) ->
            NodeContent.decode(encoded)?.let { nodeId to it }
        }?.toMap()

        val paths = raw.paths?.mapNotNull { (name, ids) ->
            GridCharacter.entries.firstOrNull { it.name == name }?.let { it to ids.toSet() }
        }?.toMap()

        return Result.success(SphereGridBuild(gridType, edits, paths))
    }
}

@JsonClass(generateAdapter = true)
internal data class RawBuild(
    val v: Int = 0,
    val grid: String = "",
    val edits: Map<String, String>? = null,
    val paths: Map<String, List<String>>? = null
)
