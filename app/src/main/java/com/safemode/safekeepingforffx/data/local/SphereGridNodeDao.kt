package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridNodeDao {

    @Query("SELECT * FROM sphere_grid_node")
    fun observeAll(): Flow<List<SphereGridNodeEntity>>

    @Upsert
    suspend fun upsert(entity: SphereGridNodeEntity)

    @Query("DELETE FROM sphere_grid_node WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)

    @Query("DELETE FROM sphere_grid_node")
    suspend fun clearAll()
}
