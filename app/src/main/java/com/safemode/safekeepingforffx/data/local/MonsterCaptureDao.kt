package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterCaptureDao {

    @Query("SELECT * FROM monster_capture")
    fun observeAll(): Flow<List<MonsterCaptureEntity>>

    @Upsert
    suspend fun upsert(entity: MonsterCaptureEntity)

    @Query("DELETE FROM monster_capture WHERE monsterId = :monsterId")
    suspend fun delete(monsterId: String)

    @Query("DELETE FROM monster_capture")
    suspend fun clearAll()
}
