package com.safemode.safekeepingforffx.data.repository

import android.content.res.AssetManager
import com.safemode.safekeepingforffx.data.reference.MixCsvParser
import com.safemode.safekeepingforffx.data.reference.MixTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ASSET_NAME = "mix_recipes.csv"

/**
 * Loads the Mix table from the bundled CSV.
 *
 * The table is far too large to live in Kotlin source - a list literal of that size overruns the
 * JVM's 64KB method limit in the class initialiser - so it ships as an asset and is parsed once.
 */
class MixRepository(private val assets: AssetManager) {

    @Volatile
    private var cached: MixTable? = null

    /** Parsed once and reused; safe to call from anywhere. */
    suspend fun load(): MixTable {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: MixCsvParser.parse(readAsset()).also { cached = it }
        }
    }

    private fun readAsset(): String =
        assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
}
