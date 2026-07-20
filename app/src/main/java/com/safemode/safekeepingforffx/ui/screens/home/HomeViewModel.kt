package com.safemode.safekeepingforffx.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_ID
import com.safemode.safekeepingforffx.data.reference.MONSTER_ARENA_LABEL
import com.safemode.safekeepingforffx.data.reference.ReferenceItem
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

data class HomeUiState(
    val categories: List<CategoryProgress> = emptyList()
) {
    val totalFound: Int get() = categories.sumOf { it.foundCount }
    val totalItems: Int get() = categories.sumOf { it.totalCount }
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
    private val monsterArenaRepository: MonsterArenaRepository
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
    private val monsterArenaProgress: Flow<CategoryProgress> = flow {
        val monsters = runCatching { monsterArenaRepository.monsters() }
            .getOrDefault(emptyList())
            .filter { it.isCapturable }
        emitAll(
            monsterArenaRepository.observeCaptures().map { counts ->
                CategoryProgress(
                    route = MONSTER_ARENA_ID,
                    label = MONSTER_ARENA_LABEL,
                    foundCount = monsters.count { (counts[it.id] ?: 0) >= MAX_CAPTURES },
                    totalCount = monsters.size
                )
            }
        )
    }

    val uiState = combine(
        categories.map { category ->
            repository.observeCategory(category.id, category.items).map { items ->
                CategoryProgress(
                    route = category.id,
                    label = category.label,
                    foundCount = items.count { it.isChecked },
                    totalCount = items.size
                )
            }
        } + monsterArenaProgress
    ) { progress -> HomeUiState(progress.toList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                // Seed with the real totals so the counts don't flash "0 / 0" on first frame.
                categories.map { CategoryProgress(it.id, it.label, 0, it.items.size) }
            )
        )

    companion object {
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
                    app.container.monsterArenaRepository
                )
            }
        }
    }
}
