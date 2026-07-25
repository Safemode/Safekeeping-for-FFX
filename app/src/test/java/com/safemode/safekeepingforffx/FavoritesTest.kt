package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.local.FavoriteDao
import com.safemode.safekeepingforffx.data.local.FavoriteEntity
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.data.reference.CelestialWeapons
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.ItemListCsvParser
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.repository.FavoritesRepository
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.domain.forVersion
import com.safemode.safekeepingforffx.ui.navigation.FfxDestination
import com.safemode.safekeepingforffx.ui.navigation.drawerDestinations
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import com.safemode.safekeepingforffx.ui.screens.favorites.FAVORITES_SORT_KEY
import com.safemode.safekeepingforffx.ui.screens.favorites.FavoriteEntry
import com.safemode.safekeepingforffx.ui.screens.favorites.FavoritesSort
import com.safemode.safekeepingforffx.ui.screens.favorites.inOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Favorites cut across lists that otherwise know nothing about each other, so the things worth
 * pinning down are the seams: that a star lands in one list and not the others, and that the set of
 * lists offering stars is the set the feature was asked for.
 */
class FavoritesTest {

    private class FakeFavoriteDao(initial: List<FavoriteEntity> = emptyList()) : FavoriteDao {
        val rows = MutableStateFlow(initial)

        private fun sorted(list: List<FavoriteEntity>) =
            list.sortedWith(compareBy({ it.categoryId }, { it.createdAt }))

        override fun observeAll(): Flow<List<FavoriteEntity>> = rows.map { sorted(it) }

        override fun observeCategory(categoryId: String): Flow<List<String>> =
            rows.map { list -> sorted(list).filter { it.categoryId == categoryId }.map { it.itemId } }

        override suspend fun snapshot(): List<FavoriteEntity> = sorted(rows.value)

        override suspend fun upsert(entity: FavoriteEntity) {
            rows.update { list ->
                list.filterNot { it.categoryId == entity.categoryId && it.itemId == entity.itemId } +
                    entity
            }
        }

        override suspend fun upsertAll(entities: List<FavoriteEntity>) {
            entities.forEach { upsert(it) }
        }

        override suspend fun delete(categoryId: String, itemId: String) {
            rows.update { list ->
                list.filterNot { it.categoryId == categoryId && it.itemId == itemId }
            }
        }

        override suspend fun clearAll() {
            rows.value = emptyList()
        }
    }

    @Test
    fun `starring an item makes it a favorite of that list`() = runTest {
        val repository = FavoritesRepository(FakeFavoriteDao())

        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)

