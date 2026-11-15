package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PointRouteDao {

    /** The library, newest first. */
    @Query("SELECT * FROM point_route ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PointRouteEntity>>

    /** The whole library in one read, oldest first, for writing a backup file. */
    @Query("SELECT * FROM point_route ORDER BY createdAt")
    suspend fun snapshot(): List<PointRouteEntity>

    @Query("SELECT * FROM point_route WHERE id = :id")
    suspend fun get(id: Long): PointRouteEntity?

    @Insert
    suspend fun insert(route: PointRouteEntity): Long

    @Insert
    suspend fun insertAll(routes: List<PointRouteEntity>)

    @Query("UPDATE point_route SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM point_route WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM point_route")
    suspend fun clearAll()
}
