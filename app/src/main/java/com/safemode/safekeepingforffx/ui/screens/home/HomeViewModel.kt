package com.safemode.safekeepingforffx.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_LABEL
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryProgress(
    val route: String,
    val label: String,
    val foundCount: Int,
    val totalCount: Int
) {
    val fraction: Float get() = if (totalCount == 0) 0f else foundCount.toFloat() / totalCount
    val isComplete: Boolean get() = totalCount > 0 && foundCount == totalCount
}

/**
 * [categories] is the full set of cards in the player's chosen order, hidden ones included, so the
 * edit screen can show and reorder everything at once. Normal display and the summary counts use
 * [visibleCategories]: a hidden list contributes nothing to "X of Y across N lists".
 *
 * [loaded] is false only for the seed value shown before the saved order and hidden set have been
 * read from disk. The list waits for it rather than drawing the default order first and then
 * animating into the saved one, which read as a flash of reordering on every cold start.
 */
data class HomeUiState(
    val categories: List<CategoryProgress> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val loaded: Boolean = false
) {
    val visibleCategories: List<CategoryProgress> get() = categories.filterNot { it.route in hidden }
    val totalFound: Int get() = visibleCategories.sumOf { it.foundCount }
    val totalItems: Int get() = visibleCategories.sumOf { it.totalCount }
}

/** One hit from the global search: the item, plus which list it lives in so we can navigate there. */
data class SearchResult(
    val categoryId: String,
    val categoryLabel: String,
    val item: ReferenceItem
)

class HomeViewModel(
    repository: ChecklistRepository,
    categories: List<ChecklistCategory>,
    private val searchCategories: List<ChecklistCategory>,
    itemListRepository: ItemListRepository,
    private val monsterArenaRepository: MonsterArenaRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    /**
     * The CSV-backed categories, which arrive after a load rather than being available at
     * construction. Empty until then, so search simply covers less for a moment rather than
     * blocking.
     */
    private val loadedCategories = MutableStateFlow<List<ChecklistCategory>>(emptyList())

    init {
        viewModelScope.launch {
            runCatching { itemListRepository.load() }
                .getOrNull()
                ?.let { loadedCategories.value = listOf(it) }
        }
    }

    /**
     * Searches the static reference data across every category. No database involved: this answers
     * "where is this thing" rather than "have I got it", so it doesn't need progress to be useful.
     */
    val results = combine(_query, loadedCategories) { raw, loaded ->
        val needle = raw.trim()
        if (needle.isEmpty()) {
            emptyList()
        } else {
            (searchCategories + loaded).flatMap { category ->
                category.items
                    .filter { it.matches(needle) }
                    .map { SearchResult(category.id, category.label, it) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private fun ReferenceItem.matches(needle: String): Boolean =
        sequenceOf(title, location, detail, section, tag)
            .any { it?.contains(needle, ignoreCase = true) == true }

    fun setQuery(value: String) {
        _query.update { value }
    }

    /**
     * Monster Arena counts a fiend only once it is fully captured, so a fiend at 9 of 10 adds
     * nothing to the bar. Emits only after the fiend list has parsed, which is why the card appears
     * a moment after the others rather than flashing "0 / 0".
     */
    private val monsterArenaProgress: Flow<CategoryProgress> =
        monsterArenaRepository.observeCaptureProgress().map { progress ->
            CategoryProgress(MONSTER_ARENA_ID, MONSTER_ARENA_LABEL, progress.found, progress.total)
        }

    private val progress: Flow<List<CategoryProgress>> = combine(
        categories.map { category ->
            repository.observeProgress(category).map { progress ->
                CategoryProgress(category.id, category.label, progress.found, progress.total)
            }
        } + monsterArenaProgress
    ) { it.toList() }

    val uiState = combine(
        progress,
        settingsRepository.gameVersion,
        settingsRepository.homeOrder,
        settingsRepository.homeHidden
    ) { progress, version, order, hidden ->
        // A list the chosen release doesn't have drops off Home entirely - card, found count and
        // total - rather than sitting at zero forever. Its screen stays in the drawer regardless.
        val unavailable = categories
            .filterNot { it.isAvailableOn(version) }
            .mapTo(HashSet()) { it.id }
        val available = progress.filterNot { it.route in unavailable }
        HomeUiState(categories = orderCards(available, order), hidden = hidden, loaded = true)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                // Seed with the real totals so the counts don't flash "0 / 0" on first frame.
                categories.map {
                    CategoryProgress(it.id, it.label, 0, it.items.size * it.progressKeys.size)
                }
            )
        )

    /** Persists a new card order. Called once, on leaving edit mode, not on every drag. */
    fun setHomeOrder(order: List<String>) {
        viewModelScope.launch { settingsRepository.setHomeOrder(order) }
    }

    /** Persists which cards are hidden. Called once, on leaving edit mode. */
    fun setHomeHidden(hidden: Set<String>) {
        viewModelScope.launch { settingsRepository.setHomeHidden(hidden) }
    }

    companion object {

        /**
         * Puts [cards] into the player's saved [order]. A card whose route the order doesn't mention -
         * a list added in an update, or one never seen when the order was saved - keeps its natural
         * position at the end, in the order [cards] already had. A route in [order] that no card
         * matches is simply skipped. The sort is stable, so those trailing cards don't shuffle among
         * themselves. An empty [order] means the player never reordered, so the natural order stands.
         */
        internal fun orderCards(
            cards: List<CategoryProgress>,
            order: List<String>
        ): List<CategoryProgress> {
            if (order.isEmpty()) return cards
            val position = order.withIndex().associate { (index, route) -> route to index }
            return cards.sortedBy { position[it.route] ?: Int.MAX_VALUE }
        }


        fun factory(
            categories: List<ChecklistCategory>,
            searchCategories: List<ChecklistCategory>
        ) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                HomeViewModel(
                    app.container.checklistRepository,
                    categories,
                    searchCategories,
                    app.container.itemListRepository,
                    app.container.monsterArenaRepository,
                    app.container.settingsRepository
                )
            }
        }
    }
}
