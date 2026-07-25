package com.safemode.safekeepingforffx.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.monsterType
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.FavoritesRepository
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.domain.Favorite
import com.safemode.safekeepingforffx.domain.forVersion
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One starred item, resolved: what to draw, and which list it belongs to. */
data class FavoriteEntry(
    val categoryId: String,
    val categoryLabel: String,
    val item: ChecklistItem,
    /** When it was starred. The whole basis of [FavoritesSort.RECENT]. */
    val addedAt: Long
)

data class FavoritesUiState(
    /**
     * Already in the order [sort] asks for, so the screen only has to decide where headers go. One
     * list rather than one shape per order: the rows are the same either way, and holding both would
     * mean keeping them in step.
     */
    val entries: List<FavoriteEntry> = emptyList(),
    val sort: FavoritesSort = FavoritesSort.DEFAULT,
    val isLoading: Boolean = true
) {
    val totalCount: Int get() = entries.size
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()
}

/**
 * The two lists whose contents are parsed from CSV rather than compiled in, so they arrive after the
 * screen does. Held together in one flow so the state has a single "sources are ready" signal.
 */
private data class LoadedSources(
    val itemList: ChecklistCategory?,
    val monsters: Map<String, Monster>
)

/**
 * Collects the starred items from every list that offers stars.
 *
 * The favorites table stores keys and nothing else, so this is where they are turned back into
 * something displayable. That means reaching into all three kinds of source at once - the Kotlin
 * categories, the item list CSV and the fiend CSV - which is exactly why the repository doesn't try
 * to: no other screen needs to know about more than its own.
 *
 * A key that no longer resolves is dropped rather than shown as a blank row. That is the normal way
 * an item leaves a list between releases, and a favorite pointing at something the app no longer has
 * is not an error worth reporting to the player.
 */
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    checklistRepository: ChecklistRepository,
    private val itemListRepository: ItemListRepository,
    private val monsterArenaRepository: MonsterArenaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val sources = MutableStateFlow<LoadedSources?>(null)

    init {
        viewModelScope.launch {
            // Neither failure is worth an error screen: a missing CSV means those favorites can't
            // resolve, and the rest of the lists still have something to show.
            val itemList = runCatching { itemListRepository.load() }.getOrNull()
            val monsters = runCatching { monsterArenaRepository.monsters() }.getOrDefault(emptyList())
            sources.value = LoadedSources(itemList, monsters.associateBy { it.id })
        }
    }

    /**
     * Remembered the same way a checklist's order is, and through the same store: this is one more
     * list with more than one sensible order, so it would be strange for it to be the one that
     * forgets. See [FAVORITES_SORT_KEY].
     */
    private val sort: Flow<FavoritesSort> = settingsRepository.checklistSort(FAVORITES_SORT_KEY)
        .map { FavoritesSort.fromStored(it) }
        .distinctUntilChanged()

    /** Folded together so the main combine stays at the five flows it has overloads for. */
    private val view = combine(settingsRepository.gameVersion, sort) { version, sort ->
        version to sort
    }

    val uiState = combine(
        favoritesRepository.observeAll(),
        checklistRepository.observeCheckedByCategory(),
        monsterArenaRepository.observeCaptures(),
        sources,
        view
    ) { favorites, checked, counts, loaded, (version, sort) ->
        if (loaded == null) return@combine FavoritesUiState(isLoading = true, sort = sort)

        val categories = buildMap {
            favoriteSources.categories.forEach { put(it.id, it) }
            loaded.itemList?.let { put(it.id, it) }
        }
        val byCategory = favorites.groupBy { it.categoryId }

        // Built in drawer order rather than in the favorites' own order, so the groups always read
        // down the screen in the same order the lists do in the menu.
        val entries = favoriteSources.ordered.flatMap { (categoryId, label) ->
            val starred = byCategory[categoryId] ?: return@flatMap emptyList()
            starred.mapNotNull { favorite ->
                val item = when (categoryId) {
                    MONSTER_ARENA_ID -> loaded.monsters[favorite.itemId]?.asItem(counts)
                    else -> categories[categoryId]
                        ?.asItem(favorite, checked[categoryId].orEmpty())
                        ?.forVersion(version)
                }
                item?.let { FavoriteEntry(categoryId, label, it, favorite.createdAt) }
            }
        }

        FavoritesUiState(
            entries = entries.inOrder(sort),
            sort = sort,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState()
    )

    /**
     * Only ever un-stars, since every row here is starred by definition. Removing the last favorite
     * in a group takes the group's header with it, because the group is derived, not stored.
     */
    fun setFavorite(categoryId: String, itemId: String, favorite: Boolean) {
        viewModelScope.launch {
            favoritesRepository.setFavorite(categoryId, itemId, favorite)
        }
    }

    fun setSort(sort: FavoritesSort) {
        viewModelScope.launch {
            settingsRepository.setChecklistSort(FAVORITES_SORT_KEY, sort.name)
        }
    }

    /** The section header is the list's name here, so the item's own section is left off. */
    private fun ChecklistCategory.asItem(
        favorite: Favorite,
        checked: Set<String>
    ): ChecklistItem? {
        val reference = items.firstOrNull { it.id == favorite.itemId } ?: return null
        return ChecklistItem(
            id = reference.id,
            title = reference.title,
            location = reference.location,
            detail = reference.detail,
            caution = reference.caution,
            isChecked = reference.id in checked,
            isFavorite = true,
            tag = reference.tag,
            imageRes = reference.imageRes
        )
    }

    /**
     * A fiend as a checklist row. The capture count rides in the badge the other lists use for their
     * group names, which keeps the whole screen to one kind of row: this is a shortlist to jump from,
     * and the stepper stays where the counting is actually done.
     */
    private fun Monster.asItem(counts: Map<String, Int>): ChecklistItem {
        val count = counts[id] ?: 0
        return ChecklistItem(
            id = id,
            title = name,
            location = area,
            detail = monsterType.orEmpty(),
            caution = null,
            // Creations are unlocked rather than captured, so they have no count to complete.
            isChecked = isCapturable && count >= MAX_CAPTURES,
            isFavorite = true,
            tag = if (isCapturable) "$count / $MAX_CAPTURES" else null
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                FavoritesViewModel(
                    app.container.favoritesRepository,
                    app.container.checklistRepository,
                    app.container.itemListRepository,
                    app.container.monsterArenaRepository,
                    app.container.settingsRepository
                )
            }
        }
    }
}
