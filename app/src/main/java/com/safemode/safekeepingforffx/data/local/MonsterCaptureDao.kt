package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterCaptureDao {

    @Query("SELECT * FROM monster_capture")
    fun observeAll(): Flow<List<MonsterCaptureEntity>>

    /** One-shot read, used by the bulk auto-capture to avoid lowering counts already higher. */
    @Query("SELECT * FROM monster_capture")
    suspend fun getAll(): List<MonsterCaptureEntity>

    @Upsert
    suspend fun upsert(entity: MonsterCaptureEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MonsterCaptureEntity>)

    @Query("DELETE FROM monster_capture WHERE monsterId = :monsterId")
    suspend fun delete(monsterId: String)

    @Query("DELETE FROM monster_capture")
    suspend fun clearAll()
}
