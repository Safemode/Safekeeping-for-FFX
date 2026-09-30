package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridActivationDao {

    /**
     * The character's activated nodes, most recently taken first, so the caller can both hold the
     * whole path and know where they last worked. Rows from before the routes feature all carry
     * seq 0 ("order unknown") and fall to the back in a stable id order.
     */
    @Query(
        "SELECT nodeId FROM sphere_grid_activation WHERE character = :character " +
            "ORDER BY seq DESC, nodeId DESC"
    )
    fun observeForCharacter(character: String): Flow<List<String>>

    /** Every character's activations in the order they happened, for exporting an ordered route. */
    @Query("SELECT * FROM sphere_grid_activation ORDER BY seq")
    suspend fun snapshot(): List<SphereGridActivationEntity>

    /**
     * The distinct characters with any activation on the grid whose node ids start with [prefix]
     * (see `GridType.idPrefix`). Lets the save dialog tell single- from multi-character work on the
     * grid in view without loading every path.
     */
    @Query("SELECT DISTINCT character FROM sphere_grid_activation WHERE nodeId LIKE :prefix || '%'")
    fun observeCharactersOnGrid(prefix: String): Flow<List<String>>

    /** Highest activation seq, or null if none. Paired with the edit table's max for the next seq. */
    @Query("SELECT MAX(seq) FROM sphere_grid_activation")
    suspend fun maxSeq(): Long?

    @Upsert
    suspend fun upsert(entity: SphereGridActivationEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SphereGridActivationEntity>)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character AND nodeId = :nodeId")
    suspend fun delete(character: String, nodeId: String)

    /** Removes a node from every character's path, e.g. when a gate is re-locked. */
    @Query("DELETE FROM sphere_grid_activation WHERE nodeId = :nodeId")
    suspend fun deleteNode(nodeId: String)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character")
    suspend fun clearCharacter(character: String)

    @Query("DELETE FROM sphere_grid_activation")
    suspend fun clearAll()
}
