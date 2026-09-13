package com.safemode.safekeepingforffx.ui.screens.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.ChecklistCategory
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.repository.ChecklistRepository
import com.safemode.safekeepingforffx.data.repository.FavoritesRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import com.safemode.safekeepingforffx.domain.ChecklistItem
import com.safemode.safekeepingforffx.domain.forVersion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The line a per-character row shows for [character], e.g. "Tidus: 150 to learn". Grouped with a
 * fixed locale so the thousands separator matches the rest of the app's English text.
 */
internal fun characterNote(character: GridCharacter, count: Int): String =
    "${character.displayName}: ${"%,d".format(Locale.US, count)} to learn"

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
    /** Whose ticks are showing, on a per-character list. Null for every other list. */
    val character: GridCharacter? = null,
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
    private val favoritesRepository: FavoritesRepository,
    private val category: ChecklistCategory
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Whose progress is on screen. Only ever read for a [ChecklistCategory.perCharacter] list. */
    private val character = MutableStateFlow(GridCharacter.DEFAULT)

    /** Looked up once: the per-character figures never change, only which one is showing. */
    private val countsById = category.items.associate { it.id to it.characterCounts }

    /** Stamps each row with [selected]'s figure. Rows with no figure for them are left as they are. */
    private fun List<ChecklistItem>.withCharacterNotes(selected: GridCharacter) = map { item ->
        val count = countsById[item.id]?.get(selected) ?: return@map item
        item.copy(characterNote = characterNote(selected, count))
    }

    /**
     * The stored ticks for this list, paired with the character they belong to. Switching character
     * swaps the query underneath rather than filtering one combined list, and pairing the two means
     * the picker and the rows can never show different characters, even for a frame mid-switch.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val progress: Flow<Pair<List<ChecklistItem>, GridCharacter?>> =
        if (category.perCharacter) {
            character.flatMapLatest { selected ->
                repository.observeCategory(category.progressKey(selected), category.items)
                    .map { items -> items.withCharacterNotes(selected) to selected }
            }
        } else {
            repository.observeCategory(category.id, category.items).map { it to null }
        }

    /**
     * Progress and stars are stored apart - one is "have I got this", the other is "keep this where
     * I can find it" - and are merged here rather than in [ChecklistRepository], which would other-
     * wise have to know about a feature that has nothing to do with reference data. Folded together
     * before the main combine because that one is already at the five flows it has overloads for.
     */
    private val listing = combine(
        progress,
        // Keyed by the list, not the character: a star is shared by every character's copy.
        favoritesRepository.observeCategory(category.id)
    ) { (items, selected), favorites ->
        items.map { it.copy(isFavorite = it.id in favorites) } to selected
    }

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
        listing,
        settingsRepository.gameVersion,
        _query,
        settingsRepository.showHelp,
        sort
    ) { (items, selected), version, query, showHelp, sort ->
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
            character = selected,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState(
            totalCount = category.items.size,
            // Settled up front so the picker doesn't pop in a frame after the list.
            canSort = category.hasStoryOrder,
            // Also settled up front, so the character picker is there from the first frame.
            character = if (category.perCharacter) GridCharacter.DEFAULT else null
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
        val key = currentProgressKey()
        viewModelScope.launch {
            repository.setChecked(key, itemId, checked)

            // Only lists with a completion item pay for the extra read. Read back from storage
            // after the write rather than from uiState, so two quick taps can't each see a list
            // missing the other's tick and leave the completion item out of step.
            val completionId = category.completionItemId ?: return@launch
            val checkedIds = repository.observeCategory(key, category.items).first()
                .filter { it.isChecked }
                .mapTo(HashSet()) { it.id }
            category.completionUpdate(checkedIds, itemId)?.let { complete ->
                repository.setChecked(key, completionId, complete)
            }
        }
    }

    /** Same one-source-of-truth path as [setChecked]: write it, let the Flow bring it back. */
    fun setFavorite(itemId: String, favorite: Boolean) {
        viewModelScope.launch {
            favoritesRepository.setFavorite(category.id, itemId, favorite)
        }
    }

    /**
     * Unchecks this list only - and on a per-character list, only the selected character's copy.
     * Caller must confirm with the user first - this cannot be undone.
     */
    fun resetCategory() {
        val key = currentProgressKey()
        viewModelScope.launch { repository.clearCategory(key) }
    }

    /** No-op for a list that isn't per character, so the screen can call it unconditionally. */
    fun setCharacter(selected: GridCharacter) {
        if (category.perCharacter) character.value = selected
    }

    /**
     * Read when the box is tapped rather than when the write lands, so a tick made just before
     * switching character still goes to the character it was made for.
     */
    private fun currentProgressKey(): String = category.progressKey(character.value)

    companion object {
        fun factory(category: ChecklistCategory) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                ChecklistViewModel(
                    app.container.checklistRepository,
                    app.container.settingsRepository,
                    app.container.favoritesRepository,
                    category
                )
            }
        }
    }
}
