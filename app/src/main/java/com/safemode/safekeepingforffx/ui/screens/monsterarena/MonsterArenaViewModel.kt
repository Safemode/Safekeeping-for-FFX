package com.safemode.safekeepingforffx.ui.screens.monsterarena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safemode.safekeepingforffx.FfxApplication
import com.safemode.safekeepingforffx.data.reference.CreationProgress
import com.safemode.safekeepingforffx.data.reference.MAX_CAPTURES
import com.safemode.safekeepingforffx.data.reference.Monster
import com.safemode.safekeepingforffx.data.reference.computeCreationProgress
import com.safemode.safekeepingforffx.data.reference.creationCaptureTargets
import com.safemode.safekeepingforffx.data.reference.creationKind
import com.safemode.safekeepingforffx.data.repository.MonsterArenaRepository
import com.safemode.safekeepingforffx.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One fiend plus how many of it the player has caught. */
data class MonsterCapture(val monster: Monster, val count: Int) {
    val isComplete: Boolean get() = count >= MAX_CAPTURES
}

/** One fiend a creation's auto-capture would raise, and the count it would raise it to. */
data class AutoCaptureFiend(val id: String, val name: String, val amount: Int)

/**
 * Everything the long-press auto-capture on a creation needs: which fiends it would set and to
 * what. Only ever built for creations won by capturing, so a non-null entry is also the signal that
 * the long-press should be offered at all.
 */
data class CreationAutoCapture(
    val creationId: String,
    val creationName: String,
    val fiends: List<AutoCaptureFiend>
) {
    /** The single amount every fiend is raised to, or null on the rare mix (none in the data). */
    val uniformAmount: Int? get() = fiends.map { it.amount }.distinct().singleOrNull()

    val targets: Map<String, Int> get() = fiends.associate { it.id to it.amount }
}

/**
 * True if [needle] appears anywhere the fiend carries text - its name, its area, or any detail
 * column, which is what lets a search reach a type, a stolen or bribed item, or a creation's
 * unlock and reward. Blank detail cells were dropped at parse time, so this only ever matches real
 * data.
 */
private fun Monster.matches(needle: String): Boolean =
    name.contains(needle, ignoreCase = true) ||
        area.contains(needle, ignoreCase = true) ||
        details.values.any { it.contains(needle, ignoreCase = true) }

data class MonsterArenaUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    /** Ordered by area, in the order the areas appear in the source file. */
    val byArea: Map<String, List<MonsterCapture>> = emptyMap(),
    /**
     * Live unlock state for every creation, keyed by its monster id. Computed over the full fiend
     * list, not the filtered view, so a creation still knows it is unlocked while a search hides
     * the fiends that unlocked it.
     */
    val creationProgress: Map<String, CreationProgress> = emptyMap(),
    /**
     * Long-press auto-capture data for each capture-based creation, keyed by its id. A creation
     * missing from this map (the conquest-gated originals) simply offers no long-press.
     */
    val autoCaptures: Map<String, CreationAutoCapture> = emptyMap(),
    val capturedCount: Int = 0,
    val totalCount: Int = 0,
    /** Any count above zero, which is what makes a reset worth offering. */
    val hasProgress: Boolean = false,
    /** False hides the advice banner at the top of the list. */
    val showHelp: Boolean = true
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val hasNoMatches: Boolean get() = isSearching && byArea.isEmpty()
    val isEmpty: Boolean get() = !isLoading && totalCount == 0
}

class MonsterArenaViewModel(
    private val repository: MonsterArenaRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val monsters = MutableStateFlow<List<Monster>>(emptyList())
    private val loaded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            monsters.value = runCatching { repository.monsters() }.getOrDefault(emptyList())
            loaded.value = true
        }
    }

    val uiState = combine(
        monsters,
        repository.observeCaptures(),
        _query,
        loaded,
        settingsRepository.showHelp
    ) { all, counts, query, isLoaded, showHelp ->
        val captures = all.map { MonsterCapture(it, counts[it.id] ?: 0) }
        val needle = query.trim()
        val visible = if (needle.isEmpty()) {
            captures
        } else {
            captures.filter { it.monster.matches(needle) }
        }

        val byId = all.associateBy { it.id }
        val autoCaptures = all
            .filter { it.creationKind() != null }
            .mapNotNull { creation ->
                val targets = creationCaptureTargets(creation, all)
                if (targets.isEmpty()) return@mapNotNull null
                // targets keeps file order, so the dialog lists fiends area by area.
                val fiends = targets.mapNotNull { (id, amount) ->
                    byId[id]?.let { AutoCaptureFiend(id, it.name, amount) }
                }
                creation.id to CreationAutoCapture(creation.id, creation.name, fiends)
            }
            .toMap()

        MonsterArenaUiState(
            isLoading = !isLoaded,
            query = query,
            // groupBy keeps insertion order, so areas stay in file order.
            byArea = visible.groupBy { it.monster.area },
            creationProgress = computeCreationProgress(all, counts),
            autoCaptures = autoCaptures,
            // Counted over everything, not the filtered view: searching narrows what you see, it
            // doesn't change how much you have caught. Arena creations are excluded because they
            // cannot be captured, so counting them would make the total unreachable.
            capturedCount = captures.count { it.monster.isCapturable && it.isComplete },
            totalCount = captures.count { it.monster.isCapturable },
            // Any partial count counts: resetting should be offered at 3 of 10, not only at 10.
            hasProgress = captures.any { it.count > 0 },
            showHelp = showHelp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MonsterArenaUiState()
    )

    val query = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.update { value }
    }

    /** [delta] is +1 or -1; the repository clamps to 0..[MAX_CAPTURES]. */
    fun adjust(capture: MonsterCapture, delta: Int) {
        viewModelScope.launch {
            repository.setCount(capture.monster.id, capture.count + delta)
        }
    }

    /**
     * Fills in exactly the fiends a creation needs, raising each to its required count without
     * lowering any that are already higher. The lock then opens on the next state emission.
     */
    fun autoCapture(auto: CreationAutoCapture) {
        viewModelScope.launch { repository.captureAtLeast(auto.targets) }
    }

    /**
     * Clears every capture count in this category only. Caller must confirm with the user first -
     * this cannot be undone.
     */
    fun resetCaptures() {
        viewModelScope.launch { repository.clearAll() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FfxApplication
                MonsterArenaViewModel(
                    app.container.monsterArenaRepository,
                    app.container.settingsRepository
                )
            }
        }
    }
}
