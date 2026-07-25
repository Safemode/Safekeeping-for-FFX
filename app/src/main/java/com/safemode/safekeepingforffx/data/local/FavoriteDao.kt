package com.safemode.safekeepingforffx.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    /** Every favorite, for the Favorites screen. Oldest first within each category. */
    @Query("SELECT * FROM favorite ORDER BY categoryId, createdAt")
    fun observeAll(): Flow<List<FavoriteEntity>>

    /**
     * Just the ids for one list, which is all a row needs to draw its star. Narrowed in SQL so a
     * category's screen isn't woken by a star tapped in a different one.
     */
    @Query("SELECT itemId FROM favorite WHERE categoryId = :categoryId")
    fun observeCategory(categoryId: String): Flow<List<String>>

    /** Every favorite in one read, for writing a backup file. */
    @Query("SELECT * FROM favorite ORDER BY categoryId, createdAt")
    suspend fun snapshot(): List<FavoriteEntity>

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorite WHERE categoryId = :categoryId AND itemId = :itemId")
    suspend fun delete(categoryId: String, itemId: String)

    @Query("DELETE FROM favorite")
    suspend fun clearAll()
}
