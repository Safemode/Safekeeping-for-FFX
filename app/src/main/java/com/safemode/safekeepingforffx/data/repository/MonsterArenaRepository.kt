package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import com.safemode.safekeepingforffx.data.local.MonsterCaptureDao
import com.safemode.safekeepingforffx.data.local.MonsterCaptureEntity
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import com.safemode.safekeepingforffx.domain.ProgressCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val ASSET_NAME = "monster_arena.csv"

/**
 * The fiend list comes from a bundled CSV; the capture counts come from the database. Reference
 * data and player progress stay separate, exactly as they do for the checklists.
 */
class MonsterArenaRepository(
    private val assets: AssetManager,
    private val dao: MonsterCaptureDao
) {
    @Volatile
    private var cached: List<Monster>? = null

    /** Parsed once and reused. */
    suspend fun monsters(): List<Monster> {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: MonsterArenaCsvParser.parse(readAsset()).also { cached = it }
        }
    }

    /** Capture counts by monster id. A fiend with no row has been captured zero times. */
    fun observeCaptures(): Flow<Map<String, Int>> =
        dao.observeAll().map { rows -> rows.associate { it.monsterId to it.count } }

    /**
     * Fully captured fiends out of every capturable one - creations are unlocked rather than
     * captured, so they have no count to complete. Emits only once the fiend list has parsed, so a
     * count never flashes "0 / 0". Shared by Home and the drawer.
     */
    fun observeCaptureProgress(): Flow<ProgressCount> = flow {
        val capturable = runCatching { monsters() }
            .getOrDefault(emptyList())
            .filter { it.isCapturable }
        emitAll(
            observeCaptures().map { counts ->
                ProgressCount(
                    found = capturable.count { (counts[it.id] ?: 0) >= MAX_CAPTURES },
                    total = capturable.size
                )
            }
        )
    }

    /**
     * Serializes every read-then-write of a count. Each +/- tap launches its own coroutine, so two
     * quick taps computed from the count on screen would both write the same value and one capture
     * would be lost. Holding this across read and write makes each change apply on top of the last.
     */
    private val writeMutex = Mutex()

    /**
     * Moves one fiend's count by [delta], starting from the stored count rather than whatever the
     * screen last showed. Clamped to 0..[MAX_CAPTURES] here rather than at the UI, so no caller can
     * write a count the game could not produce. Zero deletes the row instead of storing it.
     */
    suspend fun adjustCount(monsterId: String, delta: Int) = writeMutex.withLock {
        val clamped = ((dao.getCount(monsterId) ?: 0) + delta).coerceIn(0, MAX_CAPTURES)
        if (clamped == 0) {
            dao.delete(monsterId)
        } else {
            dao.upsert(
                MonsterCaptureEntity(
                    monsterId = monsterId,
                    count = clamped,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Raises each fiend in [targets] to at least its required count in a single write, never
     * lowering a count that is already higher - so a creation's auto-capture unlocks it without
     * undoing progress the player already made towards another. Counts are clamped to
     * 0..[MAX_CAPTURES] here, same as [adjustCount].
     */
    suspend fun captureAtLeast(targets: Map<String, Int>) = writeMutex.withLock {
        if (targets.isEmpty()) return@withLock
        val current = dao.getAll().associate { it.monsterId to it.count }
        val now = System.currentTimeMillis()
        val updates = targets.mapNotNull { (monsterId, needed) ->
            val target = needed.coerceIn(0, MAX_CAPTURES)
            if (target <= (current[monsterId] ?: 0)) {
                null
            } else {
                MonsterCaptureEntity(monsterId = monsterId, count = target, updatedAt = now)
            }
        }
        if (updates.isNotEmpty()) dao.upsertAll(updates)
    }

    suspend fun clearAll() = dao.clearAll()

    private fun readAsset(): String =
        assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
}
