package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistProgressDao {

    @Query("SELECT * FROM checklist_progress WHERE categoryId = :categoryId")
    fun observeCategory(categoryId: String): Flow<List<ChecklistProgressEntity>>

    @Upsert
    suspend fun upsert(entity: ChecklistProgressEntity)

    @Query("DELETE FROM checklist_progress WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: String)

    @Query("DELETE FROM checklist_progress")
    suspend fun clearAll()
}
