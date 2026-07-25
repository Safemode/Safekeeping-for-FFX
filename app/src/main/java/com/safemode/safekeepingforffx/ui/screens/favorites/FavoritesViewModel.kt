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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The favorites from one list, under that list's name. */
data class FavoriteGroup(
    val categoryId: String,
    val label: String,
    val items: List<ChecklistItem>
)

data class FavoritesUiState(
    val groups: List<FavoriteGroup> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && totalCount == 0
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
    settingsRepository: SettingsRepository
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

    val uiState = combine(
        favoritesRepository.observeAll(),
        checklistRepository.observeCheckedByCategory(),
        monsterArenaRepository.observeCaptures(),
        sources,
        settingsRepository.gameVersion
    ) { favorites, checked, counts, loaded, version ->
        if (loaded == null) return@combine FavoritesUiState(isLoading = true)

        val categories = buildMap {
            favoriteSources.categories.forEach { put(it.id, it) }
            loaded.itemList?.let { put(it.id, it) }
        }
        val byCategory = favorites.groupBy { it.categoryId }

        // Driven by the drawer's order rather than by the favorites themselves, so the groups always
        // read down the screen in the same order the lists do in the menu.
        val groups = favoriteSources.ordered.mapNotNull { (categoryId, label) ->
            val starred = byCategory[categoryId] ?: return@mapNotNull null
            val items = starred.mapNotNull { favorite ->
                when (categoryId) {
                    MONSTER_ARENA_ID -> loaded.monsters[favorite.itemId]?.asItem(counts)
                    else -> categories[categoryId]
                        ?.asItem(favorite, checked[categoryId].orEmpty())
                        ?.forVersion(version)
                }
            }
            if (items.isEmpty()) null else FavoriteGroup(categoryId, label, items)
        }

        FavoritesUiState(
            groups = groups,
            totalCount = groups.sumOf { it.items.size },
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
