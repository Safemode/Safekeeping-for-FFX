package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridActivationDao {

    @Query("SELECT nodeId FROM sphere_grid_activation WHERE character = :character")
    fun observeForCharacter(character: String): Flow<List<String>>

    /** One-shot read of every character's activations, for exporting a build. */
    @Query("SELECT * FROM sphere_grid_activation")
    suspend fun snapshot(): List<SphereGridActivationEntity>

    @Upsert
    suspend fun upsert(entity: SphereGridActivationEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SphereGridActivationEntity>)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character AND nodeId = :nodeId")
    suspend fun delete(character: String, nodeId: String)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character")
    suspend fun clearCharacter(character: String)

    @Query("DELETE FROM sphere_grid_activation")
    suspend fun clearAll()
}
