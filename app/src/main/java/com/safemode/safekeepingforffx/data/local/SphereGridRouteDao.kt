package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SphereGridRouteDao {

    /** The library, newest first. */
    @Query("SELECT * FROM sphere_grid_route ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SphereGridRouteEntity>>

    @Query("SELECT * FROM sphere_grid_route WHERE id = :id")
    suspend fun get(id: Long): SphereGridRouteEntity?

    @Insert
    suspend fun insert(route: SphereGridRouteEntity): Long

    @Query("UPDATE sphere_grid_route SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM sphere_grid_route WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sphere_grid_route")
    suspend fun clearAll()
}
