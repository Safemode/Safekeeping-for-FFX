package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ASSET_NAME = "item_list.csv"

/**
 * Loads the item list from the bundled CSV.
 *
 * Unlike the other categories this one is not written in Kotlin, so that the list can be edited as
 * data. Small enough to parse in one go and cache.
 */
class ItemListRepository(private val assets: AssetManager) {

    @Volatile
    private var cached: ChecklistCategory? = null

    /** Parsed once and reused; safe to call from anywhere. */
    suspend fun load(): ChecklistCategory {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: ItemListCsvParser.parse(readAsset()).also { cached = it }
        }
    }

    private fun readAsset(): String =
        assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
}
