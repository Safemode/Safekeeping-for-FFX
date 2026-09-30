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

    /** The whole library in one read, oldest first, for writing a backup file. */
    @Query("SELECT * FROM sphere_grid_route ORDER BY createdAt")
    suspend fun snapshot(): List<SphereGridRouteEntity>

    @Query("SELECT * FROM sphere_grid_route WHERE id = :id")
    suspend fun get(id: Long): SphereGridRouteEntity?

    @Insert
    suspend fun insert(route: SphereGridRouteEntity): Long

    @Insert
    suspend fun insertAll(routes: List<SphereGridRouteEntity>)

    @Query("UPDATE sphere_grid_route SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    /** Overwrites a route's build (and its grid label) in place, bumping its updated time. */
    @Query(
        "UPDATE sphere_grid_route SET gridType = :gridType, payload = :payload, " +
            "updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateContent(id: Long, gridType: String, payload: String, updatedAt: Long)

    @Query("DELETE FROM sphere_grid_route WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sphere_grid_route")
    suspend fun clearAll()
}
