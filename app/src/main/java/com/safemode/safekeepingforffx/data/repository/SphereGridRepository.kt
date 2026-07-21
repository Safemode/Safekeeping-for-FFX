package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.safemode.safekeepingforffx.data.local.FfxDatabase
import com.safemode.safekeepingforffx.data.local.SphereGridActivationDao
import com.safemode.safekeepingforffx.data.local.SphereGridActivationEntity
import com.safemode.safekeepingforffx.data.local.SphereGridNodeDao
import com.safemode.safekeepingforffx.data.local.SphereGridNodeEntity
import com.safemode.safekeepingforffx.data.local.SphereGridRouteDao
import com.safemode.safekeepingforffx.data.local.SphereGridRouteEntity
import com.safemode.safekeepingforffx.data.reference.BuildScope
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridBuildCodec
import com.safemode.safekeepingforffx.data.reference.SphereGridParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val activationDao: SphereGridActivationDao,
    private val routeDao: SphereGridRouteDao
) {
    private val cache = ConcurrentHashMap<GridType, GridData>()

    /**
     * Serializes the read-then-write of a new [seq]. Each node tap launches its own coroutine, so two
     * quick taps can both read the same max before either writes and land on the same seq - which
     * sorts arbitrarily and scrambles a saved route's order. Holding this across compute-and-write
     * keeps every activation and edit on a strictly increasing timeline.
     */
    private val seqMutex = Mutex()

    /**
     * The next value on the shared edit/activation timeline: one past the highest [seq] in either
     * table, so a new edit or activation always sorts after everything already done. Call only while
     * holding [seqMutex], and write the row before releasing it.
     */
    private suspend fun nextSeq(): Long =
        maxOf(nodeDao.maxSeq() ?: 0L, activationDao.maxSeq() ?: 0L) + 1

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
            seqMutex.withLock {
                nodeDao.upsert(
                    SphereGridNodeEntity(nodeId = nodeId, content = content.encode(), seq = nextSeq())
                )
            }
        }
    }

    /** Reverts every edited node on every grid back to vanilla. Activations are left alone. */
    suspend fun clearOverrides() = nodeDao.clearAll()

    // --- Per-character activation (each character's path) ---

    fun observeActivations(character: GridCharacter): Flow<Set<String>> =
        activationDao.observeForCharacter(character.name).map { it.toSet() }

    suspend fun setActivation(character: GridCharacter, nodeId: String, activated: Boolean) {
        if (activated) {
            seqMutex.withLock {
                activationDao.upsert(
                    SphereGridActivationEntity(character.name, nodeId, seq = nextSeq())
                )
            }
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
    suspend fun exportBuild(scope: BuildScope, character: GridCharacter, gridType: GridType): String =
        SphereGridBuildCodec.encode(currentBuild(scope, character, gridType))

    /**
     * Snapshots the player's current work as a [SphereGridBuild], carrying only the pieces [scope]
     * asks for. Edits and activations are read off the `ORDER BY seq` snapshots and merged back into
     * one timeline by that shared seq, so the events come out interleaved in the order they were done
     * - what makes this a *route*, not just a state.
     */
    private suspend fun currentBuild(
        scope: BuildScope,
        character: GridCharacter,
        gridType: GridType
    ): SphereGridBuild {
        val editEvents: List<Pair<Long, RouteEvent>> = if (scope.includesEdits) {
            nodeDao.snapshot().mapNotNull { row ->
                NodeContent.decode(row.content)?.let { row.seq to RouteEvent.Edit(row.nodeId, it) }
            }
        } else {
            emptyList()
        }

        val activationRows = when {
            !scope.includesAnyPath -> emptyList()
            scope.includesAllPaths -> activationDao.snapshot()
            else -> activationDao.snapshot().filter { it.character == character.name }
        }
        val activationEvents: List<Pair<Long, RouteEvent>> = activationRows.mapNotNull { row ->
            GridCharacter.entries.firstOrNull { it.name == row.character }
                ?.let { row.seq to RouteEvent.Activate(it, row.nodeId) }
        }

        val events = (editEvents + activationEvents).sortedBy { it.first }.map { it.second }
        return SphereGridBuild(gridType, events)
    }

    /**
     * Applies a build code, replacing (not merging) the sections it carries so the recipient's grid
     * and paths end up matching the sharer's. Edits are replaced when the code carries any edit; a
     * character's path is replaced when the code carries any activation for them. Node ids not on
     * [gridType]'s grid are dropped. The whole apply runs in one transaction, so a partial import can
     * never be observed.
     */
    suspend fun importBuild(text: String, gridType: GridType): Result<ImportSummary> {
        val build = SphereGridBuildCodec.decode(text).getOrElse { return Result.failure(it) }
        return applyBuild(build, gridType)
    }

    /**
     * Replaces the player's live progress with [build]: edits are replaced when it carries any edit,
     * and a character's path is replaced when it carries any activation for them. This is what "make
     * this route my live progress" does. Node ids not on [gridType]'s grid are dropped, and the whole
     * apply runs in one transaction so a partial state can never be observed.
     */
    suspend fun applyBuild(build: SphereGridBuild, gridType: GridType): Result<ImportSummary> {
        val validIds = grid(gridType).nodes.mapTo(HashSet()) { it.id }
        val events = build.events.filter { it.nodeId in validIds }

        if (events.isEmpty()) {
            return Result.failure(IllegalArgumentException("This build has nothing to apply."))
        }

        val hasEdits = events.any { it is RouteEvent.Edit }
        val characters = events.filterIsInstance<RouteEvent.Activate>().mapTo(HashSet()) { it.character }

        database.withTransaction {
            if (hasEdits) nodeDao.clearAll()
            characters.forEach { activationDao.clearCharacter(it.name) }
            // One increasing counter across the whole apply keeps the replayed timeline in order.
            var seq = 0L
            events.forEach { event ->
                seq += 1
                when (event) {
                    is RouteEvent.Edit ->
                        nodeDao.upsert(SphereGridNodeEntity(event.nodeId, event.content.encode(), seq))
                    is RouteEvent.Activate ->
                        activationDao.upsert(
                            SphereGridActivationEntity(event.character.name, event.nodeId, seq)
                        )
                }
            }
        }

        val pathCounts = events.filterIsInstance<RouteEvent.Activate>()
            .groupingBy { it.character }.eachCount()
        return Result.success(
            ImportSummary(
                editCount = events.count { it is RouteEvent.Edit }.takeIf { hasEdits },
                pathCounts = pathCounts.takeIf { it.isNotEmpty() }
            )
        )
    }

    // --- Saved routes library: name, keep, share and replay ordered routes ---

    /** The saved routes library, newest first, decoded to lightweight summaries for the list UI. */
    fun observeRoutes(): Flow<List<SavedRoute>> =
        routeDao.observeAll().map { rows -> rows.mapNotNull { it.toSummary() } }

    /** Saves the player's current work (per [scope]) as a named route in the library. */
    suspend fun saveCurrentAsRoute(
        name: String,
        scope: BuildScope,
        character: GridCharacter,
        gridType: GridType
    ) {
        val build = currentBuild(scope, character, gridType).copy(name = name)
        routeDao.insert(
            SphereGridRouteEntity(
                name = name,
                gridType = gridType.name,
                createdAt = System.currentTimeMillis(),
                payload = SphereGridBuildCodec.encode(build)
            )
        )
    }

    /** Saves a pasted route code into the library, using its own name unless [name] overrides it. */
    suspend fun saveImportedRoute(name: String, code: String): Result<Unit> {
        val build = SphereGridBuildCodec.decode(code).getOrElse { return Result.failure(it) }
        val label = name.ifBlank { build.name ?: "Imported route" }
        routeDao.insert(
            SphereGridRouteEntity(
                name = label,
                gridType = build.gridType.name,
                createdAt = System.currentTimeMillis(),
                payload = code.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun renameRoute(id: Long, name: String) = routeDao.rename(id, name)

    suspend fun deleteRoute(id: Long) = routeDao.delete(id)

    suspend fun clearRoutes() = routeDao.clearAll()

    /** The route's shareable code, for the library's Share action. */
    suspend fun routeCode(id: Long): String? = routeDao.get(id)?.payload

    /** The decoded route for replay, or null if the row is missing or its payload is unreadable. */
    suspend fun routeBuild(id: Long): SphereGridBuild? =
        routeDao.get(id)?.let { SphereGridBuildCodec.decode(it.payload).getOrNull() }

    private fun SphereGridRouteEntity.toSummary(): SavedRoute? {
        val build = SphereGridBuildCodec.decode(payload).getOrNull() ?: return null
        val gridType = GridType.entries.firstOrNull { it.name == this.gridType } ?: return null
        return SavedRoute(
            id = id,
            name = name,
            gridType = gridType,
            createdAt = createdAt,
            editCount = build.events.count { it is RouteEvent.Edit },
            pathCounts = build.events.filterIsInstance<RouteEvent.Activate>()
                .groupingBy { it.character }.eachCount()
        )
    }

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader().use { it.readText() }

    /** What an import changed, for a confirmation message. Null sections were not part of the build. */
    data class ImportSummary(
        val editCount: Int?,
        val pathCounts: Map<GridCharacter, Int>?
    )

    /** A row in the saved routes library, decoded enough to list and label without opening it. */
    data class SavedRoute(
        val id: Long,
        val name: String,
        val gridType: GridType,
        val createdAt: Long,
        val editCount: Int,
        val pathCounts: Map<GridCharacter, Int>
    )
}
