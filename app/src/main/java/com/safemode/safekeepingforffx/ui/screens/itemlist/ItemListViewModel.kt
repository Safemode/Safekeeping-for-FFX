package com.safemode.safekeepingforffx.ui.screens.itemlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.itemNamesFoundInArena
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemListUiState(
    val isLoading: Boolean = true,
    val category: ChecklistCategory? = null,
    /**
     * Titles of the items at least one fiend drops, steals, is bribed for or is beaten for. Only
     * these rows offer the jump to the Monster Arena, so the search waiting there always has
     * something to show.
     */
    val arenaItemTitles: Set<String> = emptySet()
)

/** Loads the CSV-backed category; the list itself is then rendered by the shared checklist screen. */
class ItemListViewModel(
    repository: ItemListRepository,
    arenaRepository: MonsterArenaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val category = runCatching { repository.load() }.getOrNull()
            // Both assets are parsed once and cached, so cross-referencing them costs one pass over
            // the fiend list. Failing to read it only costs the jump, never the item list itself.
            val monsters = runCatching { arenaRepository.monsters() }.getOrDefault(emptyList())
            _uiState.value = ItemListUiState(
                isLoading = false,
                category = category,
                arenaItemTitles = itemNamesFoundInArena(
                    category?.items.orEmpty().map { it.title },
                    monsters
                )
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                ItemListViewModel(
                    app.container.itemListRepository,
                    app.container.monsterArenaRepository
                )
            }
        }
    }
}
