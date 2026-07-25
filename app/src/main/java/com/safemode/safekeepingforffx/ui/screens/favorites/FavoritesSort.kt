package com.safemode.safekeepingforffx.ui.screens.favorites

import com.safemode.safekeepingforffx.ui.components.SortOption

/**
 * Where the chosen order is stored. A plain constant rather than the Favorites route, because this
 * is a storage key: renaming the destination should not silently lose everyone's choice.
 */
const val FAVORITES_SORT_KEY = "favorites"

/**
 * How the Favorites screen orders itself.
 *
 * [GROUPED] answers "what have I starred in the Monster Arena?", [RECENT] answers "what was I
 * looking at just now?". The second is the one that matters when you star three things while
 * reading about a fight and want them back the moment you put the phone down, which is why it
 * ignores the lists entirely.
 */
enum class FavoritesSort(
    override val label: String,
    override val description: String
) : SortOption {
    GROUPED("Grouped", "By the list each one came from"),
    RECENT("Recently added", "Newest star first, across every list");

    companion object {
        val DEFAULT = GROUPED

        /**
         * Resolves what was saved. Stored by name so reordering this enum can't change what an
         * existing install reads, and an unknown name falls back to [DEFAULT] rather than failing.
         */
        fun fromStored(name: String?): FavoritesSort =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Puts the resolved favorites in the order [sort] asks for.
 *
 * The input arrives grouped in drawer order with each list oldest-first, which is already what
 * [FavoritesSort.GROUPED] wants, so that case is the identity. [FavoritesSort.RECENT] throws the
 * grouping away and sorts across every list at once - the point of it is that you stopped caring
 * which list a thing came from.
 */
internal fun List<FavoriteEntry>.inOrder(sort: FavoritesSort): List<FavoriteEntry> = when (sort) {
    FavoritesSort.GROUPED -> this
    FavoritesSort.RECENT -> sortedByDescending { it.addedAt }
}
