package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridActivationDao {

    @Query("SELECT nodeId FROM sphere_grid_activation WHERE character = :character")
    fun observeForCharacter(character: String): Flow<List<String>>

    @Upsert
    suspend fun upsert(entity: SphereGridActivationEntity)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character AND nodeId = :nodeId")
    suspend fun delete(character: String, nodeId: String)

    @Query("DELETE FROM sphere_grid_activation WHERE character = :character")
    suspend fun clearCharacter(character: String)

    @Query("DELETE FROM sphere_grid_activation")
    suspend fun clearAll()
}
