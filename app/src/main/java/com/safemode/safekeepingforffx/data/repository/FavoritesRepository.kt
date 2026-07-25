package com.safemode.safekeepingforffx.data.repository

import com.safemode.safekeepingforffx.data.local.FavoriteDao
import com.safemode.safekeepingforffx.data.local.FavoriteEntity
import com.safemode.safekeepingforffx.domain.Favorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The starred items, as keys only.
 *
 * Deliberately knows nothing about what an item *is*. Favorites cut across lists that store their
 * reference data in completely different places - Kotlin objects, two different CSVs - and teaching
 * one repository to read all of them would tie it to every category in the app. Resolving a key back
 * to something displayable is the Favorites screen's job, which already has those sources to hand.
 */
class FavoritesRepository(private val dao: FavoriteDao) {

    /** Every favorite in the app, oldest first within each category. */
    fun observeAll(): Flow<List<Favorite>> =
        dao.observeAll().map { rows ->
            rows.map { Favorite(it.categoryId, it.itemId, it.createdAt) }
        }

    /**
     * The starred ids in one list, which is all its rows need to draw their stars. A set rather than
     * a list because rows ask "am I in it?" and never care about the order.
     */
    fun observeCategory(categoryId: String): Flow<Set<String>> =
        dao.observeCategory(categoryId)
            .map { it.toSet() }
            // Room re-runs the query on any write to the table, including one that leaves this
            // category's answer identical. Without this, starring something in another list would
            // rebuild this one.
            .distinctUntilChanged()

    suspend fun setFavorite(categoryId: String, itemId: String, favorite: Boolean) {
        if (favorite) {
            dao.upsert(
                FavoriteEntity(
                    categoryId = categoryId,
                    itemId = itemId,
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.delete(categoryId, itemId)
        }
    }

    /**
     * Unstars everything, everywhere. Irreversible - callers must confirm with the user first.
     */
    suspend fun clearAll() = dao.clearAll()
}
