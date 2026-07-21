package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import com.safemode.safekeepingforffx.data.local.SphereGridActivationDao
import com.safemode.safekeepingforffx.data.local.SphereGridActivationEntity
import com.safemode.safekeepingforffx.data.local.SphereGridNodeDao
import com.safemode.safekeepingforffx.data.local.SphereGridNodeEntity
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridData
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
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
    private val nodeDao: SphereGridNodeDao,
    private val activationDao: SphereGridActivationDao
) {
    private val cache = ConcurrentHashMap<GridType, GridData>()

    /** Parses the requested grid off the main thread and reuses it. Grids with no asset are empty. */
    suspend fun grid(type: GridType): GridData {
        val asset = type.asset ?: return GridData.EMPTY
        cache[type]?.let { return it }
        return withContext(Dispatchers.IO) {
            cache.getOrPut(type) { SphereGridParser.parse(readAsset(asset)) }
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

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader().use { it.readText() }
}
