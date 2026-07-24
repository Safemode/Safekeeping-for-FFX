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
import com.safemode.safekeepingforffx.data.reference.BASE_STATS_ASSET
import com.safemode.safekeepingforffx.data.reference.BaseStats
import com.safemode.safekeepingforffx.data.reference.BaseStatsCsvParser
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

    @Volatile
    private var baseStatsCache: Map<GridCharacter, BaseStats>? = null

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

    /**
     * Every character's starting stats, parsed off the main thread once and reused. Reference data
     * like the grid itself - the player never edits it, so there is nothing to observe.
     */
    suspend fun baseStats(): Map<GridCharacter, BaseStats> =
        baseStatsCache ?: withContext(Dispatchers.IO) {
            BaseStatsCsvParser.parse(readAsset(BASE_STATS_ASSET)).also { baseStatsCache = it }
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

    /**
     * Activates every node on [gridType] that holds a stat or an ability for [character], so the
     * planner can show what their totals look like with the whole grid taken. Locks and blank nodes
     * are skipped - neither gives anything, and a lock is a gate rather than a gain.
     *
     * Content is read the same way the canvas reads it: a player edit if there is one, else the
     * node's vanilla content, so a node edited into a stat counts and one blanked out does not.
     * Written as a single upsert rather than a tap at a time, since this is several hundred rows.
     * Returns how many nodes were activated, for the confirmation message.
     */
    suspend fun activateContentNodes(character: GridCharacter, gridType: GridType): Int {
        val grid = grid(gridType)
        val overrides = nodeDao.snapshot().mapNotNull { row ->
            NodeContent.decode(row.content)?.let { row.nodeId to it }
        }.toMap()

        val targets = grid.nodes.filter { node ->
            when (overrides[node.id] ?: node.original) {
                is NodeContent.Attribute, is NodeContent.Ability -> true
                else -> false
            }
        }.map { it.id }
        if (targets.isEmpty()) return 0

        seqMutex.withLock {
            var seq = nextSeq()
            activationDao.upsertAll(
                targets.map { SphereGridActivationEntity(character.name, it, seq++) }
            )
        }
        return targets.size
    }

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
     * asks for. The route is the path in activation order (each character's activations come off the
     * `ORDER BY seq` snapshot), with each node's edit placed right before that node is first taken -
     * so an unlocked-then-filled node reads as "set to X, then activate X".
     *
     * The interleaving is derived from node identity, not from comparing edit seqs against activation
     * seqs. The two tables keep independent seq ranges, so comparing across them can't be trusted (and
     * once produced routes where every edit clustered ahead of the whole path); anchoring each edit to
     * its node's activation avoids that entirely.
     */
    private suspend fun currentBuild(
        scope: BuildScope,
        character: GridCharacter,
        gridType: GridType
    ): SphereGridBuild {
        // Edits and activations are keyed by grid-namespaced node id but not otherwise scoped to a
        // grid, so a character carries a path on each grid at once. Keep only nodes that belong to
        // this grid, or an Expert route would sweep in the same character's Standard path (and vice
        // versa) and replay it as phantom "blank" nodes.
        val gridNodeIds = grid(gridType).nodes.mapTo(HashSet()) { it.id }

        // Edits are grid-wide, so a node has at most one; keep them in seq order for a stable lead.
        val editByNode = LinkedHashMap<String, RouteEvent.Edit>()
        if (scope.includesEdits) {
            nodeDao.snapshot().forEach { row ->
                if (row.nodeId in gridNodeIds) {
                    NodeContent.decode(row.content)?.let { editByNode[row.nodeId] = RouteEvent.Edit(row.nodeId, it) }
                }
            }
        }

        val activationRows = when {
            !scope.includesAnyPath -> emptyList()
            scope.includesAllPaths -> activationDao.snapshot()
            else -> activationDao.snapshot().filter { it.character == character.name }
        }.filter { it.nodeId in gridNodeIds }
        val activatedNodeIds = activationRows.mapTo(HashSet()) { it.nodeId }

        val events = ArrayList<RouteEvent>()
        val emitted = HashSet<String>()
        // Edits on nodes nobody takes in this scope lead the route as grid setup.
        editByNode.values.forEach { edit ->
            if (edit.nodeId !in activatedNodeIds) {
                events.add(edit)
                emitted.add(edit.nodeId)
            }
        }
        // Then the path, each activation preceded by its node's edit the first time the node is taken.
        activationRows.forEach { row ->
            val activationCharacter = GridCharacter.entries.firstOrNull { it.name == row.character }
                ?: return@forEach
            editByNode[row.nodeId]?.let { edit ->
                if (emitted.add(edit.nodeId)) events.add(edit)
            }
            events.add(RouteEvent.Activate(activationCharacter, row.nodeId))
        }
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

    /**
     * The decoded route for replay, or null if the row is missing or its payload is unreadable. Events
     * are pruned to nodes that exist on the route's grid, so a route saved before the grid-scoping fix
     * - which may carry the same character's path from the other grid - replays clean without editing.
     */
    suspend fun routeBuild(id: Long): SphereGridBuild? {
        val build = routeDao.get(id)?.let { SphereGridBuildCodec.decode(it.payload).getOrNull() }
            ?: return null
        val gridNodeIds = grid(build.gridType).nodes.mapTo(HashSet()) { it.id }
        return build.copy(events = build.events.filter { it.nodeId in gridNodeIds })
    }

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