        assertEquals(
            setOf("celestial_tidus_crest"),
            repository.observeCategory(CelestialWeapons.CATEGORY_ID).first()
        )
    }

    @Test
    fun `unstarring takes it back out again`() = runTest {
        val repository = FavoritesRepository(FakeFavoriteDao())
        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)

        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", false)

        assertTrue(repository.observeCategory(CelestialWeapons.CATEGORY_ID).first().isEmpty())
        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun `starring the same item twice leaves one favorite, not two`() = runTest {
        // The row is keyed by (category, item), so a double tap has to be an upsert rather than a
        // second row - which would otherwise show the item twice on the Favorites screen.
        val repository = FavoritesRepository(FakeFavoriteDao())

        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)
        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)

        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun `a star in one list does not appear in another`() = runTest {
        // Item ids are only unique within a category, so the pair is what isolates them. Two lists
        // that happened to share an id would otherwise star each other's rows.
        val repository = FavoritesRepository(FakeFavoriteDao())

        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "shared_id", true)

        assertTrue(repository.observeCategory(MONSTER_ARENA_ID).first().isEmpty())
        assertEquals(setOf("shared_id"), repository.observeCategory(CelestialWeapons.CATEGORY_ID).first())
    }

    @Test
    fun `favorites from several lists are all kept`() = runTest {
        val repository = FavoritesRepository(FakeFavoriteDao())

        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)
        repository.setFavorite(MONSTER_ARENA_ID, "dingo", true)

        val all = repository.observeAll().first()
        assertEquals(2, all.size)
        assertEquals(
            setOf(CelestialWeapons.CATEGORY_ID, MONSTER_ARENA_ID),
            all.map { it.categoryId }.toSet()
        )
    }

    @Test
    fun `clearing removes every star in every list`() = runTest {
        val repository = FavoritesRepository(FakeFavoriteDao())
        repository.setFavorite(CelestialWeapons.CATEGORY_ID, "celestial_tidus_crest", true)
        repository.setFavorite(MONSTER_ARENA_ID, "dingo", true)

        repository.clearAll()

        assertTrue(repository.observeAll().first().isEmpty())
    }

    @Test
    fun `Favorites sits directly below Home in the drawer`() {
        assertEquals(FfxDestination.Home, drawerDestinations[0])
        assertEquals(FfxDestination.Favorites, drawerDestinations[1])
    }

    @Test
    fun `the Sphere Grid Planner and Mix Calculator offer no stars`() {
        // Both were excluded by name in the request: neither is a list of things you could collect,
        // so neither has anything a star would point at.
        val sources = favoriteSources.ordered.map { it.first }

        assertFalse(FfxDestination.SphereGrid.route in sources)
        assertFalse(FfxDestination.MixCalculator.route in sources)
    }

    @Test
    fun `every other list can be favorited`() {
        val sources = favoriteSources.ordered.map { it.first }

        // Every compiled-in category, plus the two whose contents come from CSV.
        favoriteSources.categories.forEach {
            assertTrue("${it.id} cannot be favorited", it.id in sources)
        }
        assertTrue(ItemListCsvParser.CATEGORY_ID in sources)
        assertTrue(MONSTER_ARENA_ID in sources)
    }

    @Test
    fun `Favorites is not a source of its own favorites`() {
        // It shows other lists' stars. Listing itself would let a favorite point at a favorite.
        assertFalse(FfxDestination.Favorites.route in favoriteSources.ordered.map { it.first })
        assertFalse(FfxDestination.Home.route in favoriteSources.ordered.map { it.first })
        assertFalse(FfxDestination.Settings.route in favoriteSources.ordered.map { it.first })
    }

    @Test
    fun `favorite groups read in the same order as the drawer`() {
        // The Favorites screen walks this list to build its groups, so this ordering is what stops
        // the sections shuffling around as items are starred and unstarred.
        val drawerOrder = drawerDestinations.map { it.route }
        val sourceOrder = favoriteSources.ordered.map { it.first }

        assertEquals(sourceOrder, drawerOrder.filter { it in sourceOrder })
    }

    @Test
    fun `each source carries the label its group is headed with`() {
        favoriteSources.ordered.forEach { (route, label) ->
            assertTrue("$route has no label", label.isNotBlank())
            assertEquals(
                label,
                drawerDestinations.first { it.route == route }.label
            )
        }
    }

    @Test
    fun `a guarded warning is dropped on the release with no Dark Aeons`() {
        // Moved out of the checklist ViewModel so Favorites applies the same rule; this is the test
        // that it still behaves the same in both places.
        val guarded = ChecklistItem(
            id = "x",
            title = "Moon Crest",
            location = "Besaid Beach",
            detail = "",
            caution = Caution.Guarded("Dark Valefor"),
            isChecked = false
        )

        assertNull(guarded.forVersion(GameVersion.ORIGINAL_PS2).caution)
        assertEquals(
            Caution.Guarded("Dark Valefor"),
            guarded.forVersion(GameVersion.INTERNATIONAL_HD).caution
        )
    }

    @Test
    fun `a missable warning survives every release`() {
        // Home is destroyed in all of them, so this one must never be filtered out.
        val missable = ChecklistItem(
            id = "x",
            title = "Al Bhed Primer XIX",
            location = "Home",
            detail = "",
            caution = Caution.Missable,
            isChecked = false
        )

        GameVersion.entries.forEach { version ->
            assertEquals(
                "Missable was dropped on $version",
                Caution.Missable,
                missable.forVersion(version).caution
            )
        }
    }

    private fun entry(category: String, id: String, addedAt: Long) = FavoriteEntry(
        categoryId = category,
        categoryLabel = category,
        item = ChecklistItem(
            id = id,
            title = id,
            location = "",
            detail = "",
            caution = null,
            isChecked = false,
            isFavorite = true
        ),
        addedAt = addedAt
    )

    /** As the ViewModel builds it: drawer order, oldest first inside each list. */
    private val grouped = listOf(
        entry("celestial_weapons", "crest", 300),
        entry("celestial_weapons", "sigil", 100),
        entry(MONSTER_ARENA_ID, "dingo", 200)
    )

    @Test
    fun `grouped order leaves the lists as they were built`() {
        // The input already arrives grouped in drawer order, so this case has nothing to do - and
        // must not quietly re-sort by time and split the groups apart.
        assertEquals(grouped, grouped.inOrder(FavoritesSort.GROUPED))
    }

    @Test
    fun `recently added puts the newest star on top`() {
        val ordered = grouped.inOrder(FavoritesSort.RECENT)

        assertEquals(listOf("crest", "dingo", "sigil"), ordered.map { it.item.id })
    }

    @Test
    fun `recently added mixes the lists together`() {
        // The whole point of it: an arena fiend starred after a weapon sorts above that weapon.
        val ordered = grouped.inOrder(FavoritesSort.RECENT)

        assertEquals(MONSTER_ARENA_ID, ordered[1].categoryId)
    }

    @Test
    fun `no favorite is lost or duplicated by either order`() {
        FavoritesSort.entries.forEach { sort ->
            val ordered = grouped.inOrder(sort)
            assertEquals("$sort changed the count", grouped.size, ordered.size)
            assertEquals("$sort lost an entry", grouped.toSet(), ordered.toSet())
        }
    }

    @Test
    fun `a saved Favorites order is read back by name`() {
        FavoritesSort.entries.forEach { sort ->
            assertEquals(sort, FavoritesSort.fromStored(sort.name))
        }
    }

    @Test
    fun `an unreadable Favorites order falls back to grouping`() {
        assertEquals(FavoritesSort.DEFAULT, FavoritesSort.fromStored(null))
        assertEquals(FavoritesSort.DEFAULT, FavoritesSort.fromStored("BY_NAME"))
        assertEquals(FavoritesSort.GROUPED, FavoritesSort.DEFAULT)
    }

    @Test
    fun `the Favorites order is stored under a key of its own`() = runTest {
        // Deliberately not a category id: Favorites is not a category, and reusing a real one would
        // mean the two lists fought over the same stored value.
        assertFalse(FAVORITES_SORT_KEY in favoriteSources.ordered.map { it.first })
    }

    @Test
    fun `an item is not favorited until it is starred`() {
        val item = ChecklistItem(
            id = "x",
            title = "Moon Crest",
            location = "Besaid Beach",
            detail = "",
            caution = null,
            isChecked = true
        )

        // Being ticked says nothing about being starred; the two are stored apart on purpose.
        assertFalse(item.isFavorite)
    }
}
