package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistProgressDao {

    @Query("SELECT * FROM checklist_progress WHERE categoryId = :categoryId")
    fun observeCategory(categoryId: String): Flow<List<ChecklistProgressEntity>>

    /** Every category's progress in one read, for writing a backup file. */
    @Query("SELECT * FROM checklist_progress ORDER BY categoryId, itemId")
    suspend fun snapshot(): List<ChecklistProgressEntity>

    @Upsert
    suspend fun upsert(entity: ChecklistProgressEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ChecklistProgressEntity>)

    @Query("DELETE FROM checklist_progress WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: String)

    @Query("DELETE FROM checklist_progress")
    suspend fun clearAll()
}
