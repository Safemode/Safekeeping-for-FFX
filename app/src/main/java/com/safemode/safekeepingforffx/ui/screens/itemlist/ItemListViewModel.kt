package com.safemode.safekeepingforffx.ui.screens.itemlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.repository.ItemListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemListUiState(
    val isLoading: Boolean = true,
    val category: ChecklistCategory? = null
)

/** Loads the CSV-backed category; the list itself is then rendered by the shared checklist screen. */
class ItemListViewModel(repository: ItemListRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val category = runCatching { repository.load() }.getOrNull()
            _uiState.value = ItemListUiState(isLoading = false, category = category)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                ItemListViewModel(app.container.itemListRepository)
            }
        }
    }
}
