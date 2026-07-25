package com.safemode.safekeepingforffx.ui.screens.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.domain.ChecklistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChecklistUiState(
    /** Every item in the category, regardless of the search. Progress is counted from this. */
    val items: List<ChecklistItem> = emptyList(),
    /** What the list actually renders: [items] narrowed by [query]. */
    val visibleItems: List<ChecklistItem> = emptyList(),
    val query: String = "",
    val foundCount: Int = 0,
    val totalCount: Int = 0,
    val note: String? = null,
    /** False hides the advice banners at the top of the list. */
    val showHelp: Boolean = true,
    val sort: ChecklistSort = ChecklistSort.DEFAULT,
    /** False for the categories that have only one sensible order, which hides the picker. */
    val canSort: Boolean = false,
    val isLoading: Boolean = true
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val hasNoMatches: Boolean get() = isSearching && visibleItems.isEmpty()
}

/**
 * Drives any [ChecklistCategory]. Every category is structurally the same - a fixed list of
 * reference items plus stored progress - so they share one ViewModel rather than one each.
 */
class ChecklistViewModel(
    private val repository: ChecklistRepository,
    private val settingsRepository: SettingsRepository,
    private val category: ChecklistCategory
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /**
     * Remembered per category, so coming back to a list finds it the way you left it. Read straight
     * from storage rather than mirrored into a local flow, which keeps one source of truth and means
     * a restored backup shows up without the screen being reopened.
     *
     * Categories without a story order are pinned to the default instead of reading anything: they
     * have nothing to offer a stored value, and one left behind by an earlier build shouldn't be
     * able to reorder a list the picker can no longer reach.
     */
    private val sort: Flow<ChecklistSort> =
        if (category.hasStoryOrder) {
            settingsRepository.checklistSort(category.id)
                .map { ChecklistSort.fromStored(it) }
                // DataStore republishes on every write, so without this an unrelated setting
                // changing would rebuild the whole list.
                .distinctUntilChanged()
        } else {
            flowOf(ChecklistSort.DEFAULT)
        }

    val uiState = combine(
        repository.observeCategory(category.id, category.items),
        settingsRepository.gameVersion,
        _query,
        settingsRepository.showHelp,
        sort
    ) { items, version, query, showHelp, sort ->
        val adjusted = items.map { it.forVersion(version) }.inOrder(sort)
        ChecklistUiState(
            items = adjusted,
            visibleItems = adjusted.filter { it.matches(query) },
            query = query,
            // Counted from the whole list, not the filtered one: searching narrows what you see,
            // it doesn't change how much of the category you've collected.
            // Derived, never stored - a persisted counter is a desync bug waiting to happen.
            foundCount = adjusted.count { it.isChecked },
            totalCount = adjusted.size,
            note = noteFor(version),
            showHelp = showHelp,
            sort = sort,
            canSort = category.hasStoryOrder,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState(
            totalCount = category.items.size,
            // Settled up front so the picker doesn't pop in a frame after the list.
            canSort = category.hasStoryOrder
        )
    )

    /** No-op for categories without story stages, so the screen can call it unconditionally. */
    fun setSort(sort: ChecklistSort) {
        if (!category.hasStoryOrder) return
        viewModelScope.launch { settingsRepository.setChecklistSort(category.id, sort.name) }
    }

    /**
     * Every field the row can show is searchable, so "Besaid", "sigil" and a player's name all
     * find something. Blank query matches everything.
     */
    private fun ChecklistItem.matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return sequenceOf(title, location, detail, section, tag, stageNote)
            .any { it?.contains(needle, ignoreCase = true) == true }
    }

    /** Scoped to this ViewModel, which is keyed by category, so a search never leaks across lists. */
    fun setQuery(query: String) {
        _query.update { query }
    }

    /**
     * Dark Aeons don't exist on the original PS2 release, so a "guarded" warning there would be
     * wrong. Missable stays - Home is destroyed in every version.
     */
    private fun ChecklistItem.forVersion(version: GameVersion): ChecklistItem =
        if (!version.hasDarkAeons && caution is Caution.Guarded) copy(caution = null) else this

    private fun noteFor(version: GameVersion): String? =
        listOfNotNull(
            category.note,
            category.darkAeonNote.takeIf { version.hasDarkAeons }
        ).joinToString(" ").ifBlank { null }

    /**
     * Writes straight to the database and lets Room's Flow push the change back. One source of
     * truth, so the checkbox can never disagree with what was actually saved.
     */
    fun setChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            repository.setChecked(category.id, itemId, checked)
        }
    }

    /**
     * Unchecks this list only. Caller must confirm with the user first - this cannot be undone.
     */
    fun resetCategory() {
        viewModelScope.launch { repository.clearCategory(category.id) }
    }

    companion object {
        fun factory(category: ChecklistCategory) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                ChecklistViewModel(
                    app.container.checklistRepository,
                    app.container.settingsRepository,
                    category
                )
            }
        }
    }
}
