package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridNodeDao {

    @Query("SELECT * FROM sphere_grid_node")
    fun observeAll(): Flow<List<SphereGridNodeEntity>>

    /** Every edited node in the order it was edited, for exporting an ordered route. */
    @Query("SELECT * FROM sphere_grid_node ORDER BY seq")
    suspend fun snapshot(): List<SphereGridNodeEntity>

    /** Highest edit seq, or null if none. Paired with the activation table's max for the next seq. */
    @Query("SELECT MAX(seq) FROM sphere_grid_node")
    suspend fun maxSeq(): Long?

    @Upsert
    suspend fun upsert(entity: SphereGridNodeEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SphereGridNodeEntity>)

    @Query("DELETE FROM sphere_grid_node WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)

    @Query("DELETE FROM sphere_grid_node")
    suspend fun clearAll()
}
