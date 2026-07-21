package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.local.SphereGridActivationDao
import com.safemode.safekeepingforffx.data.local.SphereGridActivationEntity
import com.safemode.safekeepingforffx.data.local.SphereGridNodeDao
import com.safemode.safekeepingforffx.data.local.SphereGridNodeEntity
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridBuildCodec
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * The grid's shape and vanilla content are reference data parsed from bundled assets; the player's
 * per-grid content edits and per-character activations live in the database. Reference and progress
 * never mix, and each grid is parsed once and reused.
 */
class SphereGridRepository(
    private val assets: AssetManager,
    private val database: FfxDatabase,
    private val nodeDao: SphereGridNodeDao,
    private val activationDao: SphereGridActivationDao
) {
    private val cache = ConcurrentHashMap<GridType, GridData>()

    /** Parses the requested grid off the main thread and reuses it. Grids with no asset are empty. */
    suspend fun grid(type: GridType): GridData {
        val asset = type.asset ?: return GridData.EMPTY
        cache[type]?.let { return it }
        return withContext(Dispatchers.IO) {
            cache.getOrPut(type) { SphereGridParser.parse(readAsset(asset), type.idPrefix) }
        }
    }

    // --- Node content edits (shared across characters, keyed by grid-namespaced node id) ---

    fun observeOverrides(): Flow<Map<String, NodeContent>> =
        nodeDao.observeAll().map { rows ->
            rows.mapNotNull { row -> NodeContent.decode(row.content)?.let { row.nodeId to it } }.toMap()
        }

    suspend fun setContent(nodeId: String, content: NodeContent?, original: NodeContent) {
        if (content == null || content == original) {
            nodeDao.delete(nodeId)
        } else {
            nodeDao.upsert(SphereGridNodeEntity(nodeId = nodeId, content = content.encode()))
        }
    }

    /** Reverts every edited node on every grid back to vanilla. Activations are left alone. */
    suspend fun clearOverrides() = nodeDao.clearAll()

    // --- Per-character activation (each character's path) ---

    fun observeActivations(character: GridCharacter): Flow<Set<String>> =
        activationDao.observeForCharacter(character.name).map { it.toSet() }

    suspend fun setActivation(character: GridCharacter, nodeId: String, activated: Boolean) {
        if (activated) {
            activationDao.upsert(SphereGridActivationEntity(character.name, nodeId))
        } else {
            activationDao.delete(character.name, nodeId)
        }
    }

    suspend fun clearCharacterActivations(character: GridCharacter) =
        activationDao.clearCharacter(character.name)

    /** Full wipe used by the Settings "Reset all progress": both edits and every character's path. */
    suspend fun clearAll() {
        nodeDao.clearAll()
        activationDao.clearAll()
    }

    // --- Sharing: export/import a build as a copy/paste code ---

    /**
     * Builds a shareable code for the player's current work, carrying only the pieces [scope] asks
     * for: the shared content edits, and either the current [character]'s path or everyone's. The
     * grid shape itself is never exported - the recipient reconstructs it from the same bundled asset.
     */
    suspend fun exportBuild(scope: BuildScope, character: GridCharacter, gridType: GridType): String {
        val edits = if (scope.includesEdits) {
            nodeDao.snapshot().mapNotNull { row ->
                NodeContent.decode(row.content)?.let { row.nodeId to it }
            }.toMap()
        } else {
            null
        }

        val paths = when {
            !scope.includesAnyPath -> null
            scope.includesAllPaths -> activationDao.snapshot()
                .groupBy(
                    keySelector = { GridCharacter.entries.firstOrNull { c -> c.name == it.character } },
                    valueTransform = { it.nodeId }
                )
                .mapNotNull { (c, ids) -> c?.let { it to ids.toSet() } }
                .toMap()
            else -> mapOf(character to observeCharacterSnapshot(character))
        }

        return SphereGridBuildCodec.encode(SphereGridBuild(gridType, edits, paths))
    }

    /**
     * Applies a build code, replacing (not merging) whichever sections it carries so the recipient's
     * grid and paths end up identical to the sharer's. Node ids not present on [gridType]'s grid are
     * dropped. The whole apply runs in one transaction, so a partial import can never be observed.
     */
    suspend fun importBuild(text: String, gridType: GridType): Result<ImportSummary> {
        val build = SphereGridBuildCodec.decode(text).getOrElse { return Result.failure(it) }

        val validIds = grid(gridType).nodes.mapTo(HashSet()) { it.id }
        val edits = build.edits?.filterKeys { it in validIds }
        val paths = build.paths?.mapValues { (_, ids) -> ids.filter { it in validIds } }

        if (edits == null && paths == null) {
            return Result.failure(IllegalArgumentException("This build code has nothing to import."))
        }

        database.withTransaction {
            if (edits != null) {
                nodeDao.clearAll()
                nodeDao.upsertAll(
                    edits.map { (id, content) -> SphereGridNodeEntity(id, content.encode()) }
                )
            }
            paths?.forEach { (character, ids) ->
                activationDao.clearCharacter(character.name)
                activationDao.upsertAll(ids.map { SphereGridActivationEntity(character.name, it) })
            }
        }

        return Result.success(
            ImportSummary(
                editCount = edits?.size,
                pathCounts = paths?.mapValues { it.value.size }
            )
        )
    }

    private suspend fun observeCharacterSnapshot(character: GridCharacter): Set<String> =
        activationDao.snapshot().filter { it.character == character.name }.mapTo(HashSet()) { it.nodeId }

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader().use { it.readText() }

    /** What an import changed, for a confirmation message. Null sections were not part of the build. */
    data class ImportSummary(
        val editCount: Int?,
        val pathCounts: Map<GridCharacter, Int>?
    )
}
