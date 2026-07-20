package com.safemode.safekeepingforffx.ui.screens.mix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.MixCombination
import com.safemode.safekeepingforffx.data.reference.MixIngredient
import com.safemode.safekeepingforffx.data.reference.MixResult
import com.safemode.safekeepingforffx.data.reference.MixTable
import com.safemode.safekeepingforffx.data.repository.MixRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which of the two slots a pick is for. */
enum class MixSlot { FIRST, SECOND }

data class MixUiState(
    val isLoading: Boolean = true,
    val ingredients: List<MixIngredient> = emptyList(),
    val results: List<MixResult> = emptyList(),
    val first: MixIngredient? = null,
    val second: MixIngredient? = null,
    val result: MixResult? = null,
    /** The result being looked up in reverse, independent of the two slots above. */
    val browsedResult: MixResult? = null,
    val combinations: List<MixCombination> = emptyList()
) {
    val bothChosen: Boolean get() = first != null && second != null

    /** Both picked but the table has no row - say so rather than showing nothing. */
    val isUnknownCombination: Boolean get() = bothChosen && result == null

    /** The import produced nothing, so the screen has nothing to offer. */
    val failedToLoad: Boolean get() = !isLoading && ingredients.isEmpty()
}

/**
 * Pure lookup over the imported table: nothing here is stored or tracked, which is the whole point
 * of this category.
 */
class MixViewModel(private val repository: MixRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MixUiState())
    val uiState = _uiState.asStateFlow()

    private var table: MixTable = MixTable.EMPTY

    init {
        viewModelScope.launch {
            table = runCatching { repository.load() }.getOrDefault(MixTable.EMPTY)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    ingredients = table.ingredients,
                    results = table.results
                )
            }
        }
    }

    /**
     * Reverse lookup. Grouping runs off the main thread: the busiest result covers several hundred
     * pairs, which is quick but not free.
     */
    fun browse(result: MixResult) {
        viewModelScope.launch {
            val combinations = withContext(Dispatchers.Default) {
                table.combinationsFor(result.id)
            }
            _uiState.update { it.copy(browsedResult = result, combinations = combinations) }
        }
    }

    fun stopBrowsing() {
        _uiState.update { it.copy(browsedResult = null, combinations = emptyList()) }
    }

    fun choose(slot: MixSlot, ingredient: MixIngredient) {
        _uiState.update { current ->
            val updated = when (slot) {
                MixSlot.FIRST -> current.copy(first = ingredient)
                MixSlot.SECOND -> current.copy(second = ingredient)
            }
            val result = resultFor(updated.first, updated.second)

            // Drop the reverse lookup once the two slots make something else, so the screen never
            // shows how to make one result directly under a card announcing a different one. A
            // pair that matches what is being browsed keeps it, since that confirms rather than
            // contradicts, and an unrecognised pair keeps it too - there is no competing answer.
            val contradicts = result != null && result.id != updated.browsedResult?.id

            updated.copy(
                result = result,
                browsedResult = if (contradicts) null else updated.browsedResult,
                combinations = if (contradicts) emptyList() else updated.combinations
            )
        }
    }

    fun clear() {
        _uiState.update { it.copy(first = null, second = null, result = null) }
    }

    private fun resultFor(first: MixIngredient?, second: MixIngredient?): MixResult? =
        if (first == null || second == null) null else table.resultFor(first.id, second.id)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                MixViewModel(app.container.mixRepository)
            }
        }
    }
}
