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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
    settingsRepository: SettingsRepository,
    private val category: ChecklistCategory
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState = combine(
        repository.observeCategory(category.id, category.items),
        settingsRepository.gameVersion,
        _query,
        settingsRepository.showHelp
    ) { items, version, query, showHelp ->
        val adjusted = items.map { it.forVersion(version) }
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
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState(totalCount = category.items.size)
    )

    /**
     * Every field the row can show is searchable, so "Besaid", "sigil" and a player's name all
     * find something. Blank query matches everything.
     */
    private fun ChecklistItem.matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return sequenceOf(title, location, detail, section, tag)
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
