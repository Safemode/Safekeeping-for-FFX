package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import com.safemode.safekeepingforffx.data.local.MonsterCaptureDao
import com.safemode.safekeepingforffx.data.local.MonsterCaptureEntity
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.MonsterArenaCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
     * Clamped to 0..[MAX_CAPTURES] here rather than at the UI, so no caller can write a count the
     * game could not produce. Zero deletes the row instead of storing it.
     */
    suspend fun setCount(monsterId: String, count: Int) {
        val clamped = count.coerceIn(0, MAX_CAPTURES)
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

    suspend fun clearAll() = dao.clearAll()

    private fun readAsset(): String =
        assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
}
